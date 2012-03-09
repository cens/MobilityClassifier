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

/**
 * Immutable bean-style wrapper for Wi-Fi access points.
 * 
 * @author Joshua Selsky
 */
public class AccessPoint {
	private final String ssid;
	private final Double strength;
	
	/**
	 * A reading from an access point.
	 * 
	 * @param ssid The access point's unique identifier.
	 * 
	 * @param strength The strength of the signal from the access point at the
	 * 				   time of the reading.
	 */
	public AccessPoint(String ssid, double strength) {
		if(ssid == null || ssid.trim().equals("")) {
			throw new IllegalArgumentException("An SSID is required");
		}

		this.ssid = ssid;
		this.strength = strength;
	}

	/**
	 * Returns the unique identifier of the access point.
	 * 
	 * @return The unique identifier of the access point.
	 */
	public String getSsid() {
		return ssid;
	}

	/**
	 * Returns the strength of the signal from the access point.
	 * 
	 * @return The strength of the signal from the access point.
	 */
	public Double getStrength() {
		return strength;
	}

	@Override
	public String toString() {
		return "AccessPoint [ssid=" + ssid + ", strength=" + strength + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ssid == null) ? 0 : ssid.hashCode());
		result = prime * result
				+ ((strength == null) ? 0 : strength.hashCode());
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
		AccessPoint other = (AccessPoint) obj;
		if (ssid == null) {
			if (other.ssid != null)
				return false;
		} else if (!ssid.equals(other.ssid))
			return false;
		if (strength == null) {
			if (other.strength != null)
				return false;
		} else if (!strength.equals(other.strength))
			return false;
		return true;
	}
}