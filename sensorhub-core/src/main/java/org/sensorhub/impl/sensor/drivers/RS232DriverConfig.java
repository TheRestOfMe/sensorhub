/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.drivers;

import org.sensorhub.api.sensor.SensorDriverConfig;


/**
 * <p>
 * Driver configuration options for the RS232 hardware interface
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public class RS232DriverConfig extends SensorDriverConfig
{
	private static final long serialVersionUID = 7284981188037101005L;
	

    /**
	 * Try to detect newly connected device by polling
	 */
    public boolean autodetect;
	
	public int autodetectInterval;
	
	public String portNumber;
	
	public int baudRate;
	
	public int numberOfBits;
	
	public boolean parity;
}
