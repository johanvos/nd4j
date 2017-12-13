package org.nd4j.linalg.api.ops;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Slf4j
public abstract class BaseBroadcastOp extends BaseOp implements BroadcastOp {

    protected int[] dimension;


    public BaseBroadcastOp(SameDiff sameDiff,
                           SDVariable i_v1,
                           SDVariable i_v2,
                           int[] dimension) {
        this(sameDiff, i_v1, i_v2, false, dimension);
    }

    public BaseBroadcastOp(SameDiff sameDiff,
                           SDVariable i_v1,
                           SDVariable i_v2,
                           boolean inPlace,
                           int[] dimension) {
        super(sameDiff, inPlace, new Object[]{i_v2});
        if (i_v1 != null && i_v2 != null) {
            sameDiff.addArgsFor(new SDVariable[]{i_v1, i_v2}, this);
            f().validateDifferentialFunctionsameDiff(i_v1);
            f().validateDifferentialFunctionsameDiff(i_v2);
            this.sameDiff = sameDiff;
            this.inPlace = inPlace;
            this.dimension = dimension;
            val var = sameDiff.var(i_v1.getVarName() + "-" + opName() + "-" + i_v2.getVarName() ,Shape.getBroadcastDimensions(i_v1.getShape(), i_v2.getShape()));
            sameDiff.addOutgoingFor(new SDVariable[]{var},this);
            sameDiff.putShapeForVertexId(outputVariables()[0].getVertexId(), Shape.getBroadcastDimensions(i_v1.getShape(), i_v2.getShape()));
            this.xVertexId = i_v1.getVertexId();
            this.yVertexId = i_v2.getVertexId();
            this.zVertexId = var.getVertexId();

        } else {
            throw new IllegalArgumentException("Input not null variables.");
        }

    }

    public BaseBroadcastOp(SameDiff sameDiff) {
        this.sameDiff = sameDiff;
    }

    public BaseBroadcastOp(SameDiff sameDiff,
                           SDVariable i_v1,
                           SDVariable i_v2,
                           int[] dimension,
                           Object[] extraArgs) {
        super(sameDiff, extraArgs);
        this.dimension = dimension;
        if (i_v1 != null && i_v2 != null) {
            sameDiff.addArgsFor(new SDVariable[]{i_v1, i_v2}, this);

            f().validateDifferentialFunctionsameDiff(i_v1);
            f().validateDifferentialFunctionsameDiff(i_v2);

            this.sameDiff = sameDiff;
            val var = sameDiff.var(i_v1.getVarName() + "-" + opName() + "-" + i_v2.getVarName() ,
                    Shape.getBroadcastDimensions(i_v1.getShape(), i_v2.getShape()));
            sameDiff.addOutgoingFor(new SDVariable[]{var},this);
            this.xVertexId = i_v1.getVertexId();
            this.yVertexId = i_v2.getVertexId();
            this.zVertexId = var.getVertexId();
            sameDiff.addOutgoingFor(new SDVariable[]{var},this);


        } else {
            throw new IllegalArgumentException("Input not null variables.");
        }


    }


    public BaseBroadcastOp(SameDiff sameDiff, SDVariable i_v, int[] dimension, boolean inPlace) {
        this(sameDiff, i_v, i_v.getShape(), inPlace, dimension, null);
    }

    public BaseBroadcastOp(SameDiff sameDiff,
                           SDVariable i_v,
                           int[] shape,
                           boolean inPlace,
                           int[] dimension,
                           Object[] extraArgs) {
        super(sameDiff, inPlace, extraArgs);
        this.dimension = dimension;
        if (i_v != null) {
            sameDiff.addArgsFor(new SDVariable[]{i_v}, this);
            f().validateDifferentialFunctionsameDiff(i_v);
            sameDiff.putShapeForVertexId(outputVariables()[0].getVertexId(), shape);
            val var = sameDiff.var(i_v.getVarName() + "-" + opName() ,
                   i_v.getShape(),i_v.depth()  + 1);
            this.xVertexId = i_v.getVertexId();
            this.zVertexId = var.getVertexId();
            sameDiff.addOutgoingFor(new SDVariable[]{var},this);

        } else {
            throw new IllegalArgumentException("Input not null variable.");
        }


    }


    public BaseBroadcastOp(SameDiff sameDiff,
                           SDVariable i_v,
                           int[] dimension,
                           Object[] extraArgs) {
        this(sameDiff, i_v, i_v.getShape(), false, dimension, extraArgs);
    }

    public BaseBroadcastOp(INDArray x, INDArray y, INDArray z, int... dimension) {
        super(x, y, z, x.lengthLong());
        this.dimension = dimension;
        for (int i = 0; i < dimension.length; i++)
            if (dimension[i] < 0)
                dimension[i] += x.rank();

    }

    @Override
    public Type opType() {
        return Type.BROADCAST;
    }

    /**
     * Calculate the output shape for this op
     *
     * @return
     */
    public List<int[]> calculateOutputShape() {
        List<int[]> ret = new ArrayList<>();
        if (larg().getShape() != null && rarg().getShape() != null)
            ret.add(Shape.broadcastOutputShape(larg().getShape(), rarg().getShape()));
        ret.add(larg().getShape());
        return ret;
    }


    @Override
    public int[] getDimension() {
        if (dimension == null) {
            dimension = Shape.getBroadcastDimensions(larg().getShape(), rarg().getShape());
        }
        return dimension;
    }

    @Override
    public void initWithArrays(Map<String, INDArray> arrayMap, Object... extraArgs) {
        super.initWithArrays(arrayMap);
        if (args().length > 1 && larg() != null && rarg() != null && larg().getShape() != null && rarg().getShape() != null) {
            if (Shape.isRowVectorShape(rarg().getShape())) {
                this.dimension = new int[]{1};
            } else if(Shape.isColumnVectorShape(rarg().getShape()))
                this.dimension = new int[]{0};
            else if (args().length > 1 && larg() != null && rarg() != null && larg().getShape() != null && rarg().getShape() != null && !sameDiff.isPlaceHolder(larg().getVertexId()) && !sameDiff.isPlaceHolder(rarg().getVertexId()))
                this.dimension = Shape.getBroadcastDimensions(larg().getShape(), rarg().getShape());
        }

    }

    @Override
    public void setDimension(int... dimension) {
        this.dimension = dimension;
    }


    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
    }



    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {

    }
}
