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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;
import org.vast.swe.ScalarIndexer;
import org.vast.util.Bbox;


/**
 * <p>
 * Generic wrapper/adapter enabling any storage implementation to store data
 * coming from data events (e.g. sensor data, processed data, etc.)<br/>
 * This class takes care of registering with the appropriate producers and
 * uses the storage API to store records in the underlying storage.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 21, 2015
 */
public class GenericStreamStorage extends AbstractModule<StreamStorageConfig> implements IRecordStorageModule<StreamStorageConfig>, IObsStorage, IEventListener
{
    private static final Logger log = LoggerFactory.getLogger(GenericStreamStorage.class);
    
    IRecordStorageModule<StorageConfig> storage;
    WeakReference<IDataProducerModule<?>> dataSourceRef;
    Map<String, ScalarIndexer> timeStampIndexers = new HashMap<String, ScalarIndexer>();
    Map<String, String> currentFoiMap = new HashMap<String, String>(); // entity ID -> current FOI ID
    
    long lastCommitTime = Long.MIN_VALUE;
    String currentFoi;
    Timer autoPurgeTimer;
    
    
    @Override
    public void start() throws SensorHubException
    {
        // instantiate and start underlying storage
        StorageConfig storageConfig = null;
        try
        {
            storageConfig = (StorageConfig)config.storageConfig.clone();
            storageConfig.id = getLocalID();
            storageConfig.name = getName();
            Class<?> clazz = (Class<?>)Class.forName(storageConfig.moduleClass);
            storage = (IRecordStorageModule<StorageConfig>)clazz.newInstance();
            storage.init(storageConfig);
            storage.start();
        }
        catch (Exception e)
        {
            if (storageConfig == null)
                throw new StorageException("Underlying storage configuration must be provided for generic storage " + config.name);
            else
                throw new StorageException("Cannot instantiate underlying storage " + storageConfig.moduleClass, e);
        }
        
        // retrieve reference to data source
        ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
        dataSourceRef = (WeakReference<IDataProducerModule<?>>)moduleReg.getModuleRef(config.dataSourceID);
        
        // connect to data source to fetch initial info
        IDataProducerModule<?> dataSource = dataSourceRef.get();
        if (dataSource != null)
        {        
            // if storage is empty, initialize it
            if (storage.getLatestDataSourceDescription() == null)
                configureStorageForDataSource(dataSource, storage);
            
            // otherwise just get the latest sensor description in case we were down during the last update
            else if (dataSource.getLastDescriptionUpdate() != Long.MIN_VALUE)
                storage.storeDataSourceDescription(dataSource.getCurrentDescription());
            
            // also init current FOI
            if (dataSource instanceof IMultiSourceDataProducer)
            {
                for (String entityID: ((IMultiSourceDataProducer)dataSource).getEntityIDs())
                {
                    AbstractFeature foi = ((IMultiSourceDataProducer)dataSource).getCurrentFeatureOfInterest(entityID);
                    if (foi != null)
                    {
                        currentFoiMap.put(entityID, foi.getUniqueIdentifier());
                        if (storage instanceof IObsStorage)
                            ((IObsStorage)storage).storeFoi(entityID, foi);
                    }
                }
            }
            else
            {
                String producerID = dataSource.getCurrentDescription().getUniqueIdentifier();
                AbstractFeature foi = dataSource.getCurrentFeatureOfInterest();
                if (foi != null)
                {
                    currentFoi = foi.getUniqueIdentifier();
                    currentFoiMap.put(producerID, currentFoi);
                    if (storage instanceof IObsStorage)
                        ((IObsStorage)storage).storeFoi(producerID, foi);
                }
            }
            
            // register to data events
            for (IStreamingDataInterface output: getSelectedOutputs(dataSource))
                prepareToReceiveEvents(output);
            
            // register to datasource change events
            dataSource.registerListener(this);
        }
        else
            log.warn("Data source is unavailable for stream storage " + MsgUtils.moduleString(this));
        
        // start auto-purge timer thread if policy is specified and enabled
        if (config.autoPurgeConfig != null && config.autoPurgeConfig.enabled)
        {
            final IStorageAutoPurgePolicy policy = config.autoPurgeConfig.getPolicy();
            autoPurgeTimer = new Timer();
            TimerTask task = new TimerTask() {
                public void run()
                {
                    policy.trimStorage(storage);
                }
            };            
            autoPurgeTimer.schedule(task, 0, (long)(config.autoPurgeConfig.purgePeriod*1000)); 
        }
    }
    
    
    /*
     * Initializes storage by loading initial sensor description and FOI, and creating appropriate record stores
     */
    protected void configureStorageForDataSource(IDataProducerModule<?> dataSource, IRecordStorageModule<?> storage) throws StorageException
    {
        if (storage.getRecordStores().size() > 0)
            throw new RuntimeException("Storage " + MsgUtils.moduleString(storage) + " is already configured");
        
        // copy data source description
        storage.storeDataSourceDescription(dataSource.getCurrentDescription());
            
        // for multi-source producers, prepare data stores for all entities
        if (dataSource instanceof IMultiSourceDataProducer && storage instanceof IMultiSourceStorage)
        {
            for (String entityID: ((IMultiSourceDataProducer)dataSource).getEntityIDs())
                ensureProducerInfo(entityID);
        }
        
        // create one data store for each sensor output
        // we do that in multi source storage even if it's also done in each provider data store
        for (IStreamingDataInterface output: getSelectedOutputs(dataSource))
            storage.addRecordStore(output.getName(), output.getRecordDescription(), output.getRecommendedEncoding());
    }
    
    
    /*
     * Gets the list of selected outputs (i.e. a subset of all data source outputs)
     */
    protected Collection<? extends IStreamingDataInterface> getSelectedOutputs(IDataProducerModule<?> dataSource)
    {
        if (config.selectedOutputs == null || config.selectedOutputs.length == 0)
        {
            return dataSource.getAllOutputs().values();
        }
        else
        {
            int numOutputs = config.selectedOutputs.length;
            List <IStreamingDataInterface> selectedOutputs = new ArrayList<IStreamingDataInterface>(numOutputs);
            for (String outputName: config.selectedOutputs)
                selectedOutputs.add(dataSource.getAllOutputs().get(outputName));
            return selectedOutputs;
        }
    }
    
    
    protected void ensureProducerInfo(String producerID)
    {
        if (storage instanceof IMultiSourceStorage)
        {
            if (((IMultiSourceStorage<?>)storage).getProducerIDs().contains(producerID))
                return;
        
            IDataProducerModule<?> dataSource = dataSourceRef.get();
            if (dataSource != null && dataSource instanceof IMultiSourceDataProducer)
            {
                // create producer data store
                IBasicStorage dataStore = ((IMultiSourceStorage<IBasicStorage>)storage).addDataStore(producerID);
                
                // save producer SensorML description if any
                AbstractProcess sml = ((IMultiSourceDataProducer) dataSource).getCurrentDescription(producerID);
                if (sml != null)
                    dataStore.storeDataSourceDescription(sml);
                
                // create one data store for each sensor output
                for (IStreamingDataInterface output: getSelectedOutputs(dataSource))
                    dataStore.addRecordStore(output.getName(), output.getRecordDescription(), output.getRecommendedEncoding());
            }
        }
    }
    
    
    protected void prepareToReceiveEvents(IStreamingDataInterface output)
    {
        // create time stamp indexer
        String outputName = output.getName();
        ScalarIndexer timeStampIndexer = timeStampIndexers.get(outputName);
        if (timeStampIndexer == null)
        {
            timeStampIndexer = SWEHelper.getTimeStampIndexer(output.getRecordDescription());
            timeStampIndexers.put(outputName, timeStampIndexer);
        }
        
        output.registerListener(this);
    }
    
        
    @Override
    public void stop() throws SensorHubException
    {
        if (dataSourceRef != null)
        {
            IDataProducerModule<?> dataSource = dataSourceRef.get();
            if (dataSource != null)
            {            
                for (IStreamingDataInterface output: getSelectedOutputs(dataSource))
                    output.unregisterListener(this);
            }
            
            dataSourceRef = null;
        }
        
        if (autoPurgeTimer != null)
            autoPurgeTimer.cancel();

        if (storage != null)
            storage.stop();
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        if (storage != null)
            storage.cleanup();
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        if (config.processEvents)
        {
            // new data events
            if (e instanceof DataEvent)
            {
                DataEvent dataEvent = (DataEvent)e;
                boolean saveAutoCommitState = storage.isAutoCommit();
                storage.setAutoCommit(false);
                
                // get indexer for looking up time stamp value
                String outputName = dataEvent.getSource().getName();
                ScalarIndexer timeStampIndexer = timeStampIndexers.get(outputName);
                
                for (DataBlock record: dataEvent.getRecords())
                {
                    // get time stamp
                    double time;
                    if (timeStampIndexer != null)
                        time = timeStampIndexer.getDoubleValue(record);
                    else
                        time = e.getTimeStamp() / 1000.;
                    
                    // get FOI ID
                    String foiID;
                    String entityID = dataEvent.getRelatedEntityID();
                    if (entityID != null)
                    {
                        ensureProducerInfo(entityID); // to handle new producer
                        foiID = currentFoiMap.get(entityID);
                    }
                    else
                        foiID = currentFoi; 
                    
                    // store record with proper key
                    ObsKey key = new ObsKey(outputName, entityID, foiID, time);                    
                    storage.storeRecord(key, record);
                    
                    if (log.isTraceEnabled())
                        log.trace("Storing record " + key.timeStamp + " for output " + outputName);
                }
                
                // commit only when necessary
                long now = System.currentTimeMillis();
                if (lastCommitTime == Long.MIN_VALUE || (now - lastCommitTime) > config.minCommitPeriod)
                {
                    storage.commit();
                    lastCommitTime = now;
                }
                
                storage.setAutoCommit(saveAutoCommitState);
            }
            
            else if (e instanceof SensorEvent)
            {
                if (((SensorEvent) e).getType() == SensorEvent.Type.SENSOR_CHANGED)
                {
                    // TODO check that description was actually updated?
                    // in the current state, the same description would be added at each restart
                    // should we compare contents? if not, on what time tag can we rely on?
                    // AbstractSensorModule implementation of getLastSensorDescriptionUpdate() is
                    // only useful between restarts since it will be resetted to current time at startup...
                    
                    // TODO to manage this issue, first check that no other description is valid at the same time
                    storage.storeDataSourceDescription(dataSourceRef.get().getCurrentDescription());
                }
            }
            
            else if (e instanceof FoiEvent && storage instanceof IObsStorage)
            {
                FoiEvent foiEvent = (FoiEvent)e;
                String producerID = ((FoiEvent) e).getRelatedEntityID();
                
                // store feature object if specified
                if (foiEvent.getFoi() != null)
                {
                    if (producerID != null)
                        ensureProducerInfo(producerID); // in case no data has been received for this producer yet
                    ((IObsStorage) storage).storeFoi(producerID, foiEvent.getFoi());
                }
                
                // also remember as current FOI
                if (producerID != null)
                    currentFoiMap.put(producerID, foiEvent.getFoiID());
                else
                    currentFoi = foiEvent.getFoiID();
            }
        }
    }
    

    @Override
    public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        checkStarted();
        
        // register new record type with underlying storage
        if (!storage.getRecordStores().containsKey(name))
            storage.addRecordStore(name, recordStructure, recommendedEncoding);
        
        // prepare to receive events
        IDataProducerModule<?> dataSource = dataSourceRef.get();
        if (dataSource != null)
            prepareToReceiveEvents(dataSource.getAllOutputs().get(name));
    }


    @Override
    public void backup(OutputStream os) throws IOException
    {
        checkStarted();
        storage.backup(os);        
    }


    @Override
    public void restore(InputStream is) throws IOException
    {
        checkStarted();
        storage.restore(is);        
    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
        checkStarted();
        storage.setAutoCommit(autoCommit);        
    }


    @Override
    public boolean isAutoCommit()
    {
        checkStarted();
        return storage.isAutoCommit();
    }


    @Override
    public void commit()
    {
        checkStarted();
        storage.commit();        
    }


    @Override
    public void rollback()
    {
        checkStarted();
        storage.rollback();        
    }


    @Override
    public void sync(IStorageModule<?> storage) throws StorageException
    {
        checkStarted();
        this.storage.sync(storage);        
    }


    @Override
    public AbstractProcess getLatestDataSourceDescription()
    {
        checkStarted();
        return storage.getLatestDataSourceDescription();
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        checkStarted();
        return storage.getDataSourceDescriptionHistory(startTime, endTime);
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        checkStarted();
        return storage.getDataSourceDescriptionAtTime(time);
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process)
    {
        checkStarted();
        storage.storeDataSourceDescription(process);        
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process)
    {
        checkStarted();
        storage.updateDataSourceDescription(process);        
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        checkStarted();
        storage.removeDataSourceDescription(time);        
    }


    @Override
    public void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        checkStarted();
        storage.removeDataSourceDescriptionHistory(startTime, endTime);
    }


    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        checkStarted();
        return storage.getRecordStores();
    }


    public DataBlock getDataBlock(DataKey key)
    {
        checkStarted();
        return storage.getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        checkStarted();
        return storage.getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        checkStarted();
        return storage.getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter)
    {
        checkStarted();
        return storage.getNumMatchingRecords(filter);
    }

    
    @Override
    public int getNumRecords(String recordType)
    {
        checkStarted();
        return storage.getNumRecords(recordType);
    }


    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        checkStarted();
        return storage.getRecordsTimeRange(recordType);
    }
    
    
    @Override
    public Iterator<double[]> getRecordsTimeClusters(String recordType)
    {
        checkStarted();
        return storage.getRecordsTimeClusters(recordType);
    }


    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        checkStarted();
        storage.storeRecord(key, data);
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        checkStarted();
        storage.updateRecord(key, data);
    }


    @Override
    public void removeRecord(DataKey key)
    {
        checkStarted();
        storage.removeRecord(key);
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        checkStarted();
        return storage.removeRecords(filter);
    }


    @Override
    public int getNumFois(IFoiFilter filter)
    {
        checkStarted();
        
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getNumFois(filter);
        
        return 0;
    }
    
    
    @Override
    public Bbox getFoisSpatialExtent()
    {
        checkStarted();
        
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFoisSpatialExtent();
        
        return null;
    }


    @Override
    public Iterator<String> getFoiIDs(IFoiFilter filter)
    {
        checkStarted();
        
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFoiIDs(filter);
        
        return Collections.EMPTY_LIST.iterator();
    }


    @Override
    public Iterator<AbstractFeature> getFois(IFoiFilter filter)
    {
        checkStarted();
        
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFois(filter);
        
        return Collections.EMPTY_LIST.iterator();
    }


    @Override
    public void storeFoi(String producerID, AbstractFeature foi)
    {
        checkStarted();
        if (storage instanceof IObsStorage)
            storeFoi(producerID, foi);        
    }
    
    
    private void checkStarted()
    {
        if (storage == null)
            throw new RuntimeException("Storage is disabled");
    }
}
