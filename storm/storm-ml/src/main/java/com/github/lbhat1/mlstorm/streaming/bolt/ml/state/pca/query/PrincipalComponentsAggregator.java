package com.github.lbhat1.mlstorm.streaming.bolt.ml.state.pca.query;

import storm.trident.operation.CombinerAggregator;
import storm.trident.tuple.TridentTuple;
import com.github.lbhat1.mlstorm.streaming.utils.fields.FieldTemplate;

 /*
 * Copyright 2013-2015 Lakshmisha Bhat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


public class PrincipalComponentsAggregator implements CombinerAggregator<double[][]> {
    @Override
    public double[][] init(final TridentTuple components) {
        if (components.getValueByField(FieldTemplate.FieldConstants.PCA.PCA_COMPONENTS) != null) {
            return (double[][]) components.getValueByField(FieldTemplate.FieldConstants.PCA.PCA_COMPONENTS);
        } else {
            return null;
        }
    }

    @Override
    public double[][] combine(final double[][] partition1, final double[][] partition2) {
        if (partition1 == null) {
            return partition2;
        } else if (partition2 == null) {
            return partition1;
        } else {
            final double[][] combined = new double[partition1.length + partition2.length][partition1[0].length];
            int i = 0;
            for (; i < partition1.length; i++) {
                System.arraycopy(partition1, i, combined, i, partition1[i].length);
            }
            for (int j = 0; j < partition2.length && i < combined.length; i++, j++) {
                System.arraycopy(partition2, j, combined, i, partition2[j].length);
            }
            return combined;
        }
    }

    @Override
    public double[][] zero() {
        return null;
    }
}
