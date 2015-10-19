/*
 * Sifarish: Recommendation Engine
 * Author: Pranab Ghosh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.sifarish.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.sifarish.common.TaggedEntity;

/**
 * @author pranab
 *
 */
public class SemanticSimilarity extends DynamicAttrSimilarityStrategy {
	private TaggedEntity  thisEntity;
	private TaggedEntity  thatEntity;
	private int topMatchCount;
	private int scale;
	private List<MatchedItem> matchedItems = new ArrayList<MatchedItem>();
    private static final Logger LOG = Logger.getLogger(SemanticSimilarity.class);
	
	public SemanticSimilarity(String matcherClass, int topMatchCount, Map<String,Object> params) throws IOException   {
        Class<?> iterCls;
		try {
			iterCls = Class.forName(matcherClass);
			thisEntity = (TaggedEntity)iterCls.newInstance();
			thatEntity = (TaggedEntity)iterCls.newInstance();
			thisEntity.initialize(params);
			this.topMatchCount = topMatchCount;
			scale = (Integer)params.get("semanticScale");
					
			Configuration conf = (Configuration)params.get("config");
	        if (conf.getBoolean("debug.on", false)) {
	         	LOG.setLevel(Level.DEBUG);
	        }
		} catch (ClassNotFoundException e) {
			throw new IOException("failed to intialize SemanticSimilarity");
		}catch (InstantiationException e) {
			throw new IOException("failed to intialize SemanticSimilarity");
		} catch (IllegalAccessException e) {
			throw new IOException("failed to intialize SemanticSimilarity");
		}
	}
	
	/**
	 * @param src
	 * @param target
	 * @return
	 * @throws IOException 
	 */
	public  double findDistance(String src, String target) throws IOException {
		int matchScore;
		String matchingContext;
		double avScore = 0;
		matchedItems.clear();
		
		String[] thisTagItems = src.split(fieldDelimRegex);
		String[] thatTagItems = target.split(fieldDelimRegex);
		for (String thisTagItem : thisTagItems) {
			thisEntity.setTag(thisTagItem);
			for (String thatTagItem :thatTagItems) {
				LOG.debug("thisTagItem:" + thisTagItem + " thatTagItem:" + thatTagItem);
				thatEntity.setTag(thatTagItem);
				matchScore = thisEntity.match(thatEntity);
				matchScore = matchScore <= scale ? matchScore : scale;
				if (!thatEntity.isResultCorrelation()) {
					matchScore = scale - matchScore;
					LOG.debug("matchScore:" + matchScore);
				}
				matchingContext = thisEntity.getMatchingContext();
				LOG.debug("matchScore:" + matchScore + " matchingContext:" + matchingContext);
				matchedItems.add(new MatchedItem(matchScore, matchingContext));
			}
		}
		LOG.debug("matched items size:" + matchedItems.size());
		
		//sort them descending
		Collections.sort(matchedItems);
		int numMatches = matchedItems.size() < topMatchCount ? matchedItems.size() : topMatchCount;
		matchingContexts = new String[numMatches];
		for (int i = 0; i < numMatches; ++i) {
			matchingContexts[i] = matchedItems.get(i).getContext();
			avScore += matchedItems.get(i).getScore();
			LOG.debug("after sorting score:" + matchedItems.get(i).getScore());
		}
		avScore /= numMatches;
		avScore /=  scale;
		avScore = avScore > 1.0 ? 1.0 : avScore;
		LOG.debug("avScore:" + avScore);
		return avScore;
	}
	
	@Override
	public double findDistance(String thisEntityID, String thisTag,
			String thatEntityID, String thatTag, String groupingID) throws IOException {
		thisEntity.setEntityID(thisEntityID);
		thisEntity.setGroupID(groupingID);
		thatEntity.setEntityID(thisEntityID);
		thatEntity.setGroupID(groupingID);
		
		return findDistance( thisTag, thatTag);
	}
	
	private static class MatchedItem  implements Comparable<MatchedItem>{
		private int score;
		private String context;
		
		public MatchedItem(int score, String context) {
			super();
			this.score = score;
			this.context = context;
		}

		public int getScore() {
			return score;
		}

		public String getContext() {
			return context;
		}

		@Override
		public int compareTo(MatchedItem other) {
			return  other.score - score ;
		}
		
	}

}
