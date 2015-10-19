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

import org.apache.hadoop.conf.Configuration;

/**
 * Base schema class
 * @author pranab
 *
 */
public class TypeSchema {
	private String distAlgorithm;
	private double minkowskiParam;
	private double numericDiffThreshold;
	private String missingValueHandler = "default";
	private String textMatchingAlgorithm;
	private double srcNonMatchingTermWeight = 1.0;
	private double trgNonMatchingTermWeight = 1.0;
	private double[] locationComponentWeights;
	private double[] eventComponentWeights;
	private Configuration conf;
	
	public String getDistAlgorithm() {
		return distAlgorithm;
	}

	public void setDistAlgorithm(String distAlgorithm) {
		this.distAlgorithm = distAlgorithm;
	}
	
	public double getMinkowskiParam() {
		return minkowskiParam;
	}

	public void setMinkowskiParam(double minkowskiParam) {
		this.minkowskiParam = minkowskiParam;
	}

	public double getNumericDiffThreshold() {
		return numericDiffThreshold;
	}
	public void setNumericDiffThreshold(double numericDiffThreshold) {
		this.numericDiffThreshold = numericDiffThreshold;
	}
	public String getMissingValueHandler() {
		return missingValueHandler;
	}
	public void setMissingValueHandler(String missingValueHandler) {
		this.missingValueHandler = missingValueHandler;
	}
	public String getTextMatchingAlgorithm() {
		return textMatchingAlgorithm;
	}

	public void setTextMatchingAlgorithm(String textMatchingAlgorithm) {
		this.textMatchingAlgorithm = textMatchingAlgorithm;
	}

	public double getSrcNonMatchingTermWeight() {
		return srcNonMatchingTermWeight;
	}

	public void setSrcNonMatchingTermWeight(double srcNonMatchingTermWeight) {
		this.srcNonMatchingTermWeight = srcNonMatchingTermWeight;
	}

	public double getTrgNonMatchingTermWeight() {
		return trgNonMatchingTermWeight;
	}

	public void setTrgNonMatchingTermWeight(double trgNonMatchingTermWeight) {
		this.trgNonMatchingTermWeight = trgNonMatchingTermWeight;
	}
	public double[] getLocationComponentWeights() {
		return locationComponentWeights;
	}

	public void setLocationComponentWeights(double[] locationComponentWeights) {
		this.locationComponentWeights = locationComponentWeights;
	}

	public double[] getEventComponentWeights() {
		return eventComponentWeights;
	}

	public void setEventComponentWeights(double[] eventComponentWeights) {
		this.eventComponentWeights = eventComponentWeights;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Entity distance strategy
	 * @param scale
	 * @return
	 */
	public DistanceStrategy createDistanceStrategy(int scale) {
		DistanceStrategy distStrategy = null;
		
		if (distAlgorithm.equals("euclidean")) {
			distStrategy = new EuclideanDistance(scale);
		} else if (distAlgorithm.equals("manhattan")) {
			distStrategy = new ManhattanDistance(scale);
		} else if (distAlgorithm.equals("minkwoski")) {
			distStrategy = new MinkwoskiDistance(scale);
			distStrategy.setPower(minkowskiParam);
		}
		
		return distStrategy;
	}
	
	/**
	 * Text similarity strategy
	 * @return
	 */
	public DynamicAttrSimilarityStrategy createTextSimilarityStrategy() {
		DynamicAttrSimilarityStrategy  textSimStrategy = null;
		if (null  !=  textMatchingAlgorithm) {
			if (textMatchingAlgorithm.equals("jaccard")){
				textSimStrategy = new JaccardSimilarity(srcNonMatchingTermWeight, trgNonMatchingTermWeight);
			} else if (textMatchingAlgorithm.equals("cosine")){
				textSimStrategy = new CosineSimilarity();
			} else if (textMatchingAlgorithm.equals("editDistance")){
				boolean tokenWise = conf.getBoolean("edit.dist.token", true);
				textSimStrategy = new EditDistanceSimilarity(tokenWise);
			}
		}
		return textSimStrategy;
	}	

}
