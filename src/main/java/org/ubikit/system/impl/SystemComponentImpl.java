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

package org.ubikit.system.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.impl.DatabaseProxyImpl;
import org.ubikit.service.AppRegistryService;
import org.ubikit.service.HSQLDatabaseService;
import org.ubikit.service.PemRegistryService;
import org.ubikit.system.ExtensionManagerService;
import org.ubikit.system.SystemInspectionService;
import org.ubikit.system.ExtenstionRegistry;
import org.ubikit.system.SystemProperties;



@Component
@Provides
@Instantiate
public final class SystemComponentImpl implements SystemInspectionService, AppRegistryService,
	PemRegistryService, ExtensionManagerService
{
	private static final String ext_web_folder = "www";
	private static final String ext_web_URL = "/ext";

	@Requires
	private HSQLDatabaseService hsqlDatabaseService = null;

	@Requires
	private HttpService httpService = null;

	private final BundleContext bundleContext;

	private DatabaseProxyImpl databaseProxy;
	private SystemProperties properties;
	private AppRegistryImpl appRegistry;
	private PemRegistryImpl pemRegistry;
	private boolean isConfiguredForEmbeddedSystem;
	private final String extWebFullPath;

	final Logger logger = LoggerFactory.getLogger(SystemComponentImpl.class);

	private class ExternalStaticResourcesContext implements HttpContext
	{
		@Override
		public String getMimeType(String name)
		{
			if (name.endsWith("/"))
			{
				return "text/html"; // Default Mime type is HTML
			}

			return null; // Let HttpService choose itself the MimeType
		}






		@Override
		public URL getResource(String name)
		{
			URL url = null;

			try
			{
				url = new URL(extWebFullPath
					+ name);
				if (logger.isDebugEnabled())
				{
					logger.debug("URL for resources {} is {}", name, url);
				}
			}
			catch (MalformedURLException e)
			{
				logger.error("Cannot access the external web ressource '{}'", name, e);
			}

			return url;
		}






		@Override
		public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1)
			throws IOException
		{
			return true; // Ok, the request could be processed.
		}
	}






	public SystemComponentImpl(BundleContext bundleContext)
	{
		this.bundleContext = bundleContext;
		databaseProxy = null;
		properties = null;
		appRegistry = null;
		pemRegistry = null;
		isConfiguredForEmbeddedSystem = Boolean.parseBoolean(bundleContext
			.getProperty("org.ubikit.embedded"));
		extWebFullPath = "file://"
			+ System.getProperty("user.dir") + "/";

		if (logger.isDebugEnabled())
		{
			logger.debug("Component instance was created");
		}
	}






	@Validate
	public synchronized void validate()
	{
		ExternalStaticResourcesContext extCtxt = new ExternalStaticResourcesContext();
		try
		{
			httpService.registerResources(ext_web_URL, ext_web_folder, extCtxt);
			if (logger.isDebugEnabled())
			{
				logger.debug("registration of {}", ext_web_URL);
			}
		}
		catch (NamespaceException e)
		{
			logger.error(
				"External web resources CANNOT be registered. Check the {} folder existence.",
				ext_web_folder,
				e);
		}

		try
		{
			properties = SystemPropertiesImpl.getInstance(bundleContext);
			databaseProxy = new DatabaseProxyImpl(hsqlDatabaseService.create(
				"ubikit",
				HSQLDatabaseService.Type.SYSTEM), isConfiguredForEmbeddedSystem);
			appRegistry = AppRegistryImpl.getInstance(bundleContext, databaseProxy);
			pemRegistry = PemRegistryImpl.getInstance(bundleContext, databaseProxy);
		}
		catch (SQLException e)
		{
			logger.error(
				"Cannot create a connection to the ubikit database. Exception stack below.",
				e);
		}

		if (logger.isDebugEnabled())
		{
			logger.debug("Component instance did validated");
		}

		logger.info("Ubikit has started\n--------------------------------------------\n"
			+ "-- Ubikit version " + properties.getSystemVersion() + "\n" + "-- Ubikit build "
			+ properties.getSystemBuildID() + "\n" /*+ "-- Ubikit build date: "
			+ properties.getSystemBuildDate() + "\n"*/
			+ "--------------------------------------------");
	}






	@Invalidate
	public synchronized void invalidate()
	{
		databaseProxy.closeDatabaseConnection();
		databaseProxy = null;
		AppRegistryImpl.clearInstance();
		PemRegistryImpl.clearInstance();
		SystemPropertiesImpl.clearInstance();
	}






	// -----------------------------------------------------------------------------------
	// -- SystemInspectionService Methods
	// -----------------------------------------------------------------------------------

	@Override
	public SystemProperties getSystemProperties()
	{
		return properties;
	}






	@Override
	public ExtenstionRegistry getAppRegistry()
	{
		return appRegistry;
	}






	@Override
	public ExtenstionRegistry getPemRegistry()
	{
		return pemRegistry;
	}






	// -----------------------------------------------------------------------------------
	// -- AppRegistryService Methods
	// -----------------------------------------------------------------------------------

	@Override
	public void appDidStart(long bundleID)
	{
		appRegistry.appDidStart(bundleID);
	}






	@Override
	public void appDidStopped(long bundleID)
	{
		appRegistry.appDidStopped(bundleID);
	}






	// -----------------------------------------------------------------------------------
	// -- PemRegistryService Methods
	// -----------------------------------------------------------------------------------

	@Override
	public void pemDidStart(long bundleID)
	{
		pemRegistry.pemDidStart(bundleID);
	}






	@Override
	public void pemDidStop(long bundleID)
	{
		pemRegistry.pemDidStop(bundleID);
	}






	// -----------------------------------------------------------------------------------
	// -- ExtensionManagerService Methods
	// -----------------------------------------------------------------------------------

	@Override
	public boolean start(ExtensionType type, String extensionUID)
	{
		switch (type)
		{
			case APP:
				return appRegistry.start(extensionUID);
			case PEM:
				return pemRegistry.start(extensionUID);
		}

		return false;
	}






	@Override
	public boolean stop(ExtensionType type, String extensionUID)
	{
		switch (type)
		{
			case APP:
				return appRegistry.stop(extensionUID);
			case PEM:
				return pemRegistry.stop(extensionUID);
		}

		return false;
	}






	@Override
	public boolean uninstall(ExtensionType type, String extensionUID)
	{
		switch (type)
		{
			case APP:
				return appRegistry.uninstall(extensionUID);
			case PEM:
				return pemRegistry.uninstall(extensionUID);
		}

		return false;
	}






	@Override
	public InstallationStatus install(	ExtensionType type,
										String extensionURL,
										String distributorName,
										String distributorURL)
	{
		switch (type)
		{
			case APP:
				return appRegistry.install(extensionURL, distributorName, distributorURL);
			case PEM:
				return pemRegistry.install(extensionURL, distributorName, distributorURL);
		}

		return InstallationStatus.FAILED;
	}
}
