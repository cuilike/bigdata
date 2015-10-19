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
package org.calrissian.flowmix.api.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.calrissian.flowmix.api.Aggregator;
import org.calrissian.flowmix.api.Policy;
import org.calrissian.flowmix.core.model.op.AggregateOp;
import org.calrissian.flowmix.core.model.op.FlowOp;
import org.calrissian.flowmix.core.model.op.PartitionOp;

import static java.util.Collections.EMPTY_LIST;

public class AggregateBuilder extends AbstractOpBuilder {

    private Class<? extends Aggregator> aggregatorClass;
    private Policy triggerPolicy;   //触发策略.比如每隔10分钟,或每隔100条数据,统计一次
    private long triggerThreshold = -1;
    private Policy evictionPolicy;  //失效策略.当事件进来10分钟后,或者事件总数到达1000条后,移除
    private long evictionThreshold = -1;
    private boolean clearOnTrigger = false;
    private long windowEvictMillis = 3600000;   // 60 minutes by default

    private Map<String,String> config = new HashMap<String,String>();

    public AggregateBuilder(StreamBuilder flowOpsBuilder) {
        super(flowOpsBuilder);
    }

    public AggregateBuilder aggregator(Class<? extends Aggregator> aggregatorClass) {
        this.aggregatorClass = aggregatorClass;
        return this;
    }

    public AggregateBuilder clearOnTrigger() {
      this.clearOnTrigger = true;
      return this;
    }

    public AggregateBuilder config(String key, String value) {
      config.put(key, value);
      return this;
    }

    public AggregateBuilder trigger(Policy policy, long threshold) {
        this.triggerPolicy = policy;
        this.triggerThreshold = threshold;
        return this;
    }

    public AggregateBuilder evict(Policy policy, long threshold) {
        this.evictionPolicy = policy;
        this.evictionThreshold = threshold;
        return this;
    }

    public AggregateBuilder windowEvictMillis(long millis) {
      this.windowEvictMillis = millis;
      return this;
    }

    public StreamBuilder end() {

      if(aggregatorClass == null)
          throw new RuntimeException("Aggregator operator needs an aggregator class");

      if(triggerPolicy == null || triggerThreshold == -1)
          throw new RuntimeException("Aggregator operator needs to have trigger policy and threshold");

      if(evictionPolicy == null || evictionThreshold == -1)
          throw new RuntimeException("Aggregator operator needs to have eviction policy and threshold");

      List<FlowOp> flowOpList = getStreamBuilder().getFlowOpList();
      FlowOp op = flowOpList.size() == 0 ? null : flowOpList.get(flowOpList.size()-1);
      if(op == null || !(op instanceof PartitionOp))
        flowOpList.add(new PartitionOp(EMPTY_LIST));

      getStreamBuilder().addFlowOp(new AggregateOp(aggregatorClass, triggerPolicy, triggerThreshold, evictionPolicy,
              evictionThreshold, config, clearOnTrigger, windowEvictMillis));
      return getStreamBuilder();
    }
}
