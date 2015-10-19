package com.github.lbhat1.mlstorm.streaming.bolt.ml.state.pca.create;


import backtype.storm.task.IMetricsContext;
import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.pca.PrincipalComponentsBase;
import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.pca.windowed.WindowedStormPca;
import storm.trident.state.State;
import storm.trident.state.StateFactory;
import com.github.lbhat1.mlstorm.streaming.utils.fields.FieldTemplate;

import java.util.Map;

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


public class WindowedPcaFactory implements StateFactory {
    final int sampleSize;
    private final int numPrincipalComponents;
    private final FieldTemplate template;
    PrincipalComponentsBase pc = null;

    public WindowedPcaFactory(int sampleSize, int numPrincipalComponents, FieldTemplate template) {
        this.sampleSize = sampleSize;
        this.numPrincipalComponents = numPrincipalComponents;
        this.template = template;
    }

    @Override
    public synchronized State makeState(final Map conf,
                                        final IMetricsContext metrics,
                                        final int partitionIndex,
                                        final int numPartitions) {
        if (pc == null) {
            try {
                pc = new WindowedStormPca(sampleSize, numPrincipalComponents, partitionIndex, numPartitions, template);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return pc;
    }
}
