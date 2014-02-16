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

package org.sensorhub.impl.service.sps;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.service.IServiceInterface;


/**
 * <p>
 * OGC Sensor Planning Service implementation.
 * The service can manage any of the sensors installed on the SensorHub instance
 * and is configured automatically from the information generated by the sensors
 * interface.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class SPSService implements IServiceInterface<SPSServiceConfig>
{
    SPSServiceConfig config;
    
    
    public SPSService()
    {
        
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(SPSServiceConfig config) throws SensorHubException
    {
        this.config = config;
    }


    @Override
    public void updateConfig(SPSServiceConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub

    }
    
    
    @Override
    public void start()
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void stop()
    {
        // TODO Auto-generated method stub
    }


    @Override
    public SPSServiceConfig getConfiguration()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getName()
    {
        return config.name;
    }
    
    
    @Override
    public String getLocalID()
    {
        return config.id;
    }
    

    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub        
    }

}
