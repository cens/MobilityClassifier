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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Brent Longstaff
 * @author Joshua Selsky
 */
public class MobilityClassifier {
	private static final String STILL = "still";
	private static final String WALK = "walk";
	private static final String RUN = "run";
	// private static final String BIKE = "bike"; // not supported now
	private static final String DRIVE = "drive";
	private static final String UNKNOWN = "unknown";
	private static final String VERSION = "1.2.9";
	public static boolean wifiChecking = true;
	
	public static String getVersion() {
		return VERSION;
	}
	
	/**
	 * Takes the raw sensor values and returns a classification object with the
	 * transport mode and, when applicable, features.
	 */
	public Classification classify(List<Sample> accelValues, Double speed, WifiScan wifiScan, WifiScan lastWifiScan, String lastMode) {
		// Convert from triaxial to single magnitude ArrayList in gravity units
		ArrayList<Double> magnitudes = new ArrayList<Double>();
		for (Sample sample : accelValues) {
			magnitudes.add(getMagnitude(sample));
		}
		if (lastMode == null || (! lastMode.equals("STILL") && ! lastMode.equals("DRIVE"))) {
			lastMode = UNKNOWN; // Not allowing any aberrant values for this
		}
		return getTransportMode(magnitudes, speed, wifiScan, lastWifiScan, lastMode);
	}

	/**
	 * Calculates features (both in Android and N95 units) and calls the classifier.
	 * @param magnitudes
	 * @param speed
	 * @return Classification with mode, and, if they were calculated, features
	 */
	private Classification getTransportMode(ArrayList<Double> magnitudes, Double speed, WifiScan wifiScan, WifiScan lastWifiScan, String lastMode)
	{
		double dataSize = magnitudes.size();
		Classification classification = new Classification();
		
		// If there are not enough samples for feature calculation, the phone must be still
		if (dataSize <= 10) {
			classification.setHasFeatures(false);
			classification.setMode(STILL);
			return classification;
		}
		
		String wifiActivity = UNKNOWN;
		
		if (wifiScan != null) {
			if (lastWifiScan == null) {
				wifiActivity = checkWifi(wifiScan, null, lastMode);
			}
			else {
				wifiActivity = checkWifi(wifiScan, lastWifiScan, lastMode);
			}
		}
		
		classification.setWifiMode(wifiActivity);
		
		double sum = 0.0, s = 0.0;
		double avg = 0.0, a = 0.0;
		double var = 0.0, v = 0.0;

		ArrayList<Double> fft = new ArrayList<Double>(10);

		for(int i  = 1; i <= 10; i++) {
			fft.add(goertzel(magnitudes, (double) i, dataSize));
		}

		for (int i = 0; i < dataSize; i++)
		{
			s += magnitudes.get(i);
		}
		a = s / dataSize;
		s = 0.0;
		for (int i = 0; i < dataSize; i++) {
			s += Math.pow((magnitudes.get(i) - a), 2.0);
		}

		v = s / dataSize;

		for (int i = 0; i < dataSize; i++) {
			magnitudes.set(i, magnitudes.get(i) * 310.); // convert to N95 units
		}

		for (int i = 0; i < dataSize; i++) {
			sum += magnitudes.get(i);
		}

		String activity = activity(speed,a,v, fft.get(0), fft.get(1), fft.get(2), fft.get(3), fft.get(4), fft.get(5), fft.get(6), fft.get(7), fft.get(8), fft.get(9));
		
		if (wifiChecking && ! wifiActivity.equals(UNKNOWN)) {
			if (activity.equals(DRIVE) || activity.equals(STILL)) {
				activity = wifiActivity; // The other classifier is rubbish for still/drive; just use WiFi result if there is one
			}
		}
		
		classification.setMode(activity);
		classification.setAverage(a);
		classification.setVariance(v);
		classification.setFft(fft);
		classification.setHasFeatures(true);
		return classification;
	}

	/**
	 * Compares current WiFi point to the previous one, and returns UNKNOWN, STILL, or DRIVE
	 * @param sample
	 * @return Magnitude value
	 */
	private String checkWifi(WifiScan wifiScan, WifiScan lastWifiScan, String lastMode) {
		long time = wifiScan.getTime().longValue();
		
		if (lastWifiScan != null) {
			
			long lastTime = lastWifiScan.getTime().longValue();

			if (lastTime == time) { // no new wifi data 
				return lastMode;
			}

			if (lastTime < time - 1000 * 60 * 8) { // if no recent wifi for comparison
				return UNKNOWN;
			}
			
			List<String> lastSSIDList = getSSIDList(lastWifiScan.getAccessPoints());
			List<String> currentSSIDList = getSSIDList(wifiScan.getAccessPoints());
			
			// Compare to the access points from last time
			double same = 0;
			double total = 0;
			
			// Now we can do the comparison
			for (String ssid : currentSSIDList) {
				if (lastSSIDList.contains(ssid)) {
					same++;
				}
				total++;
			}
			
			for (String ssid : lastSSIDList) {
				if (! currentSSIDList.contains(ssid)) { // only count others that don't match. We don't count the same ones again. Change that if too many false DRIVE classifications
					total++;
				}
			}

			if (total > 0 && ((same / total) < (1.0 / 3.0))) {
				return DRIVE;
			}
			else if (total > 0) {
				return STILL;
			}
			else {
				return UNKNOWN;
			}
		}
		else {
			return UNKNOWN;
		}
	}
	
	private List<String> getSSIDList(List<AccessPoint> accessPoints) {
		List<String> ssidList = new ArrayList<String>();
		int strsum = 0, strcount = 0;
		int numberOfAccessPoints = accessPoints.size(); 
		
		for (int i = 0; i < numberOfAccessPoints; i++) {
			String ssid = accessPoints.get(i).getSsid(); 
			Double strength = accessPoints.get(i).getStrength();
			
			strsum += strength; 
			strcount++;
			
			if (accessPoints.get(i).getStrength() < -50) {
				ssidList.add(ssid);
			}
		}
		
		if (ssidList.size() == 0 && strcount > 0) {
			double avg = strsum / strcount;
			
			for (int i = 0; i < numberOfAccessPoints; i++) {
				
				String ssid = accessPoints.get(i).getSsid(); 
				Double strength = accessPoints.get(i).getStrength();
				
				strsum += strength;
				strcount++;
				
				if (strength < avg) {
					ssidList.add(ssid);
				}
			}
		}
		
		return ssidList;
	}
	
	/**
	 * Converts to gravity units and calculates the overall magnitude of the triaxial vectors.
	 * @param sample
	 * @return Magnitude value
	 */
	private Double getMagnitude(Sample sample) {
		double x = sample.getX();
	    double y = sample.getY();
	    double z = sample.getZ();
	    double totalForce = 0.0;
	    double grav = 9.80665; // This is the gravity value used in the Android API
	    
	    totalForce += Math.pow(x/grav, 2.0);
	    totalForce += Math.pow(y/grav, 2.0);
	    totalForce += Math.pow(z/grav, 2.0);
	    totalForce = Math.sqrt(totalForce);

	    return totalForce;
	}   
	
	/**
	 * This is the main classification method. Updated code after retraining
	 * @param acc_var
	 * @param accgz1
	 * @param accgz2
	 * @param accgz3
	 * @param gps_speed
	 * @param avg
	 * @param var
	 * @param a1
	 * @param a2
	 * @param a3
	 * @param a4
	 * @param a5
	 * @param a6
	 * @param a7
	 * @param a8
	 * @param a9
	 * @param a0
	 * @return Classification object with the mode
	 */	
	private String activity(Double gps_speed, double avg, double var, double a1, double a2, double a3, double a4, double a5,
			double a6, double a7, double a8, double a9, double a0)
	{
		String output = STILL;

		if(var <= 0.016791)
		{
			if(a6 <= 0.002427)
			{
				/*if(a7 <= 0.001608)
				{*/
					if( gps_speed <= 0.791462 || gps_speed.isNaN())//|| gps_speed != Double.NaN)
					{
						
//						if(avg <= 0.963016)
//						{
//							output = STILL;
//						}
//						else  if(avg <= 0.98282)
//						{
//							output = DRIVE;Log.d(TAG, "Drive 0 because gps speed is " + gps_speed + " and avg is " + avg);
//						}
//						else if(avg <= 1.042821)
//						{
//							if(avg <= 1.040987)
//							{
//								if(avg <= 1.037199)
//								{
//									if(avg <= 1.03592)
//									{
//										output = STILL;
//									}
//									else 
//									{
//										output = DRIVE;
//									}
//								}
//								else
//								{
//									output = STILL;
//								}
//							}
//							else
//							{
//								output = DRIVE;
//							}
//						}
//						else
						{
						 	output = STILL;
						}
					}
					else
					{
						output = DRIVE;
					}
				/*}
				else
				{
					output = DRIVE;
				}*/
			}
			else if(gps_speed <= 0.791462 || gps_speed.isNaN())//&& gps_speed != Double.NaN)
			{
				output = STILL;
			}
			else
			{
				output = DRIVE;
			}
		}
		else
		{
			if(a3 <= 16.840921)
			{
				output = WALK;
			}
			else
			{
				output = RUN;	
			}
		}

		return output;

	}

	/**
	 * Calculates FFTs
	 * @param accData
	 * @param freq
	 * @param sr
	 * @return FFT value
	 */
	private double goertzel(ArrayList<Double> accData, double freq, double sr)
	{
		double s_prev = 0;
		double s_prev2 = 0;
		double coeff = 2 * Math.cos((2 * Math.PI * freq) / sr);
		double s;
		for (int i = 0; i < accData.size(); i++)
		{
			double sample = accData.get(i);
			s = sample + coeff * s_prev - s_prev2;
			s_prev2 = s_prev;
			s_prev = s;
		}
		double power = s_prev2 * s_prev2 + s_prev * s_prev - coeff * s_prev2 * s_prev;

		return power;
	}
	
//	public static void main(String [] args)
//	{
//		// List<Sample> accelValues, Double speed, String wifi, String lastWifi, String lastMode
//		ArrayList<Sample> accelValues = new ArrayList<Sample>();
//		Sample sample = new Sample();
//		sample.setX(0.);
//		sample.setY(0.);
//		sample.setZ(1.);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		accelValues.add(sample);
//		String last = "{\"timezone\":\"org.apache.harmony.luni.internal.util.ZoneInfo[\\\"PST\\\",mRawOffset=-28800000,mUseDst=true]\",\"time\":1325716855277,\"scan\":[{\"ssid\":\"00:27:0d:ed:35:61\",\"strength\":-91},{\"ssid\":\"00:1a:1e:81:96:41\",\"strength\":-88},{\"ssid\":\"00:23:69:0d:7a:d8\",\"strength\":-88},{\"ssid\":\"00:11:24:a9:82:a4\",\"strength\":-87},{\"ssid\":\"00:1a:1e:81:96:43\",\"strength\":-87},{\"ssid\":\"00:1a:1e:81:96:45\",\"strength\":-87},{\"ssid\":\"00:1a:1e:1f:3a:24\",\"strength\":-84},{\"ssid\":\"00:1a:1e:1f:3a:25\",\"strength\":-83},{\"ssid\":\"00:1a:1e:1f:3a:22\",\"strength\":-83},{\"ssid\":\"00:1a:1e:89:4b:82\",\"strength\":-82},{\"ssid\":\"00:1a:1e:1f:3a:23\",\"strength\":-82},{\"ssid\":\"00:1a:1e:89:4b:83\",\"strength\":-82},{\"ssid\":\"00:1a:1e:89:4b:81\",\"strength\":-81},{\"ssid\":\"00:17:5a:b7:ef:90\",\"strength\":-60},{\"ssid\":\"00:1a:1e:1f:3c:c4\",\"strength\":-56},{\"ssid\":\"00:1a:1e:1f:3c:c5\",\"strength\":-53},{\"ssid\":\"00:1a:1e:1f:3c:c2\",\"strength\":-53},{\"ssid\":\"00:1a:1e:1f:3c:c1\",\"strength\":-51}]}";
//		String current = "{\"timezone\":\"org.apache.harmony.luni.internal.util.ZoneInfo[\\\"PST\\\",mRawOffset=-28800000,mUseDst=true]\",\"time\":1325716976116,\"scan\":[{\"ssid\":\"00:1a:1e:81:96:41\",\"strength\":-88},{\"ssid\":\"00:27:0d:ed:35:62\",\"strength\":-88},{\"ssid\":\"00:27:0d:ed:35:60\",\"strength\":-88},{\"ssid\":\"00:1a:1e:89:4b:83\",\"strength\":-82},{\"ssid\":\"00:1a:1e:89:4b:82\",\"strength\":-81},{\"ssid\":\"00:1a:1e:89:4b:85\",\"strength\":-81},{\"ssid\":\"00:1a:1e:1f:3a:24\",\"strength\":-80},{\"ssid\":\"00:1a:1e:1f:3a:22\",\"strength\":-80},{\"ssid\":\"00:1a:1e:89:4b:81\",\"strength\":-76},{\"ssid\":\"00:17:5a:b7:ef:90\",\"strength\":-61},{\"ssid\":\"00:1a:1e:1f:3c:c5\",\"strength\":-54},{\"ssid\":\"00:1a:1e:1f:3c:c4\",\"strength\":-53},{\"ssid\":\"00:1a:1e:1f:3c:c1\",\"strength\":-53},{\"ssid\":\"00:1a:1e:1f:3c:c2\",\"strength\":-53}]}";
//		System.out.println(new MobilityClassifier().classify(accelValues, 1.0, current, last, UNKNOWN).getMode());
//	}
}
