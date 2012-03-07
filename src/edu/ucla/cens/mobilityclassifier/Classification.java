/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.ucla.cens.mobilityclassifier;

import java.util.List;

/**
 * A classification of sensor data into features and a mobility mode.
 */
public class Classification {
	private String mode;
	private List<Double> fft;
	private String wifiMode;
	private Double average;
	private Double variance;
	private boolean hasFeatures;
	
//	private ArrayList<Double> N95Fft;
//	private Double N95Variance;
	
	/**
	 * Creates a mutable Classifcation instance.
	 */
	public Classification() {
		
	}
	
	public String getMode() {
		return mode;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public List<Double> getFft() {
		return fft;
	}
	
	public void setFft(List<Double> fft) {
		this.fft = fft;
	}
	
	public Double getAverage() {
		return average;
	}
	
	public void setAverage(Double average) {
		this.average = average;
	}
	
	public Double getVariance() {
		return variance;
	}
	
	public void setVariance(Double variance) {
		this.variance = variance;
	}
	
//	public List<Double> getN95Fft() {
//		return N95Fft;
//	}
//	
//	public void setN95Fft(List<Double> n95Fft) {
//		N95Fft = (ArrayList<Double>) n95Fft;
//	}

//	public Double getN95Variance() {
//		return N95Variance;
//	}
//	
//	public void setN95Variance(Double n95Variance) {
//		N95Variance = n95Variance;
//	}

	public boolean hasFeatures() {
		return hasFeatures;
	}
	
	public void setHasFeatures(boolean hasFeatures) {
		this.hasFeatures = hasFeatures;
	}
	
	public String getWifiMode() {
		return wifiMode;
	}
	
	public void setWifiMode(String wifiMode) {
		this.wifiMode = wifiMode;
	}	
}
