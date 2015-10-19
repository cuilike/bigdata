/*
 * Copyright (C) 2014 The Calrissian Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.calrissian.flowmix.api.aggregator;

/**
 * Simple sum calculator, this sums an aggregated tuple window adds and
 * subtracts accordingly (event added or evicted)
 *
 */
public class SumAggregator extends AbstractAggregator<Long, Long> {

    public static final String DEFAULT_OUTPUT_FIELD = "sum";

    protected long sum;

    @Override
    public void evict(Long value) {
        sum -= value;
    }

    @Override
    protected String getOutputField() {
        return DEFAULT_OUTPUT_FIELD;
    }

    @Override
    public void add(Long... value) {
        sum += value[0];
    }

    @Override
    protected Long aggregateResult() {
        return sum;
    }

}
