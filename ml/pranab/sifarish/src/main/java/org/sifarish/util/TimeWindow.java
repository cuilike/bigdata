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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Time window attribute
 * @author pranab
 *
 */
public class TimeWindow  extends StructuredAttribute{
	private long start;
	private long end;
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public TimeWindow(String start, String end) throws ParseException {
		this.start = df.parse(start).getTime();
		this.end = df.parse(end).getTime();
		if (this.start > this.end) {
			throw new IllegalArgumentException("start time should be less than end time start:" + start + 
					" end:" + end);
		}
	}
	
	public long getLength() {
		return end - start;
	}
	
	/* (non-Javadoc)
	 * @see org.sifarish.util.StructuredAttribute#distance(org.sifarish.util.StructuredAttribute, org.sifarish.util.Field)
	 * Based on the extent of overlap
	 */
	public double distance(StructuredAttribute otherAttr, Field field) {
		TimeWindow other = (TimeWindow)otherAttr;
		double distance = 1.0;
		long overlap = 0;
		if (start < other.start) {
			if (end < other.start) {
				distance = 1;
			} else if (end < other.end) {
				overlap =  end - other.start;
			} else {
				overlap =  other.end - other.start;
			}
		}  else if (start < other.end) {
			if (end <= other.end) {
				overlap = end - start;
			} else {
				overlap = other.end - start;
			}
		} else {
			distance = 1;
		}
		
		if (overlap > 0) {
			distance = ((double)overlap) / field.getMaxTimeWindowInMili();
			distance = distance > 1.0 ? 1.0 : distance;
			distance = 1.0 - distance;
		}
		return distance;
	}
	

}
