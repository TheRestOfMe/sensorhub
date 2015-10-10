/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSDataFilter;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Factory for sensor data providers with storage.<br/>
 * Most of the logic is inherited from {@link StreamWithStorageProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2014
 */
public class SensorWithStorageProviderFactory extends StreamWithStorageProviderFactory<ISensorModule<?>>
{
    SensorDataProviderConfig sensorProviderConfig;
    
    
    public SensorWithStorageProviderFactory(SensorDataProviderConfig config) throws SensorHubException
    {
        super(config, SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID));
        this.sensorProviderConfig = config;
        
        String liveSensorUID = producer.getCurrentDescription().getUniqueIdentifier();
        String storageSensorUID = storage.getLatestDataSourceDescription().getUniqueIdentifier();
        if (!liveSensorUID.equals(storageSensorUID))
            throw new SensorHubException("Storage " + storage.getName() + " doesn't contain data for sensor " + producer.getName());
    }


    @Override
    public ISOSDataProvider getNewDataProvider(SOSDataFilter filter) throws ServiceException
    {
        TimeExtent timeRange = filter.getTimeRange();
        
        if (timeRange.isBaseAtNow() || timeRange.isBeginNow())
        {
            if (!producer.isEnabled())
                throw new ServiceException("Sensor " + MsgUtils.moduleString(producer) + " is disabled");
            
            return new SensorDataProvider(producer, sensorProviderConfig, filter);
        }
        else
        {            
            return super.getNewDataProvider(filter);
        }
    }
}
