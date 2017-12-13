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

package org.nd4j.linalg.api.ops;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Base scalar operation
 *
 * @author Adam Gibson
 */
@Slf4j
public abstract class BaseScalarOp extends BaseOp implements ScalarOp {
    @Getter
    @Setter
    protected Number num;
    public int[] opDimension;


    public BaseScalarOp() {}

    public BaseScalarOp(INDArray x, INDArray y, INDArray z, long n, Number num) {
        super(x, y, z, n);
        this.num = num;

        init(x, y, z, n);
    }

    public BaseScalarOp(INDArray x, Number num) {
        super(x);
        this.num = num;
         init(x, y, z, n);

    }





    public BaseScalarOp(SameDiff sameDiff,SDVariable i_v,Number scalar) {
        this(sameDiff,i_v,scalar,false,null);
    }

    public BaseScalarOp(SameDiff sameDiff,SDVariable i_v,Number scalar,boolean inPlace) {
        this(sameDiff,i_v,scalar,inPlace,null);
    }

    public BaseScalarOp(SameDiff sameDiff,
                           SDVariable i_v,
                           Number scalar,
                           boolean inPlace,
                           Object[] extraArgs) {
        super(sameDiff,inPlace,extraArgs);
        this.scalarValue = scalar;
        if (i_v != null) {
            val var = sameDiff.var(i_v.getVarName() + "-" + opName() + "-" + "-output",i_v.getShape(),i_v.depth() + 1);
            sameDiff.addArgsFor(new SDVariable[] {i_v},this);
            sameDiff.addOutgoingFor(new int[]{var.getVertexId()},this);
            this.xVertexId = i_v.getVertexId();
            this.zVertexId = var.getVertexId();
            f().validateDifferentialFunctionsameDiff(i_v);
        } else {
            throw new IllegalArgumentException("Input not null variable.");
        }

    }


    public BaseScalarOp(SameDiff sameDiff,
                           SDVariable i_v,
                           Number scalar,
                           Object[] extraArgs) {
        this(sameDiff,i_v,scalar,false,extraArgs);
    }


    @Override
    public SDVariable[] outputVariables() {
        return new SDVariable[] {sameDiff.getVariableForVertexId(sameDiff.graph().getToFor(new int[]{arg().getVertexId()})[0])};
    }

    @Override
    public List<int[]> calculateOutputShape() {
        List<int[]> ret = new ArrayList<>(1);
        ret.add(arg().getShape());
        return ret;
    }

    @Override
    public Type opType() {
        return Type.SCALAR;
    }

    @Override
    public void setScalar(Number scalar) {
        this.num = scalar;
    }

      @Override
    public Number scalar() {
        return num;
    }


    @Override
    public int[] getDimension() {
        return opDimension;
    }

    @Override
    public void setDimension(int... dimension) {
        this.opDimension = dimension;
    }



}
