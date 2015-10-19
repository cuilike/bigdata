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

package org.sifarish.etl;

import java.io.Serializable;
import java.util.List;

/**
 * Normalizers for all structured text field types
 * @author pranab
 *
 */
public class StructuredTextNormalizer implements Serializable {
	private List<TextFieldTokenNormalizer>  textFieldTypes;

	/**
	 * @return
	 */
	public List<TextFieldTokenNormalizer> getTextFieldTypes() {
		return textFieldTypes;
	}

	/**
	 * @param textFieldTypes
	 */
	public void setTextFieldTypes(List<TextFieldTokenNormalizer> textFieldTypes) {
		this.textFieldTypes = textFieldTypes;
	}
	
	/**
	 * @param fieldType
	 * @return
	 */
	public TextFieldTokenNormalizer findTokenNormalizer(String fieldType) {
		TextFieldTokenNormalizer normalizer = null;
		for (TextFieldTokenNormalizer thisNormalizer : textFieldTypes) {
			if (thisNormalizer.getFieldType().equals(fieldType)) {
				normalizer = thisNormalizer;
				break;
			}
		}
		return normalizer;
	}
}
