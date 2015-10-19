package com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.cluster.update;

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

import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.cluster.KmeansClustererState;
import com.github.lbhat1.mlstorm.streaming.utils.MlStormFeatureVectorUtils;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.state.StateUpdater;
import storm.trident.tuple.TridentTuple;
import com.github.lbhat1.mlstorm.streaming.utils.KeyValuePair;
import com.github.lbhat1.mlstorm.streaming.utils.fields.FieldTemplate;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: lbhat <laksh85@gmail.com>
 * Date: 12/17/13
 * Time: 5:08 PM
 */
public class KmeansClusterUpdater implements StateUpdater<KmeansClustererState> {
    private final FieldTemplate template;
    private int localPartition, numPartitions;

    public KmeansClusterUpdater(FieldTemplate template) {
        this.template = template;
    }

    @Override
    public void updateState(final KmeansClustererState state,
                            final List<TridentTuple> tuples,
                            final TridentCollector collector) {
        for (TridentTuple tuple : tuples) {
            final KeyValuePair<Object, double[]> keyValue = MlStormFeatureVectorUtils.getKeyValueFromMlStormFeatureVector(template, tuple);
            final int key = (Integer) keyValue.getKey();
            final double[] fv = keyValue.getValue();

            state.getFeatureVectorsInCurrentWindow().put(key, fv);
        }
        Logger.getAnonymousLogger().log(Level.INFO, MessageFormat.format("updating state at partition [{0}] of [{1}]", localPartition, numPartitions));
    }

    @Override
    public void prepare(final Map map, final TridentOperationContext tridentOperationContext) {
        localPartition = tridentOperationContext.getPartitionIndex() + 1;
        numPartitions = tridentOperationContext.numPartitions();
    }

    @Override
    public void cleanup() {
    }
}

