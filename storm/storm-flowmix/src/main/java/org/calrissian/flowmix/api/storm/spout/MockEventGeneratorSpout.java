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
package org.calrissian.flowmix.api.storm.spout;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Values;
import com.google.common.base.Preconditions;
import org.calrissian.flowmix.example.support.ExampleRunner;
import org.calrissian.flowmix.example.support.MockEvent;
import org.calrissian.mango.domain.event.Event;

import static java.util.Collections.singleton;

public class MockEventGeneratorSpout extends EventsLoaderBaseSpout {

    private int sleepBetweenEvents = 1000;
    private Collection<Event> events;

    private SpoutOutputCollector collector;

    private transient Iterator<Event> eventItr;

    public MockEventGeneratorSpout(Collection<Event> events, int sleepBetweenEvents) {
        Preconditions.checkNotNull(events);
        Preconditions.checkArgument(events.size() > 0);

        this.events = events;
        this.sleepBetweenEvents = sleepBetweenEvents;
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.collector = spoutOutputCollector;
        this.eventItr = events.iterator();
    }

    @Override
    public void nextTuple() {
        Event event;

        if (!eventItr.hasNext()){
            //由于是模拟数据,在只有一个事件的时候,迭代器迭代完后,重头开始.
            eventItr = events.iterator();
            //event = eventItr.next();

            //重新生成事件,而不是用最开始的一直都不变的模拟数据.
            //eventItr = MockEvent.getMockEvents().iterator();
        }
        event = eventItr.next();

        //向Collector发射一条事件
        collector.emit(new Values(singleton(event)));

        try {
            Thread.sleep(sleepBetweenEvents);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
