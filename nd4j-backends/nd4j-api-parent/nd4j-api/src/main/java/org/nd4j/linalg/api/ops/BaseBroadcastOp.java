package org.nd4j.linalg.api.ops;

import com.google.common.base.Preconditions;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onnx.OnnxProto3;
import org.nd4j.autodiff.functions.DifferentialFunction;
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
                           DifferentialFunction i_v1,
                           DifferentialFunction i_v2,
                           int[] dimension) {
        this(sameDiff, i_v1, i_v2, false, dimension);
    }

    public BaseBroadcastOp(SameDiff sameDiff,
                           DifferentialFunction i_v1,
                           DifferentialFunction i_v2,
                           boolean inPlace,
                           int[] dimension) {
        super(sameDiff, inPlace, new Object[]{i_v2});
        if (i_v1 != null && i_v2 != null) {
            sameDiff.associateFunctionsAsArgs(new DifferentialFunction[]{sameDiff.setupFunction(i_v1), sameDiff.setupFunction(i_v2)}, this);
            f().validateDifferentialFunctionsameDiff(i_v1);
            f().validateDifferentialFunctionsameDiff(i_v2);
            f().validateFunctionReference(i_v1);
            f().validateFunctionReference(i_v2);
            this.sameDiff = sameDiff;
            this.inPlace = inPlace;
            this.dimension = dimension;
            ;
            addAsNewVertexId();
            sameDiff.putShapeForVertexId(vertexId, Shape.getBroadcastDimensions(i_v1.getResultShape(), i_v2.getResultShape()));
            f().addFunctionEdges(this);

        } else {
            throw new IllegalArgumentException("Input not null variables.");
        }

        Preconditions.checkState(sameDiff.setupFunction(this) == this);
    }

    public BaseBroadcastOp(SameDiff sameDiff) {
        this.sameDiff = sameDiff;
    }

    public BaseBroadcastOp(SameDiff sameDiff,
                           DifferentialFunction i_v1,
                           DifferentialFunction i_v2,
                           int[] dimension,
                           Object[] extraArgs) {
        super(sameDiff, extraArgs);
        this.dimension = dimension;
        if (i_v1 != null && i_v2 != null) {
            sameDiff.associateFunctionsAsArgs(new DifferentialFunction[]{sameDiff.setupFunction(i_v1), sameDiff.setupFunction(i_v2)}, this);

            f().validateDifferentialFunctionsameDiff(i_v1);
            f().validateDifferentialFunctionsameDiff(i_v2);

            this.sameDiff = sameDiff;
            addAsNewVertexId();
            sameDiff.putShapeForVertexId(vertexId, Shape.getBroadcastDimensions(i_v1.getResultShape(), i_v2.getResultShape()));
            f().addFunctionEdges(this);


        } else {
            throw new IllegalArgumentException("Input not null variables.");
        }

        Preconditions.checkState(sameDiff.setupFunction(this) == this);

    }


    public BaseBroadcastOp(SameDiff sameDiff, DifferentialFunction i_v, int[] dimension, boolean inPlace) {
        this(sameDiff, i_v, i_v.getResultShape(), inPlace, dimension, null);
    }

    public BaseBroadcastOp(SameDiff sameDiff,
                           DifferentialFunction i_v,
                           int[] shape,
                           boolean inPlace,
                           int[] dimension,
                           Object[] extraArgs) {
        super(sameDiff, inPlace, extraArgs);
        this.dimension = dimension;
        if (i_v != null) {
            sameDiff.associateFunctionsAsArgs(new DifferentialFunction[]{sameDiff.setupFunction(i_v)}, this);
            f().validateFunctionReference(i_v);
            f().validateDifferentialFunctionsameDiff(i_v);
            addAsNewVertexId();
            sameDiff.putShapeForVertexId(vertexId, shape);
            f().addFunctionEdges(this);

        } else {
            throw new IllegalArgumentException("Input not null variable.");
        }

        Preconditions.checkState(sameDiff.setupFunction(this) == this);

    }


    public BaseBroadcastOp(SameDiff sameDiff,
                           DifferentialFunction i_v,
                           int[] dimension,
                           Object[] extraArgs) {
        this(sameDiff, i_v, i_v.getResultShape(), false, dimension, extraArgs);
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
        if (larg().getResultShape() != null && rarg().getResultShape() != null)
            ret.add(Shape.broadcastOutputShape(larg().getResultShape(), rarg().getResultShape()));
        ret.add(larg().getResultShape());
        return ret;
    }

    @Override
    public int broadcastLength() {
        if (y == null)
            throw new IllegalStateException("Unable to get broad cast length for y, no y specified");
        return y.length();
    }

    @Override
    public int[] broadcastShape() {
        return calculateOutputShape().get(0);
    }

    @Override
    public int[] getDimension() {
        if (dimension == null) {
            dimension = Shape.getBroadcastDimensions(larg().getResultShape(), rarg().getResultShape());
        }
        return dimension;
    }

    @Override
    public void initWithArrays(Map<String, INDArray> arrayMap, Object... extraArgs) {
        super.initWithArrays(arrayMap);
        if (args().length > 1 && larg() != null && rarg() != null && larg().getResultShape() != null && rarg().getResultShape() != null) {
            if (Shape.isRowVectorShape(rarg().getResultShape())) {
                this.dimension = new int[]{1};
            } else if(Shape.isColumnVectorShape(rarg().getResultShape()))
                this.dimension = new int[]{0};
            else if (args().length > 1 && larg() != null && rarg() != null && larg().getResultShape() != null && rarg().getResultShape() != null && !sameDiff.isPlaceHolder(larg().resultVertexId()) && !sameDiff.isPlaceHolder(rarg().resultVertexId()))
                this.dimension = Shape.getBroadcastDimensions(larg().getResultShape(), rarg().getResultShape());
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
