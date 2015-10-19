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
package org.calrissian.flowmix.core.storm.bolt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.calrissian.flowmix.api.builder.FlowmixBuilder;
import org.calrissian.flowmix.api.Flow;
import org.calrissian.flowmix.core.model.FlowInfo;
import org.calrissian.flowmix.core.model.op.SelectOp;
import org.calrissian.flowmix.core.support.Utils;
import org.calrissian.mango.domain.event.BaseEvent;
import org.calrissian.mango.domain.event.Event;

import static org.calrissian.flowmix.api.builder.FlowmixBuilder.fields;
import static org.calrissian.flowmix.core.Constants.FLOW_LOADER_STREAM;
import static org.calrissian.flowmix.core.support.Utils.exportsToOtherStreams;
import static org.calrissian.flowmix.core.support.Utils.getFlowOpFromStream;
import static org.calrissian.flowmix.core.support.Utils.hasNextOutput;

public class SelectorBolt extends BaseRichBolt {

    Map<String,Flow> flows;
    OutputCollector collector;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        flows = new HashMap<String, Flow>();
    }

    @Override
    public void execute(Tuple tuple) {
        if(FLOW_LOADER_STREAM.equals(tuple.getSourceStreamId())) {
            for(Flow flow : (Collection<Flow>)tuple.getValue(0))
                flows.put(flow.getId(), flow);
        } else if(!"tick".equals(tuple.getSourceStreamId())) {
            FlowInfo flowInfo = new FlowInfo(tuple);
            Flow flow = flows.get(flowInfo.getFlowId());
            if (flow != null) {
                SelectOp selectOp =  getFlowOpFromStream(flow, flowInfo.getStreamName(), flowInfo.getIdx());
                String nextStream = Utils.getNextStreamFromFlowInfo(flow, flowInfo.getStreamName(), flowInfo.getIdx());

                //映射操作会对事件产生影响,只有包含对应字段的事件,才会进入下一轮
                Event newEvent = new BaseEvent(flowInfo.getEvent().getId(), flowInfo.getEvent().getTimestamp());
                //Event中的tuples类似于一个Map.
                for(org.calrissian.mango.domain.Tuple eventTuple : flowInfo.getEvent().getTuples()) {
                    if(selectOp.getFields().contains(eventTuple.getKey()))
                      newEvent.put(eventTuple);
                }

                /**
                 * If no selected tuples existed, event will not be emitted
                 */
                if(hasNextOutput(flow, flowInfo.getStreamName(), nextStream)) {
                  if (newEvent.getTuples().size() > 0)
                    collector.emit(nextStream, tuple, new Values(flowInfo.getFlowId(), newEvent, flowInfo.getIdx(), flowInfo.getStreamName(), flowInfo.getPreviousStream()));
                }

                // send directly to any non std output streams
                if(exportsToOtherStreams(flow, flowInfo.getStreamName(), nextStream)) {
                  for (String output : flow.getStream(flowInfo.getStreamName()).getOutputs()) {
                    String outputStream = flow.getStream(output).getFlowOps().get(0).getComponentName();
                    collector.emit(outputStream, tuple, new Values(flowInfo.getFlowId(), flowInfo.getEvent(), -1, output, flowInfo.getStreamName()));
                  }
                }

            }
        }
        collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        FlowmixBuilder.declareOutputStreams(outputFieldsDeclarer, fields);
    }
}
