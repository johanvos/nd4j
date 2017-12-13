package org.nd4j.linalg.api.ops.random.impl;

import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Range Op implementation, generates from..to distribution within Z
 *
 * @author raver119@gmail.com
 */
public class Range extends DynamicCustomOp {
    private Double from;
    private Double to;
    private Double delta;
    //used for initWithArrays when there are place holder
    //values that need to be resolved
    private int fromVertexId,toVertexId,deltaVertexId;
    public Range() {
        // no-op
    }



    @Override
    public int opNum() {
        return 4;
    }

    @Override
    public String opName() {
        return "range";
    }

    @Override
    public String onnxName() {
        return "Range";
    }

    @Override
    public String tensorflowName() {
        return "Range";
    }



    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        super.initFromTensorFlow(nodeDef, initWith, attributesForNode, graph);

        NodeDef startNode = null,endNode = null,deltaNode = null;
        for(val  node : graph.getNodeList()) {
            if(node.getName().equals(nodeDef.getInput(0))) {
                startNode = node;
            }
            if(node.getName().equals(nodeDef.getInput(1))) {
                endNode = node;
            }
            if(node.getName().equals(nodeDef.getInput(2))) {
                deltaNode = node;
            }

            if(startNode != null && endNode != null && deltaNode != null)
                break;
        }

        val start = TFGraphMapper.getInstance().getNDArrayFromTensor("value",startNode,graph);
        val end = TFGraphMapper.getInstance().getNDArrayFromTensor("value",endNode,graph);
        val delta = TFGraphMapper.getInstance().getNDArrayFromTensor("value",deltaNode,graph);
        val outputVars = outputVariables();
        if(start != null && end != null && delta != null) {
            this.from = start.getDouble(0);
            this.to = end.getDouble(0);
            this.delta = delta.getDouble(0);
            addTArgument(this.from,this.to,this.delta);
            val outputVertexId = outputVariables()[0].getVertexId();
            if(sameDiff.getArrForVertexId(outputVertexId) == null) {
                val arr = Nd4j.create(outputVars[0].getShape());
                sameDiff.putArrayForVertexId(outputVertexId, arr);
                addOutputArgument(arr);
            }
        }

        this.fromVertexId = sameDiff.getVariable(startNode.getName()).getVertexId();
        this.toVertexId = sameDiff.getVariable(endNode.getName()).getVertexId();
        this.deltaVertexId = sameDiff.getVariable(deltaNode.getName()).getVertexId();

    }



    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        super.initFromOnnx(node, initWith, attributesForNode, graph);
    }



    @Override
    public void initWithArrays(Map<String, INDArray> arrayMap, Object... extraArgs) {
        super.initWithArrays(arrayMap);
        val start = sameDiff.getVariableForVertexId(fromVertexId).getArr();
        val end = sameDiff.getVariableForVertexId(toVertexId).getArr();
        val delta = sameDiff.getVariableForVertexId(deltaVertexId).getArr();
        val outputVars = outputVariables();

        if(start != null && end != null && delta != null) {
            this.from = start.getDouble(0);
            this.to = end.getDouble(0);
            this.delta = delta.getDouble(0);
            addTArgument(this.from,this.to,this.delta);
            if(sameDiff.getArrForVertexId(outputVars[0].getVertexId()) == null) {
                val arr = Nd4j.create(outputVars[0].getShape());
                sameDiff.putArrayForVertexId(outputVars[0].getVertexId(), arr);
                addOutputArgument(arr);
            }

        }
        else {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Not all values of range mapped. ");
            errorMessage.append("Start status is null " + (start == null));
            errorMessage.append("End status is null " + (end == null));
            errorMessage.append("Delta status is null " + (delta == null));
            throw new ND4JIllegalStateException(errorMessage.toString());
        }

    }


    @Override
    public List<int[]> calculateOutputShape() {
        val iArgs = iArgs();
        val tArgs = tArgs();
        int cnt = 0;

        if (iArgs.length > 0) {
            int start = iArgs[0];
            int stop = iArgs[1];
            int step = iArgs[2];

            double e = (double) start;
            if (start > stop) {
                while (e > (double) stop) {
                    cnt++;
                    e = (double) step > 0.0 ? e - step : e + step;
                }
            } else {
                while (e < (double) stop) {
                    cnt++;
                    e += step;
                }
            }

            return Arrays.asList(new int[]{1,cnt});
        }

        else if (tArgs.length > 0) {
            double start = tArgs[0];
            double stop = tArgs[1];
            double step = tArgs[2];

            double e = start;
            if (start > stop) {
                while (e > stop) {
                    cnt++;
                    e = step > 0.0 ? e - step : e + step;
                }
            } else {
                while (e < stop) {
                    cnt++;
                    e += step;
                }
            }

            return Arrays.asList(new int[]{1,cnt});
        }


        throw new ND4JIllegalStateException("Unable to compute shape. No arguments found!");

    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        throw new UnsupportedOperationException("Unable to differentiate array creation routine");
    }

    @Override
    public Op.Type opType() {
        return Op.Type.CUSTOM;
    }
}
