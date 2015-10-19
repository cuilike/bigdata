/*
 * visitante: Web analytic using Hadoop Map Reduce and Storm
 * Author: Pranab Ghosh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, softwarSessionSummarizere
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.visitante.realtime;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.chombo.storm.GenericBolt;
import org.chombo.storm.MessageHolder;
import org.chombo.storm.MessageQueue;
import org.chombo.storm.PubSub;
import org.chombo.util.ConfigUtility;
import org.hoidla.stream.HyperLogLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.Config;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author pranab
 *
 */
public class UniqueVisitorCounterBolt extends  GenericBolt {
	private int tickFrequencyInSeconds;
	private HyperLogLog uniqueCounter;
	private PubSub pubSub;
	private long minTotalCount;
	private MessageHolder msg;
	private static final String COMM_RESET = "reset";
	
	private static final Logger LOG = LoggerFactory.getLogger(UniqueVisitorCounterBolt.class);
	
	public UniqueVisitorCounterBolt(int tickFrequencyInSeconds) {
		super();
		this.tickFrequencyInSeconds = tickFrequencyInSeconds;
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		  Config conf = new Config();
		  conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, tickFrequencyInSeconds);
		  return conf;
	}

	@Override
	public void intialize(Map stormConf, TopologyContext context) {
		int bucketBitCount = ConfigUtility.getInt(stormConf, "bucket.bit.count");
		uniqueCounter = new HyperLogLog(bucketBitCount);
		minTotalCount = ConfigUtility.getLong(stormConf, "min.total.count");
		
		//pub sub for command
		String commStore = ConfigUtility.getString(stormConf, "command.store", "none");
		if (!commStore.equals("none")) {
			int numUniqueCounterBolt = ConfigUtility.getInt(stormConf, "unique.count.bolt.threads", 1);		
			pubSub = PubSub.createPubSub(stormConf, commStore, numUniqueCounterBolt);
		}

	}

	@Override
	public boolean process(Tuple input) {
		boolean status = true;
		outputMessages.clear();
		
		if (isTickTuple(input)) {
			LOG.info("got tick tuple ");
			long totalCount = uniqueCounter.getCount();
			if (totalCount > minTotalCount)	{
				//only after some minimum number of items have been processed
				long count = uniqueCounter.getUnqueCount();
				msg = new MessageHolder();
				msg.setMessage(new Values(getID(), count));
				outputMessages.add(msg);
				LOG.info("emitted count:" + count);
			}
			
			//check if there is request to reset
			if (null != pubSub) {
				String command = pubSub.subscribe(getID());
				if (null != command) {
					if (command.equals(COMM_RESET)) {
						uniqueCounter.clear();
					}
				}
			}
		} else {
			String userID = input.getStringByField(UniqueVisitorTopology.USER_ID);
			uniqueCounter.add(userID);
			LOG.info("added to counter");
		}
		return status;
	}

	@Override
	public List<MessageHolder> getOutput() {
		return outputMessages;
	}

}
