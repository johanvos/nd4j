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

package org.nd4j.linalg.api.ops.impl.shape;

import lombok.val;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.util.ArrayUtil;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tile function
 *
 * @author Adam Gibson
 */
public class Tile extends DynamicCustomOp {
    private int[] axis;

    public Tile(SameDiff sameDiff, SDVariable i_v, int[] axis) {
        super(null,sameDiff, new SDVariable[]{i_v}, false);
        this.axis = axis;
        addIArgument(axis);
    }

    public Tile() {}

    @Override
    public List<int[]> calculateOutputShape() {
        val inputShape = args()[0].getShape();
        val shape = ArrayUtil.copy(inputShape);
        if(axis.length == inputShape.length){
            for(int i = 0; i < axis.length; i++) {
                shape[i] = inputShape[i] * axis[i];
            }
        }

       else if(org.nd4j.linalg.api.shape.Shape.isVector(shape)) {
            if(inputShape[0] == 1) {
                inputShape[1] *= axis[0];
            }
            else if(inputShape[1] == 1) {
                inputShape[0] *= axis[0];
            }
        }
        else if(axis.length == 1) {
             shape[shape.length - 1] = axis[0];
        }


        return Arrays.asList(shape);
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        val lastNode = TFGraphMapper.getInstance().getNodeWithNameFromGraph(graph,nodeDef.getInput(nodeDef.getInputCount() - 1));
        val arr = TFGraphMapper.getInstance().getNDArrayFromTensor("value",lastNode,graph);
        if(arr != null) {
            this.axis = arr.data().asInt();
            addIArgument(axis);
        }


    }

    @Override
    public String opName() {
        return "tile";
    }

    @Override
    public String onnxName() {
        return "Tile";
    }

    @Override
    public String tensorflowName() {
        return "Tile";
    }


    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v) {
        throw new UnsupportedOperationException();
    }

}
