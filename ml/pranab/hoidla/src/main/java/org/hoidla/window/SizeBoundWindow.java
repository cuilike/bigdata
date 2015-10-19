/*
 * hoidla: various algorithms for Big Data solutions
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

package org.hoidla.window;

import java.io.Serializable;


/**
 * Sliding window bounded my a max size
 * @author pranab
 *
 */
public class SizeBoundWindow<T> extends DataWindow<T> implements Serializable {
	protected int maxSize;
	private int stepSize = 1;
	private int processStepSize = 1;
	
	public SizeBoundWindow() {
	}
	
	/**
	 * @param maxSize
	 */
	public SizeBoundWindow(int maxSize) {
		super(true);
		this.maxSize = maxSize;
	}
	
	/**
	 * @param maxSize
	 * @param stepSize
	 */
	public SizeBoundWindow(int maxSize, int stepSize) {
		this(maxSize);
		this.stepSize = stepSize;
	}
	
	/**
	 * @param maxSize
	 * @param stepSize
	 */
	public SizeBoundWindow(int maxSize, int stepSize, int processStepSize) {
		this(maxSize, stepSize);
		this.processStepSize = processStepSize;
	}

	/* (non-Javadoc)
	 * @see org.hoidla.window.DataWindow#expire()
	 */
	public void expire() {
		//process window data
		if (count % processStepSize == 0) {
			processFullWindow();
		}
		
		//slide window
		if (dataWindow.size() > maxSize) {
			//manage window
			if (stepSize == maxSize) {
				//tumble
				dataWindow.clear();
			} else {
				//slide by stepSize
				for (int i = 0; i < stepSize; ++i) {
					dataWindow.remove(0);
				}
			}
			expired = true;
		} else {
			expired = false;
		}
	}
	
	public boolean isFull() {
		return dataWindow.size() == maxSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int getStepSize() {
		return stepSize;
	}

	public void setStepSize(int stepSize) {
		this.stepSize = stepSize;
	}

	public int getProcessStepSize() {
		return processStepSize;
	}

	public void setProcessStepSize(int processStepSize) {
		this.processStepSize = processStepSize;
	}
}
