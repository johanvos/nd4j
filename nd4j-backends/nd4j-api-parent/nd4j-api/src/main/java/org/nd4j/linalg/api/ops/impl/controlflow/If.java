package org.nd4j.linalg.api.ops.impl.controlflow;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import onnx.OnnxProto3;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.CustomOp;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.weightinit.impl.ZeroInitScheme;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.*;

/**
 * Equivalent to tensorflow's conditional op.
 * Runs one of 2 {@link SameDiff.SameDiffFunctionDefinition}
 * depending on a predicate {@link org.nd4j.autodiff.samediff.SameDiff.SameDiffConditional}
 *
 *
 * @author Adam Gibson
 */
@NoArgsConstructor
public class If extends DifferentialFunction implements CustomOp {

    @Getter
    protected SameDiff loopBodyExecution,predicateExecution,falseBodyExecution;


    @Getter
    protected SameDiff.SameDiffConditional predicate;
    @Getter
    protected SameDiff.SameDiffFunctionDefinition trueBody,falseBody;

    @Getter
    protected String blockName,trueBodyName,falseBodyName;

    @Getter
    protected SDVariable[] inputVars;

    @Getter
    protected Boolean trueBodyExecuted = null;

    @Getter
    protected SDVariable targetBoolean;

    protected SDVariable dummyResult;

    @Getter
    @Setter
    protected SDVariable[] outputVars;

    public If(If ifStatement) {
        this.sameDiff = ifStatement.sameDiff;
        this.outputVars = ifStatement.outputVars;
        this.falseBodyExecution = ifStatement.falseBodyExecution;
        this.trueBodyExecuted = ifStatement.trueBodyExecuted;
        this.falseBody = ifStatement.falseBody;
        this.trueBodyExecuted = ifStatement.trueBodyExecuted;
        this.dummyResult = ifStatement.dummyResult;
        sameDiff.addArgsFor(ifStatement.inputVars,this);
        this.inputVars = ifStatement.inputVars;
        this.dummyResult =  this.sameDiff.var("dummyresult-" + UUID.randomUUID().toString(),new int[]{1,1},new ZeroInitScheme('f'),sameDiff.graph().nextVertexId(),0);
        sameDiff.putShapeForVertexId(dummyResult.getVertexId(),new int[]{1,1});

        int[] inputEdges = new int[inputVars.length];

        for(int i = 0; i < inputEdges.length; i++) {
            inputEdges[i] = inputVars[i].getVertexId();
        }

        sameDiff.addOutgoingFor(new SDVariable[] {dummyResult},this);



    }

    @Builder
    public If(String blockName,
              SameDiff parent,
              SDVariable[] inputVars,
              SameDiff.SameDiffFunctionDefinition conditionBody,
              SameDiff.SameDiffConditional predicate,
              SameDiff.SameDiffFunctionDefinition trueBody,
              SameDiff.SameDiffFunctionDefinition falseBody) {
        this.sameDiff = parent;

        this.inputVars = inputVars;
        this.predicate = predicate;
        this.trueBody = trueBody;
        this.falseBody = falseBody;
        this.blockName = blockName;
        //need to add the op to the list of ops to be executed when running backwards
        sameDiff.addArgsFor(inputVars,this);
        this.dummyResult =  parent.var("dummyresult-" + UUID.randomUUID().toString(),new int[]{1,1},new ZeroInitScheme('f'),parent.graph().nextVertexId(),0);



        //create a samediff sub graph for running just the execution
        //return a reference to the loop for referencing during actual execution
        SameDiff sameDiff = SameDiff.create();
        //store the reference to the result array and the same diff execution instance
        this.targetBoolean = predicate.eval(sameDiff,conditionBody, inputVars);
        this.predicateExecution = sameDiff;
        //store references to the loop body
        String trueBodyName = "true-body-" + UUID.randomUUID().toString();
        this.trueBodyName = trueBodyName;

        String falseBodyName = "false-body-" + UUID.randomUUID().toString();
        this.falseBodyName = trueBodyName;

        //running define function will setup a proper same diff instance
        this.loopBodyExecution = parent.defineFunction(trueBodyName,trueBody,inputVars);
        this.falseBodyExecution = parent.defineFunction(falseBodyName,falseBody,inputVars);
        parent.defineFunction(blockName,conditionBody,inputVars);
        parent.putSubFunction("predicate-eval-body-" + UUID.randomUUID().toString(),sameDiff);
        //get a reference to the actual loop body
        this.loopBodyExecution = parent.getFunction(trueBodyName);
        parent.addOutgoingFor(new SDVariable[]{dummyResult},this);
    }


    /**
     * Toggle whether the true body was executed
     * or the false body
     * @param trueBodyExecuted
     */
    public void exectedTrueOrFalse(boolean trueBodyExecuted)  {
        if(trueBodyExecuted)
            this.trueBodyExecuted = true;
        else
            this.trueBodyExecuted = false;
    }


    @Override
    public SDVariable[] outputVariables() {
        return new SDVariable[0];
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        List<SDVariable> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(new IfDerivative(this).outputVariables()));
        return ret;
    }

    @Override
    public String toString() {
        return opName();
    }

    @Override
    public String opName() {
        return "if";
    }

    @Override
    public long opHash() {
        return 0;
    }

    @Override
    public boolean isInplaceCall() {
        return false;
    }

    @Override
    public INDArray[] outputArguments() {
        return new INDArray[0];
    }

    @Override
    public INDArray[] inputArguments() {
        return new INDArray[0];
    }

    @Override
    public int[] iArgs() {
        return new int[0];
    }

    @Override
    public double[] tArgs() {
        return new double[0];
    }

    @Override
    public void addIArgument(int... arg) {

    }

    @Override
    public void removeIArgument(Integer arg) {

    }

    @Override
    public Integer getIArgument(int index) {
        return null;
    }

    @Override
    public int numIArguments() {
        return 0;
    }

    @Override
    public void addTArgument(double... arg) {

    }

    @Override
    public void removeTArgument(Double arg) {

    }

    @Override
    public Double getTArgument(int index) {
        return null;
    }

    @Override
    public int numTArguments() {
        return 0;
    }

    @Override
    public void addInputArgument(INDArray... arg) {

    }

    @Override
    public void removeInputArgument(INDArray arg) {

    }

    @Override
    public INDArray getInputArgument(int index) {
        return null;
    }

    @Override
    public int numInputArguments() {
        return 0;
    }

    @Override
    public void addOutputArgument(INDArray... arg) {

    }

    @Override
    public void removeOutputArgument(INDArray arg) {

    }

    @Override
    public INDArray getOutputArgument(int index) {
        return null;
    }

    @Override
    public int numOutputArguments() {
        return 0;
    }

    @Override
    public Op.Type opType() {
        return  Op.Type.CONDITIONAL;
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {

    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {

    }



    @Override
    public List<int[]> calculateOutputShape() {
        return Arrays.asList(new int[]{1,1});
    }

    @Override
    public String onnxName() {
        throw new NoOpNameFoundException("No onnx op opName found for " + opName());
    }

    @Override
    public String tensorflowName() {
        return "cond";
    }
}
