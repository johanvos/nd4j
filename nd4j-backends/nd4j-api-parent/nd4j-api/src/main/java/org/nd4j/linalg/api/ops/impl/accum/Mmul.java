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

package org.nd4j.linalg.api.ops.impl.accum;

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.blas.params.MMulTranspose;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Matrix multiplication/dot product
 *
 * @author Adam Gibson
 */
public class Mmul extends TensorMmul {


    /**
     *
     * @param sameDiff
     * @param i_v1
     * @param i_v2
     * @param mMulTranspose
     */
    public Mmul(SameDiff sameDiff,
                SDVariable i_v1,
                SDVariable i_v2,
                MMulTranspose mMulTranspose) {
        super(sameDiff,
                i_v1,
                i_v2, new int[][] {
                        {1},{0}
                },mMulTranspose);

        this.mMulTranspose = mMulTranspose;
    }


    /**
     *
     * @param sameDiff
     * @param i_v1
     * @param i_v2
     */
    public Mmul(SameDiff sameDiff,
                SDVariable i_v1,
                SDVariable i_v2) {
        this(sameDiff,i_v1,i_v2,MMulTranspose.allFalse());
    }

    public Mmul(INDArray x, INDArray y, int[][] axes, MMulTranspose mMulTranspose) {
        super(x, y, axes);
        this.mMulTranspose = mMulTranspose;
    }

    public Mmul(INDArray x, INDArray y, INDArray z, MMulTranspose mMulTranspose) {
        super(x, y, z,  new int[][] {
                {1},{0}
        });
        this.mMulTranspose = mMulTranspose;
    }

    public Mmul() {}

    @Override
    public List<int[]> calculateOutputShape() {

        return super.calculateOutputShape();
    }



    @Override
    public String onnxName() {
        return "MatMul";
    }

    @Override
    public String tensorflowName() {
        return "MatMul";
    }


    @Override
    public int opNum() {
        return 3;
    }

    @Override
    public String opName() {
        return "mmul";
    }



    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v1) {
        List<SDVariable> ret = new ArrayList<>();
        SDVariable setup = sameDiff.setupFunction(i_v1.get(0));
        SDVariable gradWrtX = sameDiff.setupFunction(f().reshape(f().mmul(setup,rarg(),
                MMulTranspose.builder()
                        .transposeB(!mMulTranspose.isTransposeB())
                        .transposeResult(mMulTranspose.isTransposeA())
                        .build()),larg().getShape()));

        SDVariable gradWrtY = sameDiff.setupFunction(f().reshape(f().mmul(larg(),setup,
                MMulTranspose.builder()
                        .transposeA(!mMulTranspose.isTransposeA())
                        .transposeResult(mMulTranspose.isTransposeB())
                        .build()),rarg().getShape()));

        ret.add(gradWrtX);
        ret.add(gradWrtY);
        return ret;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mmul mmul = (Mmul) o;

        return mMulTranspose != null ? mMulTranspose.equals(mmul.mMulTranspose) : mmul.mMulTranspose == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mMulTranspose != null ? mMulTranspose.hashCode() : 0);
        return result;
    }
}

