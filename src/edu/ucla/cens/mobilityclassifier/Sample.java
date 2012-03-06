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
 * A sample reading from the accelerometer of our device.
 * 
 * @author John Jenkins
 */
public class Sample
{
	private double x;
	private double y;
	private double z;
	
	/**
	 * Creates a new Sample from the three accelerometer points.
	 * 
	 * @param x The X component of the accelerometer reading.
	 * 
	 * @param y The Y component of the accelerometer reading.
	 * 
	 * @param z The Z component of the accelerometer reading.
	 */
	public Sample(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Returns the X component of the accelerometer reading.
	 * 
	 * @return The X component of the accelerometer reading.
	 */
	public Double getX() {
		return x;
	}

	/**
	 * Returns the Y component of the accelerometer reading.
	 * 
	 * @return The Y component of the accelerometer reading.
	 */
	public Double getY()
	{
		return y;
	}

	/**
	 * Returns the Z component of the accelerometer reading.
	 * 
	 * @return The Z component of the accelerometer reading.
	 */
	public Double getZ()
	{
		return z;
	}
}