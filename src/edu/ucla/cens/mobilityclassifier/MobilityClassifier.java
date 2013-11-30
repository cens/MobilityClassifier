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
	public static final long WIFI_THRESHOLD_MILLIS = 1000 * 60 * 10;
	public static final long LOC_THRESHOLD_MILLIS = 1000 * 60 * 6;
	private static final String STILL = "still";
	private static final String WALK = "walk";
	private static final String RUN = "run";
	// private static final String BIKE = "bike"; // not supported now
	private static final String DRIVE = "drive";
	private static final String UNKNOWN = "unknown";
	private static final String VERSION = "1.4.7"; // newly retrained classifier
	public static boolean wifiChecking = true;
	public static boolean locationChecking = true;
	
	public static String getVersion() {
		return VERSION;
	}
	
	/**
	 * Takes the raw sensor values and returns a classification object with the
	 * transport mode and, when applicable, features.
	 */
	public Classification classify(List<Sample> accelValues, Double speed, WifiScan wifiScan, List<WifiScan> lastWifiScans, Location currLoc, ArrayList<Location> histLocs, Classification lastClassification) {
		// Convert from triaxial to single magnitude ArrayList in gravity units
		ArrayList<Double> magnitudes = new ArrayList<Double>();
		if (speed < 0)
			speed = Double.NaN;
		for (Sample sample : accelValues) {
			magnitudes.add(getMagnitude(sample));
		}
		if (lastClassification == null)
		{
			lastClassification = new Classification();
			lastClassification.setWifiMode(UNKNOWN);
			lastClassification.setLocationMode(UNKNOWN);
		}
		
		if (lastClassification.getWifiMode() == null)
			lastClassification.setWifiMode(UNKNOWN);
		if (lastClassification.getLocationMode() == null)
			lastClassification.setLocationMode(UNKNOWN);
		
		if (!lastClassification.getWifiMode().equals(STILL) && !lastClassification.getWifiMode().equals(DRIVE)) {
			lastClassification.setWifiMode(UNKNOWN); // Not allowing any aberrant values for this
		}
		if (! lastClassification.getLocationMode().equals(STILL) && ! lastClassification.getLocationMode().equals(DRIVE)) {
			lastClassification.setLocationMode(UNKNOWN); // Not allowing any aberrant values for this
		}
		return getTransportMode(magnitudes, speed, wifiScan, lastWifiScans, currLoc, histLocs, lastClassification);
	}

	/**
	 * Calculates features (both in Android and N95 units) and calls the classifier.
	 * @param magnitudes
	 * @param speed
	 * @return Classification with mode, and, if they were calculated, features
	 */
	private Classification getTransportMode(ArrayList<Double> magnitudes, Double speed, WifiScan wifiScan, List<WifiScan> lastWifiScans, Location currLoc, ArrayList<Location> histLocs, Classification lastClassification)
	{
		double dataSize = magnitudes.size();
		Classification classification = new Classification();
		
		// If there are not enough samples for feature calculation, the phone must be still
		if (dataSize <= 10) {
			classification.setHasFeatures(false);
			classification.setMode(STILL);
			return classification;
		}
		
		Classification wifiClassification, locationClassification;
		
		if (wifiScan != null) {
			if (lastWifiScans == null || lastWifiScans.size() == 0) {
				wifiClassification = checkWifi(wifiScan, null, lastClassification);
			}
			else {
				wifiClassification = checkWifi(wifiScan, lastWifiScans, lastClassification);
			}
		}
		else
		{
			wifiClassification = new Classification();
			wifiClassification.setMode(UNKNOWN);
		}
		
		if (currLoc != null)
		{
			if (histLocs == null || histLocs.size() == 0) {
				locationClassification = checkLocation(currLoc, null, lastClassification);
			}
			else {
				locationClassification = checkLocation(currLoc, histLocs, lastClassification);
			}
		}
		else
		{
			locationClassification = new Classification();
			locationClassification.setLocationMode(UNKNOWN);
		}
		classification.updateWifi(wifiClassification);
		classification.updateLocation(locationClassification);
		
		
		
		double sum = 0.0;
		double average = 0.0;
		double variance = 0.0;

		ArrayList<Double> fft = new ArrayList<Double>(10);

		for(int i  = 1; i <= 10; i++) {
			fft.add(goertzel(magnitudes, (double) i, dataSize));
		}

		for (int i = 0; i < dataSize; i++) {
			sum += magnitudes.get(i);
		}
		average = sum / dataSize;
		sum = 0.0;
		for (int i = 0; i < dataSize; i++) {
			sum += Math.pow((magnitudes.get(i) - average), 2.0);
		}

		variance = sum / dataSize;

//		for (int i = 0; i < dataSize; i++) {
//			magnitudes.set(i, magnitudes.get(i) * 310.); // convert to N95 units
//		}

//		for (int i = 0; i < dataSize; i++) {
//			magnitudes.get(i);
//		}

		String activity = activity(speed,average,variance, fft.get(0), fft.get(1), fft.get(2), fft.get(3), fft.get(4), fft.get(5), fft.get(6), fft.get(7), fft.get(8), fft.get(9), classification);
		
//		if (wifiChecking && ! classification.getWifiMode().equals(UNKNOWN)) {
//			if (activity.equals(DRIVE) || activity.equals(STILL)) {
//				activity = classification.getWifiMode(); // The other classifier is rubbish for still/drive; just use WiFi result if there is one
//			}
//		}
//		if (locationChecking && !classification.getLocationMode().equals(UNKNOWN)) {
//			if (activity.equals(DRIVE) || activity.equals(STILL)) {
//				activity = classification.getLocationMode(); // Sometimes Wi-Fi is not enough. TODO update this after using weka
//			}
//		}
		
		classification.setMode(activity);
		classification.setAverage(average);
		classification.setVariance(variance);
		classification.setFft(fft);
		classification.setHasFeatures(true);
		return classification;
	}

	private Classification checkLocation(Location currLoc,
			ArrayList<Location> histLocs, Classification lastClassification) {
		long time = currLoc.getTime();
		Classification lc = new Classification();
		if (histLocs != null && histLocs.size() > 0) {
			long lastTime = histLocs.get(histLocs.size() - 1).getTime();
			if (lastTime == time) {
				lc.updateLocation(lastClassification);
				return lc;
			}
			
			if (lastTime < time - LOC_THRESHOLD_MILLIS) { // if no recent wifi for comparison
				// System.out.println("unknown because the previous wifi scan was ages ago");
				lc.setLocationMode(UNKNOWN);
				return lc;
			}
			
			
//			List<Long> prevTimeStamps = new ArrayList<Long>();
//			List<Location> lastLocList = new ArrayList<Location>();
//			for (Location loc : histLocs)
//				if (loc.getTime() >= time - HISTORY_THRESHOLD_MILLIS && !prevTimeStamps.contains(loc.getTime())) // make sure old points aren't getting mixed in
//				{
//					lastLocList.add(loc);
//					prevTimeStamps.add(loc.getTime());
//				}
			
//				else
//					// System.out.println("Skippin' "+ scan.getAccessPoints().size());
//			List<String> currentLocList = getSSIDList(wifiScan.getAccessPoints());
			
			// Compare to the access points from last time
			double maxDist = 0;
			double travelled = Distance(currLoc, histLocs.get(0));
//			List<Double> speeds = new ArrayList<Double>();
//			List<Location> othersSoFar = new ArrayList<Location>();
			// Now we can do the comparison
//			Location prev = null;
			for (Location loc1 : histLocs) {
//				if (prev != null)
					
				for (Location loc2 : histLocs)
				{
					double dist = Distance(loc1, loc2);
					if (dist > maxDist)
						maxDist = dist;
				}
			}
			double radius = maxDist / 2; // not used, see next part
			
			
			if (histLocs.size() >= 2)
            {
            	int index = histLocs.size() - 2;
                while (index >= 0)
                {
                	if (histLocs.get(histLocs.size() - 1).getTime() > 60  * 1000 + histLocs.get(index).getTime())
                
                	{
                		radius = Distance(histLocs.get(histLocs.size() - 1), histLocs.get(index)) / 2;
                		break;
                	}
                	index--;
                }
            }   
			
			
			
//			for (String ssid : lastSSIDList) {
//				if (! currentSSIDList.contains(ssid)) { // only count others that don't match. We don't count the same ones again. Change that if too many false DRIVE classifications
//					total++;
//				}
//			}
			
			

			lc.setRadius(radius);
			lc.setTravelled(travelled);
			// TODO after weka, make it do correct logic
			// lc.setLocationMode(mode);
			
		}
		lc.setLocationMode(UNKNOWN);
		return lc;
	}

	private double NaïveDistance(Location loc1, Location loc2) {
		// TODO Auto-generated method stub
		return Math.sqrt(Math.pow(loc1.getLatitude() - loc2.getLatitude(), 2) + Math.pow(loc1.getLongitude() - loc2.getLongitude(), 2));
	}

	private double Distance(Location loc1, Location loc2) {
    	
	    double pk = (float) (180/3.14169);

	    double a1 = loc1.getLatitude() / pk;
	    double a2 = loc1.getLongitude() / pk;
	    double b1 = loc2.getLatitude() / pk;
	    double b2 = loc2.getLongitude() / pk;

	    double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
	    double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
	    double t3 = Math.sin(a1)*Math.sin(b1);
	    double tt = Math.acos(t1 + t2 + t3);
	   
	    return 6366000*tt;
    	
    }
	
	/**
	 * Compares current WiFi point to the previous one, and returns UNKNOWN, STILL, or DRIVE
	 * @param sample
	 * @return Magnitude value
	 */
	private Classification checkWifi(WifiScan wifiScan, List<WifiScan> lastWifiScans, Classification lastClassification) {
		long time = wifiScan.getTime().longValue();
		Classification wifiClassification = new Classification();
		if (lastWifiScans != null && lastWifiScans.size() > 0) {
			long lastTime = lastWifiScans.get(lastWifiScans.size() - 1).getTime().longValue();
			
			if (lastTime == time) { // no new wifi data
				// System.out.println("At " + time + " lastMode is " + lastMode);
				
				wifiClassification.updateWifi(lastClassification);
				return wifiClassification;
			}
//			System.err.println(lastTime + " " + time);
//			else
				// System.out.println("This is a new point: " + time + " is not " + lastTime);

			if (lastTime < time - WIFI_THRESHOLD_MILLIS) { // if no recent wifi for comparison
				// System.out.println("unknown because the previous wifi scan was ages ago");
				wifiClassification.setWifiMode(UNKNOWN);
				return wifiClassification;
			}
			List<Long> prevTimeStamps = new ArrayList<Long>();
			List<String> lastSSIDList = new ArrayList<String>();
			for (WifiScan scan : lastWifiScans)
				if (scan.getTime().longValue() >= time - WIFI_THRESHOLD_MILLIS && !prevTimeStamps.contains(scan.getTime())) // make sure old points aren't getting mixed in
				{
					lastSSIDList.addAll(getSSIDList(scan.getAccessPoints()));
					prevTimeStamps.add(scan.getTime());
				}
//				else
//					// System.out.println("Skippin' "+ scan.getAccessPoints().size());
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
//			for (String ssid : lastSSIDList) {
//				if (! currentSSIDList.contains(ssid)) { // only count others that don't match. We don't count the same ones again. Change that if too many false DRIVE classifications
//					total++;
//				}
//			}
			
			String mode = UNKNOWN;
			if (total > 0)
			{
				int threshold = 2;
				if (total <= 3)
					threshold = 1;
				if (total == 1)
					threshold = 0;
				if (same <= threshold)
				{
					mode = DRIVE;// + " " + same / total;
				}
				else
				{
					mode = STILL;// + " " + same / total;
				}

			}
			else
			{
				// System.out.println("unknown because there are no APs in this sample");
				mode = UNKNOWN;
			}
			wifiClassification.setWifiMode(mode);
			wifiClassification.setWifiTotal((int)total);
			wifiClassification.setWifiRecogTotal((int)same);
			return wifiClassification;
			
		}
		else {
			// System.out.println("unknown because last wifi scans were null or empty");
			wifiClassification.setWifiMode(UNKNOWN);
			return wifiClassification;
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
			
//			if (accessPoints.get(i).getStrength() > -50) {
//				ssidList.add(ssid);
//			}
		}
		
		if (ssidList.size() == 0 && strcount > 0) {
			double avg = strsum / strcount;
			
			for (int i = 0; i < numberOfAccessPoints; i++) {
				
				String ssid = accessPoints.get(i).getSsid(); 
				Double strength = accessPoints.get(i).getStrength();
				
//				strsum += strength;
//				strcount++;
				
				if (strength >= avg) {
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
	
	
	private String activity(Double gps_speed, double avg, double var, double a1, double a2, double a3, double a4, double a5,
			double a6, double a7, double a8, double a9, double a0, Classification classification)
	{
		
		if (var <= 0.038625)
			if ((classification.getWifiRecogTotal() <= 3 && classification.getWifiRecogRatio() <= .380952) || classification.getRadius() > 108)
				return DRIVE;
			else
				return STILL;
		else return WALK;
						
		
//		if (classification.getWifiRecogTotal() <= 3)
//		{
//			if (var <= 0.038625)
//			{
//				if (classification.getRadius() <= 108.095049)//38.44889)//0.002696)
//				{
//					if (avg <= .999593)
//						return DRIVE;
//					else
//						return STILL;
//				}
//				else
//					return DRIVE;
//			}
//			else
//				return WALK;
//		}
//		else
//		{
//			if (a9 <= 1.206552)//(a2 <= .314018)
//				return STILL;
//			else
//				return WALK;
//		}
	}
	
	/**
	 * This is the old main classification method. Updated code after retraining
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
//		Sample sample = new Sample(0, 0, 1);
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
//		ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
//		accessPoints.add(new AccessPoint("1", -40));
//		accessPoints.add(new AccessPoint("2", -40));
//		accessPoints.add(new AccessPoint("3", -30));
//		accessPoints.add(new AccessPoint("4", -80));
//		accessPoints.add(new AccessPoint("5", -30));
//		WifiScan ws1 = new WifiScan(System.currentTimeMillis() - 1000, accessPoints);
//		accessPoints = new ArrayList<AccessPoint>();
//		accessPoints.add(new AccessPoint("1", -30));
//		accessPoints.add(new AccessPoint("2", -30));
////		accessPoints.add(new AccessPoint("3", -40));
////		accessPoints.add(new AccessPoint("4", -40));
////		accessPoints.add(new AccessPoint("5", -40));
//		WifiScan ws2 = new WifiScan(System.currentTimeMillis() - 2000, accessPoints);
//		accessPoints = new ArrayList<AccessPoint>();
////		accessPoints.add(new AccessPoint("3", -40));
////		accessPoints.add(new AccessPoint("4", -30));
////		accessPoints.add(new AccessPoint("5", -40));
//		
//		WifiScan ws3 = new WifiScan(System.currentTimeMillis() - 3000, accessPoints);
//		accessPoints = new ArrayList<AccessPoint>();
//		accessPoints.add(new AccessPoint("1", -40));
//		accessPoints.add(new AccessPoint("2", -40));
//		accessPoints.add(new AccessPoint("3", -40));
//		accessPoints.add(new AccessPoint("4", -40));
//		accessPoints.add(new AccessPoint("5", -40));
//		
//		WifiScan ws4 = new WifiScan(System.currentTimeMillis() - 4000, accessPoints);
//		
//		
//		List<WifiScan> lasts = new ArrayList<WifiScan>();
//		lasts.add(ws2);
//		lasts.add(ws3);
//		lasts.add(ws4);
//		System.out.println("lasts has " + lasts.size());
//		System.out.println(new MobilityClassifier().classify(accelValues, 1.0, ws1, lasts, UNKNOWN).getMode());
//	}
}
