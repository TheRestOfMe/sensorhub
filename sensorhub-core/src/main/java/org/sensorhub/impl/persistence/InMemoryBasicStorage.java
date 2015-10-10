/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageEvent;
import org.sensorhub.api.persistence.StorageEvent.Type;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * In-memory basic storage implementation.
 * This is used mainly for test purposes but could perhaps be improved to be
 * used as a local memory cache of a remote storage.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 8, 2013
 */
public class InMemoryBasicStorage extends AbstractModule<StorageConfig> implements IRecordStorageModule<StorageConfig>
{
    Map<String, TimeSeriesImpl> dataStores = new LinkedHashMap<String, TimeSeriesImpl>();
    List<AbstractProcess> dataSourceDescriptions = new ArrayList<AbstractProcess>();
    
    
    public InMemoryBasicStorage()
    {
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
    }


    @Override
    public void stop() throws SensorHubException
    {                
    }


    @Override
    public AbstractProcess getLatestDataSourceDescription()
    {
        int historySize = dataSourceDescriptions.size();
        if (historySize == 0)
            return null;
        return dataSourceDescriptions.get(historySize - 1);
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        ArrayList<AbstractProcess> smlList = new ArrayList<AbstractProcess>();
        
        for (AbstractProcess sml: dataSourceDescriptions)
        {
            AbstractTimeGeometricPrimitive validTime = sml.getValidTimeList().get(0);
            
            if (validTime instanceof TimeInstant)
            {
                double time = ((TimeInstant)validTime).getTimePosition().getDecimalValue();
                if (time >= startTime && time <= endTime)
                    smlList.add(sml);
            }
            else if (validTime instanceof TimePeriod)
            {
                double beginPeriod = ((TimePeriod)validTime).getBeginPosition().getDecimalValue();
                double endPeriod = ((TimePeriod)validTime).getBeginPosition().getDecimalValue();
                if ((beginPeriod >= startTime && beginPeriod <= endTime) ||
                    (endPeriod >= startTime && endPeriod <= endTime))
                    smlList.add(sml);
            }
        }
        
        return smlList;
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        for (AbstractProcess sml: dataSourceDescriptions)
        {
            AbstractTimeGeometricPrimitive validTime = sml.getValidTimeList().get(0);
            
            if (validTime instanceof TimeInstant)
            {
                if (time == ((TimeInstant)validTime).getTimePosition().getDecimalValue())
                    return sml;
            }
            else if (validTime instanceof TimePeriod)
            {
                if (time >= ((TimePeriod)validTime).getBeginPosition().getDecimalValue() &&
                    time <= ((TimePeriod)validTime).getEndPosition().getDecimalValue())
                    return sml;
            }
        }
        
        return null;
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process)
    {
        dataSourceDescriptions.add(process);        
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process)
    {
        int index = dataSourceDescriptions.indexOf(process);
        dataSourceDescriptions.set(index, process);
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        AbstractProcess process = getDataSourceDescriptionAtTime(time);
        if (process != null)
            dataSourceDescriptions.remove(process);
    }


    @Override
    public void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        dataSourceDescriptions.clear();        
    }


    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        return Collections.unmodifiableMap(dataStores);
    }


    @Override
    public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        TimeSeriesImpl timeSeries = new TimeSeriesImpl(recordStructure.copy(), recommendedEncoding);
        dataStores.put(name, timeSeries);
    }


    @Override
    public void backup(OutputStream os)
    {
    }


    @Override
    public void restore(InputStream is)
    {
    }


    @Override
    public void sync(IStorageModule<?> storage)
    {
    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
    }
    
    
    @Override
    public boolean isAutoCommit()
    {
        return false;
    }
    

    @Override
    public void commit()
    {
    }


    @Override
    public void rollback()
    {
    }
    
    
    @Override
    public void cleanup() throws StorageException
    {
        dataStores.clear();
        dataSourceDescriptions.clear();
    }
    
    
    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        TimeSeriesImpl dataStore = dataStores.get(key.recordType);
        if (dataStore == null)
            return null;
        
        return dataStore.getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        TimeSeriesImpl dataStore = dataStores.get(filter.getRecordType());
        if (dataStore == null)
            return null;
        
        return dataStore.getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        TimeSeriesImpl dataStore = dataStores.get(filter.getRecordType());
        if (dataStore == null)
            return null;
        
        return dataStore.getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter)
    {
        TimeSeriesImpl dataStore = dataStores.get(filter.getRecordType());
        if (dataStore == null)
            return 0;
        
        return dataStore.getNumMatchingRecords(filter);
    }


    @Override
    public int getNumRecords(String recordType)
    {
        TimeSeriesImpl dataStore = dataStores.get(recordType);
        if (dataStore == null)
            return 0;
        
        return dataStore.getNumRecords();
    }

    
    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        TimeSeriesImpl dataStore = dataStores.get(recordType);
        if (dataStore == null)
            return new double[] {Double.NaN, Double.NaN};
        
        return dataStore.getDataTimeRange();
    }
    
    
    @Override
    public Iterator<double[]> getRecordsTimeClusters(String recordType)
    {
        return Arrays.asList(getRecordsTimeRange(recordType)).iterator();
    }
    

    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        TimeSeriesImpl dataStore = dataStores.get(key.recordType);
        if (dataStore != null)
            dataStore.store(key, data);
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        TimeSeriesImpl dataStore = dataStores.get(key.recordType);
        if (dataStore != null)
            dataStore.update(key, data);        
    }


    @Override
    public void removeRecord(DataKey key)
    {
        TimeSeriesImpl dataStore = dataStores.get(key.recordType);
        if (dataStore == null)
            return;
        
        dataStore.remove(key);
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        TimeSeriesImpl dataStore = dataStores.get(filter.getRecordType());
        if (dataStore == null)
            return 0;
        
        return dataStore.remove(filter);
    }
    
    
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }
    

    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }


    /*
     * Implementation of individual time series {key,record} pair
     */
    private class DBRecord implements IDataRecord
    {
        DataKey key;
        DataBlock data;
        
        public DBRecord(DataKey key, DataBlock data)
        {
            this.key = key;
            this.data = data;
        }

        @Override
        public final DataKey getKey()
        {
            return key;
        }
        
        @Override
        public final DataBlock getData()
        {
            return data;
        }
        
        protected final boolean matches(DataKey key)
        {
            return ( (key.producerID == null || key.producerID.equals(this.key.producerID)) &&
                 (key.timeStamp == Double.NaN || (key.timeStamp == this.key.timeStamp)) );
        }
        
        protected final boolean matches(IDataFilter filter)
        {
            return ( (filter.getProducerIDs() == null || filter.getProducerIDs().contains(this.key.producerID)) &&
                 (filter.getTimeStampRange() == null || (filter.getTimeStampRange()[0] <= this.key.timeStamp && filter.getTimeStampRange()[1] >= this.key.timeStamp)) );
        }
    }
    
    
    /*
     * Implementation of an individual time series data store
     */
    public class TimeSeriesImpl implements IRecordStoreInfo
    {
        List<DBRecord> recordList = new LinkedList<DBRecord>();;
        DataComponent recordDescription;
        DataEncoding recommendedEncoding;
        
        TimeSeriesImpl(DataComponent recordDescription, DataEncoding recommendedEncoding)
        {
            this.recordDescription = recordDescription;
            this.recommendedEncoding = recommendedEncoding;
        }
        
        @Override
        public String getName()
        {
            return recordDescription.getName();
        }
        
        @Override
        public DataComponent getRecordDescription()
        {
            return recordDescription;
        }
        
        @Override
        public DataEncoding getRecommendedEncoding()
        {
            return recommendedEncoding;
        }
        
        public int getNumRecords()
        {
            return recordList.size();
        }

        public DataBlock getDataBlock(DataKey key)
        {
            IDataRecord rec = getRecord(key);
            return rec.getData();
        }

        public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
        {
            final Iterator<DBRecord> it = getRecordIterator(filter);
            return new Iterator<DataBlock>() {

                public final boolean hasNext()
                {
                    return it.hasNext();
                }

                public final DataBlock next()
                {
                    return it.next().getData();
                }

                public final void remove()
                {
                    it.remove();                    
                }                
            };
        }

        public IDataRecord getRecord(DataKey key)
        {
            Iterator<DBRecord> it = recordList.iterator();
            while (it.hasNext())
            {
                DBRecord rec = it.next();
                if (rec.matches(key))
                    return rec;
            }
            
            return null;
        }

        public int getNumMatchingRecords(IDataFilter filter)
        {
            final Iterator<DBRecord> it = getRecordIterator(filter);
            int matchCount = 0;
            while (it.hasNext())
            {
                if (it.next().matches(filter))
                    matchCount++;
            }
            return matchCount;
        }
        
        public Iterator<DBRecord> getRecordIterator(final IDataFilter filter)
        {
            final Iterator<DBRecord> it = recordList.iterator();
            return new Iterator<DBRecord>() {
                DBRecord nextRec;
                
                public final boolean hasNext()
                {
                    nextRec = fetchNext();
                    return (nextRec != null);
                }

                public final DBRecord next()
                {
                    if (nextRec != null)
                    {
                        // get already fetched record and clear it
                        DBRecord rec = nextRec;
                        nextRec = null;
                        return rec;
                    }
                    
                    return fetchNext();
                }
                
                protected final DBRecord fetchNext()
                {
                    while (it.hasNext())
                    {
                        DBRecord rec = it.next();
                        if (rec.matches(filter))
                            return rec;
                    }
                    
                    return null;
                }

                public final void remove()
                {
                    it.remove();                    
                }                
            };
        }

        public DataKey store(DataKey key, DataBlock data)
        {
            recordList.add(new DBRecord(key, data));
            eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), InMemoryBasicStorage.this, key.recordType, Type.STORE));
            return key;
        }

        public void update(DataKey key, DataBlock data)
        {
            ListIterator<DBRecord> it = recordList.listIterator();
            while (it.hasNext())
            {
                DBRecord rec = it.next();                
                if (rec.matches(key))
                    rec.data = data;
            }
        }

        public void remove(DataKey key)
        {
            ListIterator<DBRecord> it = recordList.listIterator();
            while (it.hasNext())
            {
                DBRecord rec = it.next();                
                if (rec.matches(key))
                    it.remove();
            }
        }

        public int remove(IDataFilter filter)
        {
            ListIterator<DBRecord> it = recordList.listIterator();
            int count = 0;
            while (it.hasNext())
            {
                DBRecord rec = it.next();
                if (rec.matches(filter))
                {
                    it.remove();
                    count++;
                }
            }
            
            return count;
        }
        
        public double[] getDataTimeRange()
        {
            double[] period = new double[] { Double.MAX_VALUE, Double.MIN_VALUE};
            
            Iterator<DBRecord> it = recordList.iterator();
            while (it.hasNext())
            {
                double timeStamp = it.next().getKey().timeStamp;
                if (timeStamp < period[0])
                    period[0] = timeStamp;
                if (timeStamp > period[1])
                    period[1] = timeStamp;
            }
            
            return period;
        }
    }
}
