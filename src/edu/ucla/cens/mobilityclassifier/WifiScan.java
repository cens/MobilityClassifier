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
 * Immutable bean-style wrapper for a Wi-Fi scan.
 * 
 * @author Joshua Selsky
 */
public class WifiScan {
	private final Long time;
	private final List<AccessPoint> accessPoints;
	
	/**
	 * Creates a new WiFi scan.
	 * 
	 * @param time The time at which the scan occurred.
	 * 
	 * @param accessPoints The list of access points that were heard from in
	 * 					   this scan.
	 */
	public WifiScan(
			final Long time, 
			final List<AccessPoint> accessPoints) {
		
		if(time == null) {
			throw new IllegalArgumentException("Time is required");
		}
		if(accessPoints == null) {
			throw new IllegalArgumentException("Access points are required");
		}
		
		this.time = time;
		this.accessPoints = accessPoints;
	}

	/**
	 * Returns the time at which the scan occurred.
	 * 
	 * @return The time at which the scan occurred.
	 */
	public Long getTime() {
		return time;
	}

	/**
	 * Returns the list of access points that were heard from in this scan.
	 * 
	 * @return The list of access points that were heard form in this scan.
	 */
	public List<AccessPoint> getAccessPoints() {
		return accessPoints;
	}

	@Override
	public String toString() {
		return "WifiScan [time=" + time + ", accessPoints=" + accessPoints
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessPoints == null) ? 0 : accessPoints.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WifiScan other = (WifiScan) obj;
		if (accessPoints == null) {
			if (other.accessPoints != null)
				return false;
		} else if (!accessPoints.equals(other.accessPoints))
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		return true;
	}
}