package org.nd4j.linalg.api.ops;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.*;

/**
 * Basic implementation for CustomOp
 *
 * @author raver119@gmail.com
 */
@Slf4j
public class DynamicCustomOp extends DifferentialFunction implements CustomOp {

    private String opName;
    @Builder.Default private List<INDArray> inputArguments = new ArrayList<>();
    @Builder.Default private List<INDArray> outputArguments = new ArrayList<>();


    @Builder.Default private List<Double> tArguments = new ArrayList<>();
    @Builder.Default  private List<Integer> iArguments = new ArrayList<>();
    @Getter private boolean inplaceCall;
    @Getter private long hash;
    protected DifferentialFunction[] outputFunctions;
    private List<int[]> outputShapes;

    public DynamicCustomOp() {
        iArguments = new ArrayList<>();
        tArguments = new ArrayList<>();
    }

    public DynamicCustomOp(String opName, SameDiff sameDiff, DifferentialFunction[] args) {
        super(sameDiff, args);
        this.opName = opName;
        iArguments = new ArrayList<>();
        tArguments = new ArrayList<>();
        addAsNewVertexId();
        f().addFunctionEdges(this);

    }


    /**
     * Initialize this custom op with all of the
     * inputs, outputs, and respective
     * arguments for execution
     * @param opName the opName of the op to execute
     * @param inputs the inputs to the op
     * @param outputs the outputs of the op
     * @param tArguments the input float arguments
     * @param iArguments the input int arguments
     */
    public DynamicCustomOp(String opName, INDArray[] inputs, INDArray[] outputs, List<Double> tArguments, List<Integer> iArguments) {
        inputArguments = new ArrayList<>(Arrays.asList(inputs));
        outputArguments = new ArrayList<>(Arrays.asList(outputs));
        this.opName = opName;
        this.tArguments = tArguments;
        this.iArguments = iArguments;
    }


    /**
     * Initialize this operation for execution (pre created ndarrays)
     * @param opName the operation opName to use
     *               for invocation
     * @param inputs the inputs
     * @param outputs the outputs of the op
     */
    public DynamicCustomOp(String opName,INDArray[] inputs,INDArray[] outputs) {
        this(opName,inputs,outputs, Lists.<Double>newArrayList(), Lists.<Integer>newArrayList());
    }

    /**
     * Initialize this for {@link SameDiff} execution
     * Any extra int or float arguments for operations
     * must be added to the respective TArguments
     *  or IArguments lists upon construction
     * @param opName the operation opName
     * @param sameDiff the samediff instance to use
     * @param args the arguments to use
     * @param inPlace whether the operation is in place or not
     *
     */
    public DynamicCustomOp(String opName,SameDiff sameDiff, DifferentialFunction[] args, boolean inPlace) {
        super(sameDiff, inPlace, args);
        this.opName = opName;
        iArguments = new ArrayList<>();
        tArguments = new ArrayList<>();
    }

    protected DynamicCustomOp(String opName) {
        this.opName = opName;
        iArguments = new ArrayList<>();
        tArguments = new ArrayList<>();
    }



    /**
     * This method returns op opName as string
     *
     * @return
     */
    @Override
    public String opName() {
        return opName;
    }

    @Override
    public int[] getVertexId() {
        return vertexId;
    }



    @Override
    public DifferentialFunction[] outputFunctions() {
        if(this.outputFunctions == null) {
            int[] to = sameDiff.graph().getToFor(vertexId);
            if(to == null) {
                val args = args();
                List<Integer> ids = new ArrayList<>();
                for(int i = 0; i < args.length; i++) {
                    ids.addAll(Ints.asList(args[i].resultVertexId()));
                }

                to = sameDiff.graph().getToFor(Ints.toArray(ids));
                if(to == null) {
                    throw new ND4JIllegalStateException("Unable to find to vertex id output functions for vertex " + Arrays.toString(resultVertexId()));
                }
            }

            List<DifferentialFunction> funcs = new ArrayList<>();
            funcs.add(sameDiff.getFunctionForVertexId(to));
            this.outputFunctions = funcs.toArray(new DifferentialFunction[funcs.size()]);
        }
        return outputFunctions;
    }

    /**
     * This method returns LongHash of the opName()
     *
     * @return
     */
    @Override
    public long opHash() {
        if (hash == 0) {
            val map = Nd4j.getExecutioner().getCustomOperations();
            val lcName = opName().toLowerCase();
            val desc = map.get(lcName);

            hash = desc.getHash();
        }

        return hash;
    }

    @Override
    public INDArray[] outputArguments() {
        if(!outputArguments.isEmpty())
            return outputArguments.toArray(new INDArray[outputArguments.size()]);
        return new INDArray[0];
    }

    @Override
    public INDArray[] inputArguments() {
        if(!inputArguments.isEmpty())
            return inputArguments.toArray(new INDArray[inputArguments.size()]);
        return new INDArray[0];

    }

    @Override
    public int[] iArgs() {
        return Ints.toArray(iArguments);
    }

    @Override
    public double[] tArgs() {
        return Doubles.toArray(tArguments);
    }

    @Override
    public void addIArgument(int... arg) {
        addIArgument(Ints.asList(arg).toArray(new Integer[arg.length]));
    }

    private void addIArgument(Integer... arg) {
        iArguments.addAll(Arrays.asList(arg));
    }

    @Override
    public void removeIArgument(Integer arg) {
        iArguments.remove(arg);
    }

    @Override
    public Integer getIArgument(int index) {
        return iArguments.get(index);
    }

    @Override
    public int numIArguments() {
        return iArguments.size();
    }

    @Override
    public void addTArgument(double... arg) {
        addTArgument(Doubles.asList(arg).toArray(new Double[arg.length]));

    }

    private void addTArgument(Double... arg) {
        tArguments.addAll(Arrays.asList(arg));
    }

    @Override
    public void removeTArgument(Double arg) {
        tArguments.remove(arg);
    }

    @Override
    public Double getTArgument(int index) {
        return tArguments.get(index);
    }

    @Override
    public int numTArguments() {
        return tArguments.size();
    }

    @Override
    public void addInputArgument(INDArray... arg) {
        inputArguments.addAll(Arrays.asList(arg));
    }

    @Override
    public void removeInputArgument(INDArray arg) {
        inputArguments.remove(arg);
    }

    @Override
    public INDArray getInputArgument(int index) {
        return inputArguments.get(index);
    }

    @Override
    public int numInputArguments() {
        return inputArguments.size();
    }

    @Override
    public void addOutputArgument(INDArray... arg) {
        outputArguments.addAll(Arrays.asList(arg));
    }

    @Override
    public void removeOutputArgument(INDArray arg) {
        outputArguments.remove(arg);
    }

    @Override
    public INDArray getOutputArgument(int index) {
        return outputArguments.get(index);
    }

    @Override
    public int numOutputArguments() {
        return outputArguments.size();
    }


    @Override
    public int opNum() {
        return (int) opHash();
    }

    /**
     * This method takes custom opname, and return Op DynamicCustomOpsBuilder instance
     * @param opName
     * @return
     */
    public static DynamicCustomOpsBuilder builder(String opName) {
        val map = Nd4j.getExecutioner().getCustomOperations();
        val lcName = map.containsKey(opName) ? opName : opName.toLowerCase();
        val desc = map.get(lcName);

        if (desc == null)
            throw new ND4JIllegalStateException("Unknown operations requested: [" + opName + "]");

        return new DynamicCustomOpsBuilder(lcName, desc.getHash(), desc.getNumInputs(), desc.getNumOutputs(), desc.isAllowsInplace(), desc.getNumTArgs(), desc.getNumIArgs());
    }

    @Override
    public List<int[]> calculateOutputShape() {
        for(val arg : args()) {
            if(sameDiff.isPlaceHolder(arg.resultVertexId()) && !sameDiff.shapeAlreadyExistsForVertexId(arg.resultVertexId()))
                return Collections.emptyList();
        }

        if(outputShapes != null)
            return outputShapes;
        if(inputArguments.isEmpty()) {
            addArrayInputArguments();
            if(inputArguments.isEmpty())
                throw new ND4JIllegalStateException("Inputs must not be empty when calling this method");
        }


        /**
         * Note that we are currently getting shape errors
         * because the input and output arguments are not specified.
         */
        return Nd4j.getExecutioner().calculateOutputShape(this);
    }


    public void addArrayInputArguments() {
        if(!inputArguments.isEmpty()) {
            return;
        }


        val args = args();
        for(val func : args) {
            val funcVar = sameDiff.getVariableForVertexId(func.resultVertexId());
            if(func.getResultShape() == null) {
                log.warn("Unable to initialize shape of " +  func + " for vertex id " + Arrays.toString(func.resultVertexId()));
                return;
            }

            if(!sameDiff.arrayAlreadyExistsForVertexId(func.resultVertexId())) {
                val arr = funcVar.getWeightInitScheme().create(func.getResultShape());
                sameDiff.putArrayForVertexId(func.resultVertexId(),arr);
                addInputArgument(arr);
            }
            else {
                //already exists, just add
                addInputArgument(sameDiff.getArrForVertexId(func.resultVertexId()));
            }


        }

    }

    @Override
    public List<DifferentialFunction> doDiff(List<DifferentialFunction> f1) {
        throw new UnsupportedOperationException("Please extend DynamicCustomOp to run samediff graph operations.");
    }

    @Override
    public String toString() {
        return opName();
    }

    @Override
    public int depth() {
        int maxDepth = 0;
        for(DifferentialFunction func : args()) {
            maxDepth = Math.max(maxDepth,func.depth());
        }

        return maxDepth;
    }

    @Override
    public List<DifferentialFunction> outputs() {
        return Arrays.asList(outputFunctions);
    }




    public static class SameDiffBuilder extends DynamicCustomOpsBuilder {
        private SameDiff sameDiff;
        private List<DifferentialFunction> args = new ArrayList<>();
        private List<DifferentialFunction> outputs = new ArrayList<>();
        private SameDiffBuilder(String opName,SameDiff sameDiff) {
            this(opName,sameDiff,0,0,0,false,0,0);
        }

        protected SameDiffBuilder(String opName, SameDiff sameDiff,long hash, int numInputs, int numOutputs, boolean inplaceAllowed, int numTArguments, int numIArguments) {
            super(opName, hash, numInputs, numOutputs, inplaceAllowed, numTArguments, numIArguments);
            this.sameDiff = sameDiff;
        }

        public SameDiffBuilder sameDiff(SameDiff sameDiff) {
            this.sameDiff = sameDiff;
            return this;
        }

        @Override
        public DynamicCustomOpsBuilder addInputs(INDArray... inputs) {
            throw new UnsupportedOperationException("Unable to add direct ndarrays. Please use the normal builder for that.");
        }

        @Override
        public DynamicCustomOpsBuilder addOutputs(INDArray... outputs) {
            throw new UnsupportedOperationException("Unable to add direct ndarrays. Please use the normal builder for that.");

        }


        public DynamicCustomOpsBuilder addInputs(DifferentialFunction... inputs) {
            for(DifferentialFunction  function : inputs) {
                args.add(function);
            }

            return this;
        }

        public DynamicCustomOpsBuilder addOutputs(DifferentialFunction... outputs) {
            this.outputs.addAll(Arrays.asList(outputs));
            return this;

        }


        @Override
        public DynamicCustomOp build() {
            DynamicCustomOp ret =  super.build();
            sameDiff.associateFunctionsAsArgs(args.toArray(new DifferentialFunction[args.size()]),ret);
            ret.setSameDiff(sameDiff);
            ret.outputShapes = outputShapes;
            if(outputs.isEmpty() && !outputShapes.isEmpty()) {
                for (int i = 0; i < outputShapes.size(); i++) {
                    outputs.add(sameDiff.var(sameDiff.generateVariableName(
                            "dynamicoutput-" + i + "-" + UUID.randomUUID().toString(),
                            false,ret.args()),outputShapes.get(i)));
                }

            }

            ret.outputFunctions = outputs.toArray(new DifferentialFunction[outputs.size()]);
            return ret;
        }
    }


    @Override
    public String onnxName() {
        throw new NoOpNameFoundException("No onnx op opName found for " +  opName());
    }

    @Override
    public String tensorflowName() {
        throw new NoOpNameFoundException("No tensorflow op opName found for " +  opName());
    }


    @Override
    public Op.Type opType() {
        return Op.Type.CUSTOM;
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {

    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {

    }

    @Override
    public void initWithArrays(Map<String, INDArray> arrayMap, Object... extraArgs) {
        val args = args();
        for(int i = 0; i < args().length; i++) {
            val var = sameDiff.getVariableForVertexId(args()[i].resultVertexId());
            val func = args[i];
            if(func instanceof DynamicCustomOp) {
                DynamicCustomOp dynamicCustomOp = (DynamicCustomOp) func;
                dynamicCustomOp.addArrayInputArguments();
            }


            if(var != null) {
                if(var.getArr() == null) {
                    int[] shape = sameDiff.getShapeForVertexId(var.getVertexId());
                    if(shape == null) {
                        shape = func.getResultShape();
                    }
                    if(shape == null) {
                        throw new ND4JIllegalStateException("Unable to resolve shape for variable " + var.getVarName());
                    }
                    sameDiff.putArrayForVertexId(var.resultVertexId(),var.getWeightInitScheme().create(shape));
                }
            }
        }
    }

    public static SameDiffBuilder sameDiffBuilder(String opName, SameDiff sameDiff) {
        return new SameDiffBuilder(opName,sameDiff);
    }

    public static class DynamicCustomOpsBuilder {
        protected String opName;
        protected int numInputs;
        protected int numOutputs;
        protected int numTArguments;
        protected int numIArguments;
        protected boolean inplaceCall;
        protected boolean inplaceAllowed;
        protected long opHash;
        protected List<int[]> outputShapes = new ArrayList<>();

        private List<INDArray> inputArguments = new ArrayList<>();
        private List<INDArray> outputArguments = new ArrayList<>();
        private List<Double> tArguments = new ArrayList<>();
        private List<Integer> iArguments = new ArrayList<>();

        protected DynamicCustomOpsBuilder(String opName, long hash, int numInputs, int numOutputs, boolean inplaceAllowed, int numTArguments, int numIArguments) {
            this.opHash = hash;
            this.opName = opName;
            this.numInputs = numInputs;
            this.numOutputs = numOutputs;
            this.numIArguments = numIArguments;
            this.numTArguments = numTArguments;
            this.inplaceAllowed = inplaceAllowed;
        }

        /**
         * This method
         * takes arbitrary number of input INDArrays in, as Op input
         * Note that this ACCUMULATES arguments. You are able to call this method
         * multiple times and it will add arguments to a list.
         * PLEASE NOTE: this method does NOT validate lengths/shapes.
         *
         * @param inputs
         * @return
         */
        public DynamicCustomOpsBuilder addInputs(INDArray... inputs) {
            // if we have positive value as numInputs - we should ensure equal amount of arguments
            if (numInputs >= 0) {
                if (inputs == null)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numInputs + " arguments. Null was passed instead.");

                if (numInputs > inputs.length)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numInputs + " arguments, but " + inputs.length + " was passed to constructor");
            }

            for (val in: inputs)
                inputArguments.add(in);

            return this;
        }

        /**
         * This method takes arbitrary number of
         * output INDArrays in, to store operation result
         * Note that this ACCUMULATES arguments. You are able to call this method
         * multiple times and it will add arguments to a list.
         * PLEASE NOTE: this method does NOT validate lengths/shapes.
         *
         * @param outputs
         * @return
         */
        public DynamicCustomOpsBuilder addOutputs(INDArray... outputs) {
            if (numOutputs >= 0) {
                if (outputs == null)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numOutputs + " arguments. Null was passed instead.");

                if (numOutputs > outputs.length)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numOutputs + " arguments, but " + outputs.length + " was passed to constructor");
            }

            for (val in: outputs)
                outputArguments.add(in);

            return this;
        }

        /**
         * Whether an op call is in place or not.
         * @param reallyCall
         * @return
         */
        public DynamicCustomOpsBuilder callInplace(boolean reallyCall) {
            if (reallyCall && !inplaceAllowed)
                throw new ND4JIllegalStateException("Requested op can't be called inplace");

            this.inplaceCall = reallyCall;
            return this;
        }

        /**
         * This method takes arbitrary number of Integer arguments for op,
         * Note that this ACCUMULATES arguments. You are able to call this method
         * multiple times and it will add arguments to a list.
         * PLEASE NOTE: this method does NOT validate values.
         *
         * @param iargs
         * @return
         */
        public DynamicCustomOpsBuilder addIntegerArguments(Integer... iargs) {
            if (numIArguments >= 0) {
                if (iargs == null)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects " + numIArguments + " integer arguments. Null was passed instead.");

                if (numIArguments > iargs.length)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numIArguments + " integer arguments, but " + iargs.length + " was passed to constructor");
            }

            for (val in: iargs)
                iArguments.add(in);

            return this;
        }

        /**
         * This method takes arbitrary number of Integer arguments for op,
         * Note that this ACCUMULATES arguments. You are able to call this method
         * multiple times and it will add arguments to a list.
         * PLEASE NOTE: this method does NOT validate values.
         *
         * @param arg
         * @return
         */
        public DynamicCustomOpsBuilder addIntegerArguments(int arg) {
            if (numIArguments != 1 && numIArguments > 0)
                throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects " + numIArguments + " integer arguments. One arg was passed instead.");

            iArguments.add(arg);

            return this;
        }

        /**
         * This method takes arbitrary number of Integer arguments for op,
         * Note that this ACCUMULATES arguments. You are able to call this method
         * multiple times and it will add arguments to a list.
         * PLEASE NOTE: this method does NOT validate values.
         *
         * @param iargs
         * @return
         */
        public DynamicCustomOpsBuilder addIntegerArguments(int... iargs) {
            if (numIArguments >= 0) {
                if (iargs == null)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numIArguments + " integer arguments. Null was passed instead.");

                if (numIArguments > iargs.length)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numIArguments + " integer arguments, but " + iargs.length + " was passed to constructor");
            }

            for (val in: iargs)
                iArguments.add(in);

            return this;
        }

        /**
         * This method takes arbitrary number of Double arguments for op,
         * Note that this ACCUMULATES arguments. You are able to call this method
         * multiple times and it will add arguments to a list.
         * PLEASE NOTE: this method does NOT validate values.
         *
         * @return
         */
        public DynamicCustomOpsBuilder addFloatingPointArguments(Double... targs) {
            if (numTArguments >= 0) {
                if (targs == null)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numTArguments + " integer arguments. Null was passed instead.");

                if (numTArguments > targs.length)
                    throw new ND4JIllegalStateException("CustomOp [" + opName + "] expects at least " + numTArguments + " integer arguments, but " + targs.length + " was passed to constructor");
            }

            for (val in: targs)
                tArguments.add(in);

            return this;
        }


        /**
         * Adds an oup
         * @param shape
         * @return
         */
        public DynamicCustomOpsBuilder addOutputShape(int[] shape) {
            this.outputShapes.add(shape);
            return this;
        }




        public DynamicCustomOp build() {
            // Eventually we probably will lift this restriction
            //if (!inplaceCall && outputArguments.size() == 0)
            //    throw new ND4JIllegalStateException("If operation is not-inplace, it must have outputs defined");

            val result = new DynamicCustomOp(opName);
            result.inputArguments = inputArguments;
            result.outputArguments = outputArguments;
            result.iArguments = iArguments;
            result.tArguments = tArguments;
            result.inplaceCall = inplaceCall;
            result.hash = opHash;
            result.outputShapes = outputShapes;

            return result;
        }
    }
}
