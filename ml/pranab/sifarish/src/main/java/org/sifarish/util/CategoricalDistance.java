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

package org.sifarish.util;

import java.io.Serializable;

/**
 * Explicity set distance between two categorical values
 * @author pranab
 *
 */
public class CategoricalDistance implements Serializable {

	private String thisValue;
	private String thatValue;
	private double distance;
	
	public String getThisValue() {
		return thisValue;
	}
	public void setThisValue(String thisValue) {
		this.thisValue = thisValue;
	}
	public String getThatValue() {
		return thatValue;
	}
	public void setThatValue(String thatValue) {
		this.thatValue = thatValue;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
}
