/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.service.HttpServer;
import com.vaadin.server.VaadinServlet;


public class AdminUIModule extends AbstractModule<AdminUIConfig>
{
    protected final static String SERVLET_PARAM_UI_CLASS = "UI";
    protected final static String SERVLET_PARAM_MODULE_ID = "module_id";
    protected final static String WIDGETSET = "widgetset";
    
    VaadinServlet vaadinServlet;
    
    
    @Override
    public void start() throws SensorHubException
    {
        // reset java util logging config so we don't get annoying atmosphere logs
        LogManager.getLogManager().reset();//.getLogger("org.atmosphere").setLevel(Level.OFF);
        
        vaadinServlet = new VaadinServlet();
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(SERVLET_PARAM_UI_CLASS, AdminUI.class.getCanonicalName());
        initParams.put(SERVLET_PARAM_MODULE_ID, getLocalID());
        if (config.widgetSet != null)
            initParams.put(WIDGETSET, config.widgetSet);
        initParams.put("productionMode", "true");  // set to false to compile theme on-the-fly       
        HttpServer.getInstance().deployServlet(vaadinServlet, initParams, "/admin/*", "/VAADIN/*");
        HttpServer.getInstance().addServletSecurity("/admin/*", "admin");
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        if (vaadinServlet != null)
            HttpServer.getInstance().undeployServlet(vaadinServlet);        
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {        
    }

}
