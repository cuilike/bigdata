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

import java.util.List;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.StormTopology;
import com.google.common.collect.Iterables;
import org.calrissian.flowmix.api.Flow;
import org.calrissian.flowmix.api.FlowTestCase;
import org.calrissian.flowmix.api.Policy;
import org.calrissian.flowmix.api.builder.FlowBuilder;
import org.calrissian.flowmix.api.kryo.EventSerializer;
import org.calrissian.flowmix.api.Function;
import org.calrissian.flowmix.api.storm.bolt.MockSinkBolt;
import org.calrissian.mango.domain.Tuple;
import org.calrissian.mango.domain.event.BaseEvent;
import org.calrissian.mango.domain.event.Event;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JoinBoltIT extends FlowTestCase {


  @Test
  public void test_timeEvict() {
    Flow flow = new FlowBuilder()
      .id("flow")
      .flowDefs()
        .stream("stream1")
          .each().function(new Function() {
            @Override
            public List<Event> execute(Event event) {
              Event newEvent = new BaseEvent(event.getId(), event.getTimestamp());
              newEvent.putAll(Iterables.concat(event.getTuples()));
              newEvent.put(new Tuple("stream", "stream1"));
              return singletonList(newEvent);
            }
        }).end()
        .endStream(false, "stream3")   // send ALL results to stream2 and not to standard output
        .stream("stream2")
          .each().function(new Function() {
            @Override
            public List<Event> execute(Event event) {
              Event newEvent = new BaseEvent(event.getId(), event.getTimestamp());
              newEvent.putAll(Iterables.concat(event.getTuples()));
              newEvent.put(new Tuple("stream", "stream2"));
              return singletonList(newEvent);
            }
          }).end()
        .endStream(false, "stream3")
        .stream("stream3", false)  // don't read any events from standard input
          .join("stream1", "stream2").evict(Policy.TIME, 5).end()
        .endStream()
      .endDefs()
    .createFlow();

    StormTopology topology = buildTopology(flow, 500);
    Config conf = new Config();
    conf.setNumWorkers(20);
    conf.registerSerialization(BaseEvent.class, EventSerializer.class);
    conf.setSkipMissingKryoRegistrations(false);

    LocalCluster cluster = new LocalCluster();
    cluster.submitTopology("test", conf, topology);

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    cluster.shutdown();
    System.out.println(MockSinkBolt.getEvents());
    assertTrue(MockSinkBolt.getEvents().size() > 0);

    System.out.println(MockSinkBolt.getEvents().size());

    for(Event event : MockSinkBolt.getEvents()) {
      //stream1中事件的stream字段=stream1, stream2中事件的stream字段为stream2
      //所以join时, 对相同stream字段会组成: stream, [Tuple("stream","stream1"), Tuple("steram","stream2")]. 即stream这个field有两个Tuple.
      assertEquals(2, event.getAll("stream").size());
      assertEquals(1, event.getAll("key1").size());
      assertEquals(1, event.getAll("key2").size());
      assertEquals(1, event.getAll("key3").size());
      assertEquals(1, event.getAll("key4").size());
      assertEquals(1, event.getAll("key5").size());
    }
  }

  @Test
  public void test_countEvict() {
    Flow flow = new FlowBuilder()
      .id("flow")
      .flowDefs()
        .stream("stream1")
          .each().function(new Function() {
            @Override
            public List<Event> execute(Event event) {
              Event newEvent = new BaseEvent(event.getId(), event.getTimestamp());
              newEvent.putAll(Iterables.concat(event.getTuples()));
              newEvent.put(new Tuple("stream", "stream1"));
              return singletonList(newEvent);
            }
          }).end()
        .endStream(false, "stream3")   // send ALL results to stream3 and not to standard output
        .stream("stream2")
          .each().function(new Function() {
            @Override
            public List<Event> execute(Event event) {
              Event newEvent = new BaseEvent(event.getId(), event.getTimestamp());
              newEvent.putAll(Iterables.concat(event.getTuples()));
              newEvent.put(new Tuple("stream", "stream2"));
              return singletonList(newEvent);
            }
          }).end()
        .endStream(false, "stream3")
        .stream("stream3", false)  // don't read any events from standard input
          .join("stream1", "stream2").evict(Policy.COUNT, 1).end()
        .endStream()
      .endDefs()
    .createFlow();

    StormTopology topology = buildTopology(flow, 1000);
    Config conf = new Config();
    conf.setNumWorkers(20);
    conf.registerSerialization(BaseEvent.class, EventSerializer.class);
    conf.setSkipMissingKryoRegistrations(false);

    LocalCluster cluster = new LocalCluster();
    cluster.submitTopology("test", conf, topology);

    try {
      Thread.sleep(6000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    cluster.shutdown();
    System.out.println(MockSinkBolt.getEvents());
    assertTrue(MockSinkBolt.getEvents().size() > 0);

    System.out.println(MockSinkBolt.getEvents().size());

    //assertTrue(MockSinkBolt.getEvents().size() >= 4);
    //assertTrue(MockSinkBolt.getEvents().size() <= 5);
    for(Event event : MockSinkBolt.getEvents()) {
      assertEquals(2, event.getAll("stream").size());
      assertEquals(1, event.getAll("key1").size());
      assertEquals(1, event.getAll("key2").size());
      assertEquals(1, event.getAll("key3").size());
      assertEquals(1, event.getAll("key4").size());
      assertEquals(1, event.getAll("key5").size());
    }
  }
}
