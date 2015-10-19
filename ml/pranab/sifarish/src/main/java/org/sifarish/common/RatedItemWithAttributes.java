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

package org.sifarish.common;

/**
 * @author pranab
 *
 */
public class RatedItemWithAttributes  extends RatedItem {
	private String attributes;
	private String[] attributeArray;
	
	public RatedItemWithAttributes(String itemID, int rating) {
		super(itemID, rating);
	}

	public RatedItemWithAttributes(String itemID, int rating, String attributes) {
		this(itemID, rating);
		this.attributes = attributes;
	}

	public RatedItemWithAttributes(String itemID, int rating, String[] attributeArray) {
		this(itemID, rating);
		this.attributeArray = attributeArray;
	}

	public String getAttributes() {
		return attributes;
	}

	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}

	public String[] getAttributeArray() {
		return attributeArray;
	}

	public void setAttributeArray(String[] attributeArray) {
		this.attributeArray = attributeArray;
	}
	
}
