package org.nd4j.autodiff.execution;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.autodiff.execution.conf.ExecutionMode;
import org.nd4j.autodiff.execution.conf.ExecutorConfiguration;
import org.nd4j.autodiff.execution.conf.OutputMode;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.graph.FlatArray;
import org.nd4j.graph.FlatResult;
import org.nd4j.graph.FlatVariable;
import org.nd4j.graph.OpType;
import org.nd4j.linalg.api.memory.pointers.PagedPointer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.nativeblas.NativeOpsHolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author raver119@gmail.com
 */
@Slf4j
public class NativeGraphExecutioner implements GraphExecutioner {
    /**
     * This method returns Type of this executioner
     *
     * @return
     */
    @Override
    public Type getExecutionerType() {
        return Type.LOCAL;
    }


    /**
     * This method executes given graph and returns results
     *
     * PLEASE NOTE: Default configuration is used
     *
     * @param sd
     * @return
     */
    @Override
    public INDArray[] executeGraph(SameDiff sd) {
        return executeGraph(sd, ExecutorConfiguration.builder().outputMode(OutputMode.IMPLICIT).executionMode(ExecutionMode.SEQUENTIAL).profilingMode(OpExecutioner.ProfilingMode.DISABLED).build());
    }

    @Override
    public INDArray[] reuseGraph(SameDiff graph, Map<Integer, INDArray> inputs) {
        throw new UnsupportedOperationException();
    }

    public ByteBuffer convertToFlatBuffers(SameDiff sd, ExecutorConfiguration configuration, Map<Integer, Node> intermediate) {
        log.info("Configuration: {}", configuration);

        return sd.asFlatBuffers();
    }

    @Override
    public ByteBuffer convertToFlatBuffers(SameDiff sd, ExecutorConfiguration configuration) {
        return convertToFlatBuffers(sd, configuration, new HashMap<Integer, Node>());
    }

    /**
     * This method executes given graph and returns results
     *
     * @param sd
     * @return
     */
    @Override
    public INDArray[] executeGraph(SameDiff sd, ExecutorConfiguration configuration) {

        Map<Integer, Node> intermediate = new HashMap<>();

        ByteBuffer buffer = convertToFlatBuffers(sd, configuration, intermediate);

        BytePointer bPtr = new BytePointer(buffer);

        log.info("Buffer length: {}", buffer.limit());

        Pointer res  = NativeOpsHolder.getInstance().getDeviceNativeOps().executeFlatGraphFloat(null, bPtr);

        // FIXME: this is BAD
        PagedPointer pagedPointer = new PagedPointer(res,1024 * 1024L);
        FlatResult fr = FlatResult.getRootAsFlatResult(pagedPointer.asBytePointer().asByteBuffer());


        log.info("VarMap: {}", sd.variableMap());

        INDArray[] results = new INDArray[fr.variablesLength()];

        for (int e = 0; e < fr.variablesLength(); e++) {
            FlatVariable var = fr.variables(e);
            log.info("Var received: id: [{}:{}/<{}>];", var.id().first(), var.id().second(), var.name());
            FlatArray ndarray = var.ndarray();

            INDArray val = Nd4j.createFromFlatArray(ndarray);
            results[e] = val;

            if (var.name() != null && sd.variableMap().containsKey(var.name())) {
                //log.info("VarName: {}; Exists: {}; NDArrayInfo: {};", var.opName(), sd.variableMap().containsKey(var.opName()), sd.getVertexToArray().containsKey(var.opName()));
                sd.associateArrayWithVariable(val, sd.variableMap().get(var.name()));

            } else {
                int[] original = intermediate.get(var.id()).getOriginalOutput();
                //log.info("Original id: {}; out: {}; out2: {}", original, sd.getVertexIdxToInfo().get(original), graph.getVariableForVertex(original));
                if (sd.variableMap().get(sd.graph().getVariableForVertex(original[0]).getVarName()) != null) {
                    sd.associateArrayWithVariable(val,sd.variableMap().get(sd.graph().getVariableForVertex(original[0]).getVarName()));
                } else {
                    SDVariable variable = SDVariable.builder()
                            .varName(sd.graph().getVariableForVertex(original[0]).getVarName())
                            .shape(val.shape())
                            .sameDiff(sd)
                            .build();

                    sd.associateArrayWithVariable(val,variable);
                    sd.addVariable(variable);
                }
            }
        }


        return results;
    }


    /*
    // first version

    @Override
    public INDArray[] executeGraph(SameDiff sd, ExecutorConfiguration configuration) {
        FlatBufferBuilder bufferBuilder = new FlatBufferBuilder(2048);

        SDGraph graph =  sd.getGraph();

        log.info("SameDiff variables: {}", sd.getSameDiffVariables());
        log.info("Graph outputs: {}", graph.getOutputs());
        log.info("--------------");

        // we use this map to convert SDVariables to op nodes for native backend
        Map<Integer, Integer> vertexMap = new HashMap<>();
        Map<String, Integer> vertexMapS = new HashMap<>();
        Map<Integer, List<Integer>> useMap = new HashMap<>();

        List<OpExecAction> ops = graph.getOpOrder().getActions();
        List<Integer> nodes = new ArrayList<>();
        List<Integer> variables = new ArrayList<>();
        int nodesCount = 1;

        // in first loop we build vertexMap only for output nodes
        for (OpExecAction action: ops) {
            log.info("Action: {}", action);
            NDArrayInformation out = action.getOutput();
            SDVariable sdOut = sd.variableMap().get(out.getId());

            // output of this operation is declared variable
            if (sdOut != null && sdOut.getId() < 0) {
                vertexMapS.put(out.getId(), sdOut.getId());
                log.info("Storing [{}/{}] variable as node_{} output", action.getOutputId(), out.getId(), nodesCount);
            } else {
                // output of this node is internal variable, we'll assume this node everywhere
                vertexMap.put(action.getOutputId(), nodesCount);
                vertexMapS.put(out.getId(), nodesCount);
                log.info("Storing [{}/{}] variable as node_{} output", action.getOutputId(), out.getId(), nodesCount);
            }

            if (useMap.get(nodesCount) == null)
                useMap.put(nodesCount, new ArrayList<>());

            nodesCount++;
        }

        log.info("-------------------");

        // in this loop we build list of input nodes
        nodesCount = 1;
        for (OpExecAction action: ops) {

            for (NDArrayInformation var: action.getInputs()) {
                SDVariable sdVar = sd.variableMap().get(var.getId());

                log.info("Var: {}; Mapping {} to node: {}", var.getId(), vertexMapS.get(var.getId()), nodesCount);

                if (sdVar != null && sdVar.getId() >= 0)
                    useMap.get(vertexMapS.get(var.getId())).add(nodesCount);
            }

            nodesCount++;
        }

        log.info("-------------------");

        // in this loop we build nodes
        nodesCount = 1;
        for (OpExecAction action: ops) {
            log.info("Op: {}", action.getOpState());

            int[] mappedIns = new int[action.getInputs().length];

            // meh
            int[] mappedOuts = new int[useMap.get(nodesCount).size()];


            int varsCount = 0;
            // fetching input vars first
            for (NDArrayInformation var: action.getInputs()) {
                SDVariable sdVar = sd.variableMap().get(var.getId());

                // negative ID assumes pre-created array
                if (sdVar !=  null && sdVar.getId() < 0) {
                    log.info("Input varId: {}; varName: {};", sdVar.getId(), var.getId());

                    INDArray arr = sdVar.getArr().isView() ? sdVar.getArr().dup(sdVar.getArr().ordering()) : sdVar.getArr();
                    int opName = bufferBuilder.createString(sdVar.getVarName());
                    int values = FlatVariable.createValuesVector(bufferBuilder, arr.data().asFloat());
                    int shape = FlatVariable.createShapeVector(bufferBuilder, arr.shapeInfoDataBuffer().asInt());

                    int flatVariable = FlatVariable.createFlatVariable(bufferBuilder, sdVar.getId(), opName, shape, values, -1);
                    variables.add(flatVariable);

                    mappedIns[varsCount++] = sdVar.getId();
                } else {
                    log.info("Empty Input varId: {}; varName: {};", vertexMapS.get(var.getId()), var.getId());

                    // in all other cases - it's "virtual" array, will be created as op result instead
                    int opName = bufferBuilder.createString("auto");
                    int values = FlatVariable.createValuesVector(bufferBuilder, new float[]{});
                    int shape = FlatVariable.createShapeVector(bufferBuilder, new int[]{});

                    // FIXME: we need auto ID here instead of 119
                    int flatVariable = FlatVariable.createFlatVariable(bufferBuilder, 119, opName, shape, values, -1);
                    variables.add(flatVariable);

                    mappedIns[varsCount++] = vertexMapS.get(var.getId());
                }
            }

            int outCount = 0;
            for (Integer o : useMap.get(nodesCount)) {
                mappedOuts[outCount++] = o;
            }

            // make this variable
            float[] extras = action.getOpState().getExtraArgs() != null ? new float[action.getOpState().getExtraArgs().length] : new float[0];
            for (int e = 0; e < extras.length; e++) {
                extras[e] = ((Number) action.getOpState().getExtraArgs()[e]).floatValue();
            }

            log.info("Node_{} inputs: {}; outputs: {}", nodesCount, Arrays.toString(mappedIns), Arrays.toString(mappedOuts));
            int nodesIn = FlatNode.createInputVector(bufferBuilder, mappedIns);
            int nodesOut = FlatNode.createOutputVector(bufferBuilder, mappedOuts);
            int extraz = FlatNode.createExtraParamsVector(bufferBuilder, extras);
            int dimensions = FlatNode.createDimensionsVector(bufferBuilder, action.getOpState().getAxes() != null ? action.getOpState().getAxes() : new int[]{});

            int flatNode = FlatNode.createFlatNode(bufferBuilder,
                                                   nodesCount,
                                                   getFlatOpType(action.getOpState().getOpType()),
                                                   getOpNum(action.getOpState().getOpName(), action.getOpState().getOpType()),
                                                   nodesIn,
                                                   (byte) 0,
                                                   nodesOut,
                                                   extraz,
                                                   dimensions,
                                            -1,
                    action.getOpState().getOpType() == OpState.OpType.SCALAR_TRANSFORM ? action.getOpState().getScalarValue().floatValue() : 0.0f);

            nodes.add(flatNode);
            nodesCount++;
        }

        log.info("Variables: {}", variables);
        log.info("Nodes: {}", nodes);

        int outputsOffset = FlatGraph.createVariablesVector(bufferBuilder, new int[]{});
        int variablesOffset = FlatGraph.createVariablesVector(bufferBuilder, Ints.toArray(variables));
        int nodesOffset = FlatGraph.createNodesVector(bufferBuilder, Ints.toArray(nodes));

        int fg = FlatGraph.createFlatGraph(bufferBuilder, 119, variablesOffset, nodesOffset, outputsOffset, configuration.getFlatConfiguration(bufferBuilder));
        bufferBuilder.finish(fg);

        ByteBuffer buffer = bufferBuilder.dataBuffer();
        BytePointer bPtr = new BytePointer(buffer);

        log.info("Buffer length: {}", buffer.limit());

        Pointer res  = NativeOpsHolder.getInstance().getDeviceNativeOps().executeFlatGraphFloat(null, bPtr);

        PagedPointer pagedPointer = new PagedPointer(res,      1024 * 1024L);
        FlatResult fr = FlatResult.getRootAsFlatResult(pagedPointer.asBytePointer().asByteBuffer());

        INDArray[] results = new INDArray[fr.variablesLength()];
        for (int e = 0; e < fr.variablesLength(); e++) {
            FlatVariable var = fr.variables(e);
            float[] values = new float[var.valuesLength()];
            int[] shape = new int[var.shapeLength()];

            for (int i = 0; i < var.valuesLength(); i++) {
                values[i] = var.values(i);
            }

            for (int i = 0; i < var.shapeLength(); i++) {
                shape[i] = var.shape(i);
            }

            int[] _shape = new int[shape[0]];
            for (int i = 0; i < _shape.length; i++) {
                _shape[i] = shape[i+1];
            }

            char _order = shape[shape[0] * 2 + 4 - 1] == 99 ? 'c' : 'f';

            INDArray val = Nd4j.create(values, _shape, _order, 0);
            results[e] = val;
        }

        return results;
    }
    */

    public static long getOpNum(String name, Op.Type type) {
        if (type == Op.Type.CUSTOM)
            return Nd4j.getExecutioner().getCustomOperations().get(name.toLowerCase()).getHash();
        else
            return (long) Nd4j.getOpFactory().getOpNumByName(name);
    }

    public static byte getFlatOpType(Op.Type type) {
        switch (type) {
            case SCALAR:
                return OpType.SCALAR;
            case BROADCAST:
                return OpType.BROADCAST;
            case TRANSFORM:
                return OpType.TRANSFORM;
            case REDUCE:
                return OpType.ACCUMULATION;
            case INDEXREDUCE:
                return OpType.INDEX_ACCUMULATION;
            case CUSTOM:
                return OpType.CUSTOM;
            default:
                throw new UnsupportedOperationException("Unknown op type passed in: " + type);
        }
    }

    /**
     * This method executes
     *
     * @param id
     * @param variables
     * @return
     */
    @Override
    public INDArray[] executeGraph(int id, SDVariable... variables) {
        return new INDArray[0];
    }

    /**
     * This method stores given graph for future execution
     *
     * @param graph
     * @return
     */
    @Override
    public int registerGraph(SameDiff graph) {
        return 0;
    }


    @Override
    public INDArray[] importProto(File file) {
        // TODO: to be implemented
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
