/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */

package org.nd4j.linalg.api.ops.impl.accum.distances;

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseAccumulation;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;
import org.nd4j.linalg.factory.Nd4j;

import java.util.List;

/**
 * Jaccard distance (dissimilarity)
 *
 * @author raver119@gmail.com
 */
public class JaccardDistance extends BaseAccumulation {
    private Number constantNormalizedByNorm2X, constantNormalizedByNorm2Y;

    public JaccardDistance(SameDiff sameDiff, SDVariable i_v, int[] dimensions, Number constantNormalizedByNorm2X, Number constantNormalizedByNorm2Y) {
        super(sameDiff, i_v, dimensions);
        this.constantNormalizedByNorm2X = constantNormalizedByNorm2X;
        this.constantNormalizedByNorm2Y = constantNormalizedByNorm2Y;
    }

    public JaccardDistance(SameDiff sameDiff, SDVariable i_v, SDVariable i_v2, int[] dimensions, Number constantNormalizedByNorm2X, Number constantNormalizedByNorm2Y) {
        super(sameDiff, i_v, i_v2, dimensions);
        this.constantNormalizedByNorm2X = constantNormalizedByNorm2X;
        this.constantNormalizedByNorm2Y = constantNormalizedByNorm2Y;
    }

    public JaccardDistance() {
        passThrough = true;
    }

    public JaccardDistance(INDArray x, INDArray y, INDArray z, long n) {
        super(x, y, z, n);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public JaccardDistance(INDArray x, INDArray y, long n) {
        super(x, y, n);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public JaccardDistance(INDArray x) {
        super(x);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public JaccardDistance(INDArray x, INDArray y) {
        super(x, y);
        passThrough = Nd4j.getExecutioner().executionMode() == OpExecutioner.ExecutionMode.JAVA;
        extraArgs = new Object[2];
        extraArgs[0] = 0.0f;
        extraArgs[1] = 0.0f;
    }

    public JaccardDistance(INDArray x, INDArray y, INDArray z, boolean allDistances) {
        this(x, y, z, x.lengthLong());
    }

    public JaccardDistance(INDArray x, INDArray y, boolean allDistances) {
        this(x, y);
    }


    @Override
    public int opNum() {
        return 6;
    }

    @Override
    public String opName() {
        return "jaccarddistance";
    }


    @Override
    public void exec() {
        this.constantNormalizedByNorm2X = x.norm2Number();
        this.constantNormalizedByNorm2Y = y.norm2Number();
        this.extraArgs = new Object[] {0.0, constantNormalizedByNorm2X, constantNormalizedByNorm2Y};
        double dot = Nd4j.getBlasWrapper().dot(x, y);
        this.finalResult = dot / (constantNormalizedByNorm2X.doubleValue() * constantNormalizedByNorm2Y.doubleValue());
    }




    @Override
    public List<SDVariable> doDiff(List<SDVariable> f1) {
        return null;
    }

    @Override
    public String onnxName() {
        throw new NoOpNameFoundException("No onnx op opName found for " +  opName());

    }

    @Override
    public String tensorflowName() {
        throw new NoOpNameFoundException("No tensorflow op opName found for " +  opName());
    }



}
