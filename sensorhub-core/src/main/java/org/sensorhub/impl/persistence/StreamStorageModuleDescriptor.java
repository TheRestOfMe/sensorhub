/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


public class StreamStorageModuleDescriptor implements IModuleProvider
{
    @Override
    public String getModuleName()
    {
        return "Data Stream Storage";
    }

    @Override
    public String getModuleDescription()
    {
        return "Storage wrapper for storing a stream of data records to an underlying storage";
    }

    @Override
    public String getModuleVersion()
    {
        return "1.0";
    }

    @Override
    public String getProviderName()
    {
        return "Sensia Software LLC";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return GenericStreamStorage.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return StreamStorageConfig.class;
    }
}
