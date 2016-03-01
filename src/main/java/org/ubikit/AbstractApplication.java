/*
 *
 * Copyright (c) Immotronic, 2012
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of ubikit-core, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package org.ubikit;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.LoggerFactory;
import org.ubikit.event.EventGate;
import org.ubikit.event.EventInstanciator;
import org.ubikit.event.HttpEventGate;
import org.ubikit.event.impl.EventGateImpl;
import org.ubikit.event.impl.HttpEventGateImpl;
import org.ubikit.impl.ConfigurationProperties;
import org.ubikit.impl.DatabaseProxyImpl;
import org.ubikit.system.SystemApp;
import org.ubikit.impl.PhysicalEnvironmentModelManagerImpl;
import org.ubikit.service.AppRegistryService;
import org.ubikit.service.HSQLDatabaseService;
import org.ubikit.service.PhysicalEnvironmentModelService;
import org.ubikit.tools.BundleResourceUtil;
import org.ubikit.tools.http.HttpSecurityHandler;


public abstract class AbstractApplication
{
	
	/*
	 * ___NOTICEABLE_CONSTANTS_____________________________________________________________________
	 */
	
	private static final String app_resources_folder = "app-resources";	// ATTENTION: DO NOT INSERT A FINAL "/" (because of HttpService.registerResources() constraints (see javadoc)
	public static final String app_icon_path = app_resources_folder+"/icon_76.png";
	
	private static final String app_data_folder = "/";
	private static final String app_metadata_file = "app-metadata.json";
	
	
	
	/*
	 * ___EXTERNAL_DEPENDENCIES__________________________________________________________________
	 */
	
	private final BundleContext bundleContext;
	private HttpService httpService;
	private AppRegistryService appRegistry;
	private PhysicalEnvironmentModelManagerImpl physicalEnvironmentModelProxyManager;
	private HSQLDatabaseService hsqlDatabaseService;
	private ConfigurationAdmin configurationAdminService;
	
	
	/*
	 * ___INTERNAL_CLASSES_&_MEMBERS_______________________________________________________________
	 */
	
	private boolean applicationDidValidate;
	private boolean applicationDidStart;
	private final long bundleUID;
	private final String appUniqueId;
	private final String appPackageName;
	private final String appName;
	private final List<String> webServiceURLs;
	private final int executorCorePoolSize;
	private final Collection<EventGateImpl> appCustomEventGates;
	private HttpSecurityHandler httpSecurityHandler;
	private final HttpContext appServletHttpContext;
	private final boolean isRootApp;
	
	private ScheduledExecutorService executorService;
	private DatabaseProxyImpl databaseProxy;
	private String webUserInterfaceURL;
	//private boolean isConfiguredForEmbeddedSystem;
	
	final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractApplication.class);

	// ---------------------------------------------------------------------------

	private class AppStaticResourcesContext implements HttpContext
	{
		@Override
		public String getMimeType(String name) 
		{
			if(name.endsWith("/")) {
				return "text/html"; // Default Mime type is HTML
			}
			
			return null; // Let HttpService choose itself the MimeType
		}

		@Override
		public URL getResource(String name) 
		{
			URL url;
			
			if(name.endsWith("/")) {
				url = bundleContext.getBundle().getResource("/" + name + "index.html");
			}
			else {
				url = bundleContext.getBundle().getResource("/" + name);
			}
			
			logger.trace("URL for resources {} is {}.",name, url);
			
			return url;
		}

		@Override
		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException 
		{
			if(httpSecurityHandler == null) {
				return true; // Ok, the request could be processed.
			}
			
			return httpSecurityHandler.handleSecurity(request, response);
		}
	}
	
	private class AppServletHttpContext implements HttpContext
	{

		@Override
		public String getMimeType(String name) 
		{
			return null; // Let HttpService choose itself the MimeType
		}

		@Override
		public URL getResource(String name) 
		{
			return null;
		}

		@Override
		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException 
		{
			if(httpSecurityHandler == null) {
				return true; // Ok, the request could be processed.
			}
			
			return httpSecurityHandler.handleSecurity(request, response);
		}
	}

	
	/*
	 * ___PUBLIC_METHODS___________________________________________________________________________
	 */

	public abstract void startApplication() throws Exception;
	public abstract void stopApplication();
	
	// ---------------------------------------------------------------------------
	
	public synchronized void validate(ConfigurationAdmin configurationAdminService, HttpService httpService, HSQLDatabaseService hsqlDatabaseService)
	{
		logger.trace("{} has just @Validate", appName);
		
		try 
		{
			this.configurationAdminService = configurationAdminService;
			this.httpService = httpService;
			this.hsqlDatabaseService = hsqlDatabaseService;
			
			applicationDidValidate = true;
			startApplicationIfFeasible();
		}
		catch(Exception e)
		{
			logger.error("###### ERROR WHILE STARTING AN APPLICATION #######");
			logger.error("###### During @Validate operation: ", e);
			logger.error("##################################################");
		}
	}
	
	// ---------------------------------------------------------------------------
	
	public synchronized void invalidate()
	{
		logger.trace("{} has just @Invalidate", appName);
		
		stopApplicationIfNecessary();
		applicationDidValidate = false;
	}
		
	// ---------------------------------------------------------------------------

	public synchronized void bindPhysicalEnvironmentModelService(PhysicalEnvironmentModelService model)
	{
		boolean b = physicalEnvironmentModelProxyManager.bind(model);
		
		if(b) {
			logger.debug("{} was required by & binded to {}", model.getUID(), appName);
		}
		
		if(physicalEnvironmentModelProxyManager.allRequiredDependenciesResolved())
		{
			try {
				startApplicationIfFeasible();
			}
			catch(Exception e)
			{
				logger.error("###### ERROR WHILE STARTING AN APPLICATION #######");
				logger.error("###### During @bindPhysicalEnvironmentModelService operation: ", e);
				logger.error("##################################################");
			}
		}
	}

	// ---------------------------------------------------------------------------

	public synchronized void unbindPhysicalEnvironmentModelService(PhysicalEnvironmentModelService model)
	{
		
		boolean b = physicalEnvironmentModelProxyManager.unbind(model);
		
		if(b) {
			logger.debug("{} was unbinded from {}", model.getUID(), appName);
		}
		
		if(!physicalEnvironmentModelProxyManager.allRequiredDependenciesResolved())
		{
			stopApplicationIfNecessary();
		}
	}
	
	// ---------------------------------------------------------------------------
	
	public synchronized void bindAppRegistry(AppRegistryService appRegistry)
	{
		this.appRegistry = appRegistry;
		
		logger.trace("App registry was binded");
		
		if(applicationDidStart)
		{
			logger.debug("Annoucing starting of application contained in bundle {}", bundleContext.getBundle().getBundleId());
			this.appRegistry.appDidStart(bundleContext.getBundle().getBundleId());
		}
	}
	
	// ---------------------------------------------------------------------------
	
	public synchronized void unbindAppRegistry()
	{
		this.appRegistry = null;
	}

	// ---------------------------------------------------------------------------
	
	/*
	 * ___PROTECTED_METHODS___________________________________________________________________________
	 */
	protected AbstractApplication(int threadCorePoolSize, BundleContext bundleContext, String[] supportedPhysicalEnvironmentModelUIDs)
	{
		executorCorePoolSize = threadCorePoolSize + 1; // +1 because one will be used by the event gate to PEMs
		appServletHttpContext = new AppServletHttpContext();
		applicationDidValidate = false;
		applicationDidStart = false;
		appRegistry = null;
		httpService = null;
		hsqlDatabaseService = null;
		databaseProxy = null;
		httpSecurityHandler = null;
		
		// Build the APP unique identifier
		String packageName = this.getClass().getPackage().getName();
		appPackageName = packageName.substring(0, packageName.lastIndexOf("."));
		
		physicalEnvironmentModelProxyManager = new PhysicalEnvironmentModelManagerImpl(supportedPhysicalEnvironmentModelUIDs, appPackageName+"_ll");
		appCustomEventGates = new ArrayList<EventGateImpl>();
		
		webServiceURLs = new ArrayList<String>();
		
		this.bundleContext = bundleContext;
		
		bundleUID = bundleContext.getBundle().getBundleId();
		
		String appUniqueId = null;
		String appName = null;
		
		try 
		{
			JSONObject appMetaData = BundleResourceUtil.getResourceJSONContent(bundleContext.getBundle().getEntry(app_data_folder+app_metadata_file));
			appUniqueId = appMetaData.getString("uid");
			appName = appMetaData.getString("name");
		}
		catch (IOException e) {
			logger.error("Application Metadata were NOT found. Check JAR contains {} file.\n", app_data_folder+app_metadata_file, e);
		} 
		catch (JSONException e) {
			logger.error("Application Metadata were NOT found. Check {} file content.\n", app_data_folder+app_metadata_file, e);
		}
		
		this.appName = appName;
		this.appUniqueId = appUniqueId;
		
		String rootAppProperty = ConfigurationProperties.getInstance().getRootApp();
		boolean rootApp = false;
		if(rootAppProperty != null)
		{
			if(rootAppProperty.equals(appUniqueId)) {
				rootApp = true;
			}
		}
		
		isRootApp = rootApp;
	}
	
	// ---------------------------------------------------------------------------
	
	protected void setHttpSecurityHandler(HttpSecurityHandler httpSecurityHandler)
	{
		this.httpSecurityHandler = httpSecurityHandler;
	}
	
	// ---------------------------------------------------------------------------
	
	protected boolean registerServlet(String baseServiceUrl, HttpServlet servlet)
	{
		String url = (webUserInterfaceURL.equals("/"))?"/api":(webUserInterfaceURL+"/api");
		
		if(baseServiceUrl != null && !baseServiceUrl.isEmpty()) {
			url += "/" + baseServiceUrl;
		}
		
		try
		{
			httpService.registerServlet(url, servlet, null, appServletHttpContext);
			webServiceURLs.add(url);
			logger.debug("registration of {}", url);
			return true;
		}
		catch (ServletException e) 
		{
			logger.error("{} application CANNOT register {} servlet: ServletException occured.\n", appName, url, e);
		} 
		catch (NamespaceException e) 
		{
			logger.error("{} application CANNOT register {} servlet: NamespaceException occured.\n", appName, url, e);
		}
		
		return false;
	}
	
	// ---------------------------------------------------------------------------
	

	/**
	 * @param resourceUrl The resource relative URL. This URL will be prefixed by the base URL of the application
	 * @param pathToResource The path to the resource to expose. This path is relative to the bundle root foldeR.
	 * @param httpContext (Optional). If null, a default context is used. If NOT null, httpContext MUST point onto
	 * a valid and well configured HttpContext object.
	 * @return
	 */
	protected boolean registerResources(String resourceUrl, String pathToResource, HttpContext httpContext)
	{
		if(httpContext == null) {
			httpContext =  new AppStaticResourcesContext();
		}
		
		try 
		{
			httpService.registerResources(getWebUserInterfaceURL()+resourceUrl, "/"+pathToResource, httpContext);
			logger.debug("registration of {} (link to /{})", getWebUserInterfaceURL()+resourceUrl, pathToResource);
			return true;
		} 
		catch (NamespaceException e) 
		{
			logger.error("Application web resources CANNOT be registered. Check the {} folder existence.", pathToResource, e);
		}
		
		return false;
	}
	
	// ---------------------------------------------------------------------------

	protected PhysicalEnvironmentModelService getPhysicalEnvironmentModel(String modelUID)
	{
		return physicalEnvironmentModelProxyManager.getModel(modelUID);
	}
	
	// ---------------------------------------------------------------------------

	/*protected Map<String, PhysicalEnvironmentModelProxy> getPhysicalEnvironmentModelProxies()
	{
		return physicalEnvironmentModelProxyManager.getAllModelProxies();
	}*/
	
	protected PhysicalEnvironmentModelManager getPhysicalEnvironmentModelManager()
	{
		return physicalEnvironmentModelProxyManager;
	}
	
	// ---------------------------------------------------------------------------
	
	/*protected String getAppIconUrl()
	{
		return "/"+appUniqueId+"/www/icon_76.png";
	}*/
	
	// ---------------------------------------------------------------------------

	protected DatabaseProxy getDatabaseConnection() throws SQLException
	{
		if(databaseProxy == null || databaseProxy.isClosed() || !databaseProxy.isValid()) 
		{	
			HSQLDatabaseService.Type type = HSQLDatabaseService.Type.APP;
			
			if(this instanceof SystemApp) {
				type = HSQLDatabaseService.Type.SYSTEM;
			}
			
			databaseProxy = new DatabaseProxyImpl(hsqlDatabaseService.create(appPackageName, type), ConfigurationProperties.getInstance().getEmbedded()); 
		}
		
		return databaseProxy;
	}
	
	// ---------------------------------------------------------------------------

	protected EventGate getPhysicalEnvironmentModelsEventGate()
	{
		return physicalEnvironmentModelProxyManager.getEventGate();
	}	
	
	// ---------------------------------------------------------------------------
	
	public ScheduledExecutorService getExecutorService()
	{
		return executorService;
	}
	
	// ---------------------------------------------------------------------------
	
	protected BundleContext getBundleContext()
	{
		return bundleContext;
	}
	
	// ---------------------------------------------------------------------------
	
	protected String getWebUserInterfaceURL()
	{
		return webUserInterfaceURL.equals("/")?"/":webUserInterfaceURL.concat("/");
	}
	
	protected ConfigurationAdmin getConfigurationAdminService()
	{
		return configurationAdminService;
	}
	
	// ---------------------------------------------------------------------------
	
	public boolean isEmbbeded()
	{
		return ConfigurationProperties.getInstance().getEmbedded();
	}
	
	public boolean isRootApp()
	{
		return isRootApp;
	}
	
	public int getHttpServicePort()
	{
		return ConfigurationProperties.getInstance().getHttServicePort();
	}
		
	// ---------------------------------------------------------------------------
	
	protected EventGate createNewEventGate()
	{
		EventGateImpl eg = null;
		
		//if(LC.debugEvent) {
			eg = new EventGateImpl(appPackageName+"_custom_"+appCustomEventGates.size());
		/*}
		else {
			eg = new EventGateImpl();
		}*/
		
		appCustomEventGates.add(eg);
		return eg;
	}
	
// ---------------------------------------------------------------------------
	
	protected HttpEventGate createNewHttpEventGate(String eventGateName, EventInstanciator eventInstanciator, HttpEventGate.Mode mode) throws NamespaceException
	{
		HttpEventGateImpl eg = null;
		
		eg = new HttpEventGateImpl(httpService, eventInstanciator, eventGateName, mode);
		executorService.execute(eg.getEventDelivererTask());
		
		appCustomEventGates.add(eg);
		return eg;
	}
	
	/*
	 * ___PACKAGE_METHODS___________________________________________________________________________
	 */
	String getAppUniqueID()
	{
		return appUniqueId;
	}
	
	/*
	 * ___PRIVATE_METHODS___________________________________________________________________________
	 */
	
	private void startApplicationIfFeasible() throws Exception
	{
		if(applicationDidValidate 
				&& physicalEnvironmentModelProxyManager.allRequiredDependenciesResolved() 
				&& !applicationDidStart)
		{
			applicationDidStart = true;
			
			executorService = Executors.newScheduledThreadPool(executorCorePoolSize);
			
			webUserInterfaceURL = "/"+appUniqueId;
			
			if(isRootApp) {
				webUserInterfaceURL = "/";
			}
			
			AppStaticResourcesContext ctxt = new AppStaticResourcesContext();
			try 
			{
				httpService.registerResources(webUserInterfaceURL, "/"+app_resources_folder, ctxt);
				logger.debug("registration of {}", webUserInterfaceURL);
			} 
			catch (NamespaceException e) 
			{
				logger.error("Application web resources CANNOT be registered. Check the {} folder existence.", app_resources_folder, e);
			}
			
			executorService.execute(physicalEnvironmentModelProxyManager.getEventGate().getEventDelivererTask());
			for(EventGateImpl eg : appCustomEventGates)
			{
				executorService.execute(eg.getEventDelivererTask());
			}
			
			startApplication();
			if(appRegistry != null) {
				appRegistry.appDidStart(bundleUID);
			}
			else {
				logger.debug("App registry not yet available, application start up will be annouce later.");
			}
		}
		else
		{
			if(!applicationDidValidate) {
				logger.debug("Waiting applicationDidValidate...");
			}
			
			if(!physicalEnvironmentModelProxyManager.allRequiredDependenciesResolved()) {
				logger.debug("Waiting physicalEnvironmentModelDependencies Ok...");
			}
			
			if(applicationDidStart) {
				logger.debug("Application already started...");
			}	
		}
	}
	
	// ---------------------------------------------------------------------------
	
	private void stopApplicationIfNecessary()
	{
		if(applicationDidStart)
		{
			applicationDidStart = false;
			stopApplication();
			
			physicalEnvironmentModelProxyManager.unlinkAll();
			for(EventGateImpl eg : appCustomEventGates)
			{
				eg.unlinkAll();
				if(eg instanceof HttpEventGateImpl) 
				{
					((HttpEventGateImpl) eg).terminate();
				}
			}
			
			Iterator<String> url_it = webServiceURLs.iterator();
			while(url_it.hasNext())
			{
				String url = url_it.next();
				httpService.unregister(url);
				logger.debug("UNregistration of {}", url);
			}
			
			httpService.unregister(webUserInterfaceURL);
			logger.debug("UNregistration of {}", webUserInterfaceURL);
			
			if(executorService != null)
			{
				logger.debug("Waiting for application threads to terminate...");
				
				executorService.shutdownNow();
				try 
				{
					executorService.awaitTermination(5, TimeUnit.SECONDS);
					executorService = null;
					
					logger.debug("Application threads have terminated...");
				} 
				catch (InterruptedException e) 
				{
					logger.error("Application termination was interrupted while stopping application threads", e);
				}
			}
			
			if(databaseProxy != null) {
				databaseProxy.closeDatabaseConnection();
			}
			
			webServiceURLs.clear();
			
			if(appRegistry != null) {
				appRegistry.appDidStopped(bundleUID);
			}
		}
	}
}
