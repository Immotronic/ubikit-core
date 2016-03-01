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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.DatabaseProxy;
import org.ubikit.system.BundleManager;
import org.ubikit.system.ExtensionManager;
import org.ubikit.system.ExtensionManagerService.ExtensionType;
import org.ubikit.system.ExtensionManagerService.InstallationStatus;
import org.ubikit.system.ExtensionOrigin;
import org.ubikit.system.ExtensionProperties;
import org.ubikit.system.ExtenstionRegistry;
import org.ubikit.system.ExtensionExecutionProperties;
import org.ubikit.tools.BundleResourceUtil;


public class ExtensionRegistryImpl implements ExtenstionRegistry, ExtensionManager, BundleListener
{
	private static final String TABLE_STRUCTURE = "CREATE TABLE IF NOT EXISTS exts ("
			+ "bundleUID BIGINT, " + "type VARCHAR(3) NOT NULL," + "distributorName VARCHAR(100), "
			+ "distributorURL VARCHAR(200), " + "PRIMARY KEY (bundleUID));";

	private static final String INSERT_QUERY = "INSERT INTO exts "
			+ "(bundleUID, type, distributorName, distributorURL) " + "VALUES (?, ?, ?, ?);";

	private static final String REMOVE_QUERY = "DELETE FROM exts WHERE bundleUID = ?;";

	private static final String QUERY_ALL_ITEMS = "SELECT * FROM exts WHERE type = ?;";

	private final BundleContext bundleContext;

	private final String extensionType;
	private final String metadataFilename;

	private final DatabaseProxy databaseProxy;
	private final PreparedStatement getExtensionList;
	private final PreparedStatement insertNewExtension;
	private final PreparedStatement removeExtension;

	private final Map<Long, ExtensionProperties> exts;
	private final Map<Long, ExtensionExecutionPropertiesImpl> extExecutionProperties;
	private final Map<String, Long> extBundleUIDs;

	private final BundleManager bundleManager;
	private final Semaphore extensionManagementSemaphore;
	private final BlockingQueue<Boolean> extensionOperationAccomplishment;

	final Logger logger = LoggerFactory.getLogger(ExtensionRegistryImpl.class);



	public ExtensionRegistryImpl(
			ExtensionType extensionType,
			String metadataFilename,
			BundleContext bundleContext,
			DatabaseProxy databaseProxy)
	{
		bundleContext.addBundleListener(this);

		this.extensionType = extensionType.name();
		this.metadataFilename = metadataFilename;
		this.bundleContext = bundleContext;
		this.databaseProxy = databaseProxy;

		bundleManager = BundleManagerImpl.getInstance(bundleContext);

		exts = Collections.synchronizedMap(new HashMap<Long, ExtensionProperties>());

		extExecutionProperties = Collections
				.synchronizedMap(new HashMap<Long, ExtensionExecutionPropertiesImpl>());

		extBundleUIDs = Collections.synchronizedMap(new HashMap<String, Long>());

		databaseProxy.executeUpdate(TABLE_STRUCTURE);
		getExtensionList = databaseProxy.getPreparedStatement(QUERY_ALL_ITEMS);
		insertNewExtension = databaseProxy.getPreparedStatement(INSERT_QUERY);
		removeExtension = databaseProxy.getPreparedStatement(REMOVE_QUERY);

		extensionManagementSemaphore = new Semaphore(1);
		extensionOperationAccomplishment = new ArrayBlockingQueue<Boolean>(1);

		loadExtCollections();
	}


	// -----------------------------------------------------------------------------------
	// -- ExtenstionRegistry Methods
	// -----------------------------------------------------------------------------------



	@Override
	public Collection<ExtensionProperties> getExtensionCollection()
	{
		Collection<ExtensionProperties> res = new ArrayList<ExtensionProperties>();

		synchronized (exts)
		{
			for (ExtensionProperties p : exts.values())
			{
				res.add(p);
			}
		}

		return res;

	}



	@Override
	public Map<String, ExtensionExecutionProperties> getExtensionStatusList()
	{
		Map<String, ExtensionExecutionProperties> res = new HashMap<String, ExtensionExecutionProperties>();

		synchronized (extExecutionProperties)
		{
			for (Long bundleUID : extExecutionProperties.keySet())
			{
				ExtensionExecutionProperties eep = extExecutionProperties.get(bundleUID);
				res.put(eep.getExtensionUID(), eep);
			}
		}

		return res;
	}



	@Override
	public ExtensionProperties getExtensionProperties(String extensionUID)
	{
		return exts.get(extBundleUIDs.get(extensionUID));
	}



	@Override
	public ExtensionExecutionProperties getExtensionExecutionProperties(String extensionUID)
	{
		return extExecutionProperties.get(extBundleUIDs.get(extensionUID));
	}


	// -----------------------------------------------------------------------------------
	// -- ExtensionManager Methods
	// -----------------------------------------------------------------------------------



	@Override
	public boolean start(String extensionUID)
	{
		Long bundleUID = extBundleUIDs.get(extensionUID);

		if (bundleUID != null) // before eep was tested against null
		{
			try
			{
				extensionManagementSemaphore.acquire();

				boolean res = bundleManager.start(/* eep.getExtensionBundleUID() */bundleUID
						.longValue());

				// Start token has to be taken ONLY if the operation is ongoing
				if (res)
				{
					extensionOperationAccomplishment.take();
				}

				extensionManagementSemaphore.release();
				return res;
			}
			catch (InterruptedException e)
			{
				extensionManagementSemaphore.release();
				return false;
			}
		}
		else
		{
			logger.error("Try to start an unknown app: {}", extensionUID);
		}

		return false;
	}



	@Override
	public boolean stop(String extensionUID)
	{
		ExtensionExecutionPropertiesImpl eep = extExecutionProperties.get(extBundleUIDs
				                                                                  .get(extensionUID));

		if (eep != null)
		{
			try
			{
				extensionManagementSemaphore.acquire();

				boolean res = bundleManager.stop(eep.getExtensionBundleUID());

				// Stop token has to be taken ONLY if the operation is ongoing
				if (res)
				{
					extensionOperationAccomplishment.take();
				}

				extensionManagementSemaphore.release();
				return res;
			}
			catch (InterruptedException e)
			{
				extensionManagementSemaphore.release();
				return false;
			}
		}
		else
		{
			logger.error("Try to stop an unknown ext: {}", extensionUID);
		}

		return false;
	}



	@Override
	public boolean uninstall(String extensionUID)
	{
		try
		{
			extensionManagementSemaphore.acquire();

			long bundleUID = extBundleUIDs.get(extensionUID);

			ExtensionExecutionPropertiesImpl eep = extExecutionProperties.get(bundleUID);

			if (eep != null)
			{
				ExtensionProperties ep = exts.get(bundleUID);
				boolean res = false;
				if (ep.getOrigin() == ExtensionOrigin.DOWNLOADED)
				{
					if (logger.isDebugEnabled())
					{
						logger.debug("Starting Uninstallation, sem was acquired.");
					}
					/* boolean */
					res = bundleManager.uninstall(bundleUID);

					if (logger.isDebugEnabled())
					{
						logger.debug("Uninstalled, res={}, waiting uninstall token...", res);
					}
				}
				else
				// else, ExtensionOrigin is PRE-INSTALLED
				{
					File f = new File(bundleContext.getBundle(bundleUID).getLocation().substring(5));
					f.delete();
					res = true;
				}

				if (res)
				{
					// Uninstall token has to be taken ONLY if the operation is ongoing
					extensionOperationAccomplishment.take();

					if (logger.isDebugEnabled())
					{
						logger.debug("Uninstall token was taken.");
						logger.debug("Removing extension from DB");
					}

					if (removeExtensionFromDatabase(bundleUID))
					{
						this.removingExtensionFromRegistry(bundleUID);

						if (logger.isDebugEnabled())
						{
							logger.debug("Releasing semaphore & returning true");
						}
						extensionManagementSemaphore.release();

						return true;
					}
				}
				else
				{
					logger.error("An error occured while uninstalling {} extention", extensionUID);
				}
			}
			else
			{
				logger.error("Try to unsinstall an unknown extention: {}", extensionUID);
			}
		}
		catch (InterruptedException e)
		{
			logger.warn("Uninstall procedure for extention {} was interrupted", extensionUID, e);
		}

		if (logger.isDebugEnabled())
		{
			logger.debug("Releasing semaphore & returning false");
		}
		extensionManagementSemaphore.release();
		return false;
	}



	@Override
	public InstallationStatus install(
			String extensionURL,
			String distributorName,
			String distributorURL)
	{
		Bundle bundle = null;
		long extensionBundleID = -1;
		ExtensionProperties ep = null;

		try
		{
			extensionManagementSemaphore.acquire();

			if (logger.isDebugEnabled())
			{
				logger.debug("Installing {}...", extensionURL);
			}

			bundle = bundleContext.installBundle(extensionURL);
			extensionBundleID = bundle.getBundleId();

			if (logger.isDebugEnabled())
			{
				logger.debug("Installed as bundle {}", extensionBundleID);
			}

			insertNewExtension.setLong(1, extensionBundleID);
			insertNewExtension.setString(2, extensionType);
			insertNewExtension.setString(3, distributorName);
			insertNewExtension.setString(4, distributorURL);

			if (databaseProxy.executePreparedUpdate(insertNewExtension) < 0)
			{
				logger.error("Failed to insert a new {} extension from {} in the ubikit database."
						             + " Sound like a bug.", extensionType, extensionURL);
			}

			ep = createAppProperties(extensionBundleID, distributorName, distributorURL);

			// Insert extension properties in the extensions and extension bundle UID maps
			exts.put(extensionBundleID, ep);
			extBundleUIDs.put(ep.getUID(), extensionBundleID);

			if (logger.isDebugEnabled())
			{
				logger.debug("Starting bundle {}...", extensionBundleID);
			}
			bundle.start();

			extensionOperationAccomplishment.take();

			if (logger.isDebugEnabled())
			{
				logger.debug("Start token was taken");
			}

			extensionManagementSemaphore.release();
			return InstallationStatus.OK;
		}
		catch (BundleException e)
		{
			logger.error("failed to install an extension from {}", extensionURL);
			if (extensionBundleID != -1)
			{
				exts.remove(extensionBundleID);

				if (ep != null)
				{
					extBundleUIDs.remove(ep.getUID());
				}

				try
				{
					removeExtension.setLong(1, extensionBundleID);
					if (databaseProxy.executePreparedUpdate(removeExtension) < 0)
					{
						logger.error("Failed to remove the buggy {} extension from {} in the "
								             + "ubikit database. Sound like a bug.", extensionType, extensionURL);
					}
				}
				catch (SQLException e1)
				{
					logger.error(
							"failed to remove from database the buggy extension from {}",
							extensionURL,
							e1);
				}
			}

			if (bundle != null)
			{
				try
				{
					bundle.uninstall();
				}
				catch (BundleException e1)
				{
					logger.error("failed to uninstall the buggy extension from {}"
							             + extensionURL, e1);
				}
			}
		}
		catch (SQLException e)
		{
			logger.error(
					"While inserting a new {} extension from {} in the ubikit database",
					extensionType,
					extensionURL,
					e);
		}
		catch (InterruptedException e)
		{
		}

		extensionManagementSemaphore.release();
		return InstallationStatus.FAILED;
	}



	@Override
	public String getExtensionResourceTextualContent(String extensionUID, String resourcePath)
	throws IOException
	{
		URL resourceURL = getExtensionResourceURL(extensionUID, resourcePath);
		if (resourceURL != null)
		{
			return BundleResourceUtil.getResourceTextualContent(resourceURL);
		}

		return null;
	}



	@Override
	public byte[] getExtensionResourceBinaryContent(String extensionUID, String resourcePath)
	throws IOException
	{
		URL resourceURL = getExtensionResourceURL(extensionUID, resourcePath);
		if (resourceURL != null)
		{
			return BundleResourceUtil.getResourceBinaryContent(resourceURL);
		}

		return null;
	}



	@Override
	public JSONObject getExtensionResourceJSONContent(String extensionUID, String resourcePath)
	throws IOException, JSONException
	{
		URL resourceURL = getExtensionResourceURL(extensionUID, resourcePath);
		if (resourceURL != null)
		{
			return BundleResourceUtil.getResourceJSONContent(resourceURL);
		}

		return null;
	}



	@Override
	public URL getExtensionResourceURL(String extensionUID, String resourcePath)
	{
		Long bundleUID = extBundleUIDs.get(extensionUID);
		if (bundleUID != null)
		{
			return bundleContext.getBundle(bundleUID.longValue()).getEntry(resourcePath);
		}

		return null;
	}



	@Override
	public void bundleChanged(BundleEvent event)
	{
		if (event.getType() == BundleEvent.UNINSTALLED)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("UNINSTALLED BundleChanged event for bundle {}", event
						.getBundle()
						.getBundleId());
			}
			if (extensionManagementSemaphore.availablePermits() == 0)
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("Extension was uninstalled from the UI");
				}
				try
				{
					if (logger.isDebugEnabled())
					{
						logger.debug("Delivering a extension operation accomplishment token");
					}
					extensionOperationAccomplishment.put(new Boolean(true));
				}
				catch (InterruptedException e)
				{
					logger.error("While delivering an extension operation accomplishment token", e);
				}
			}
			else
			{
				if (logger.isDebugEnabled())
				{
					logger
							.debug("Extension was uninstalled but no under human control from the UI");
				}

				// The bundle reference is removed from the database & registry
				long bundleUID = event.getBundle().getBundleId();
				if (removeExtensionFromDatabase(bundleUID))
				{
					this.removingExtensionFromRegistry(bundleUID);
				}
			}
		}
	}


	// -----------------------------------------------------------------------------------
	// -- Protected Methods
	// -----------------------------------------------------------------------------------



	protected void extensionDidStart(long extensionBundleID)
	{
		// Getting properties for the extension having the given UID.
		ExtensionProperties ep = exts.get(extensionBundleID);

		// If no app having the given UID already exists in the extensions map, this app
		// was pre-installed and have to be known as installed. (e.g. this extension does
		// NOT have been downloaded from an extension distributor)
		if (ep == null)
		{
			ep = createAppProperties(extensionBundleID, null, null);

			try
			{
				insertNewExtension.setLong(1, extensionBundleID);
				insertNewExtension.setString(2, extensionType);
				insertNewExtension.setString(3, null);
				insertNewExtension.setString(4, null);

				if (databaseProxy.executePreparedUpdate(insertNewExtension) < 0)
				{
					logger.error(
							"Failed to insert a new {} extension in the ubikit database. "
									+ "Is this extension ({}) already stored in DB ? Sound like a bug.",
							extensionType,
							ep.getUID());
				}
			}
			catch (SQLException e)
			{
				logger.error(
						"While inserting a new {} extension in the ubikit database ({}).",
						extensionType,
						ep.getUID(),
						e);
				return;
			}

			// Insert extension properties in the extensions and extension bundle UID maps
			exts.put(extensionBundleID, ep);
			extBundleUIDs.put(ep.getUID(), extensionBundleID);
		}

		// Getting execution properties for this extension
		ExtensionExecutionPropertiesImpl eep = extExecutionProperties.get(extensionBundleID);

		if (eep == null)
		{
			// If no such properties exists, the system is booting. Create those properties.
			eep = new ExtensionExecutionPropertiesImpl(ep.getUID(), extensionBundleID);
			eep.extensionDidStart();
			extExecutionProperties.put(extensionBundleID, eep);
		}
		else
		{
			// Update those properties.
			eep.extensionDidStart();
		}

		if (extensionManagementSemaphore.availablePermits() == 0)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Extension was started from the UI");
			}
			try
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("Delivering a extension operation accomplishment token");
				}
				extensionOperationAccomplishment.put(new Boolean(true));
			}
			catch (InterruptedException e)
			{
				logger.error("While delivering an extension operation accomplishment token", e);
			}
		}
		else
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Extension was started but no under human control from the UI");
			}
		}
	}



	protected void extensionDidStop(long extensionBundleID)
	{
		ExtensionExecutionPropertiesImpl eep = extExecutionProperties.get(extensionBundleID);

		if (eep != null)
		{
			eep.extensionDidStop();
		}
		else
		{
			logger.error("received an 'extensionDidStopped' message but no extension with the "
					             + "given bundle UID is referenced ({} {}).", extensionType, extensionBundleID);
		}

		if (extensionManagementSemaphore.availablePermits() == 0)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Extension was stopped from the UI");
			}
			try
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("Delivering a extension operation accomplishment token");
				}
				extensionOperationAccomplishment.put(new Boolean(true));
			}
			catch (InterruptedException e)
			{
				logger.error("While delivering an extension operation accomplishment token", e);
			}
		}
		else
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Extension was stopped but no under human control from the UI");
			}
		}
	}


	// -----------------------------------------------------------------------------------
	// -- PRIVATE Methods
	// -----------------------------------------------------------------------------------



	private void loadExtCollections()
	{
		try
		{
			getExtensionList.setString(1, extensionType);
			ResultSet rs = databaseProxy.executePreparedQuery(getExtensionList);
			if (rs != null)
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("Ubikit::exts table (getting {} extension only)", extensionType);
					logger.debug("-----------------------------------------------");
				}

				while (rs.next())
				{
					long extBundleID = rs.getLong(1);
					String extType = rs.getString(2);
					String distributorName = rs.getString(3);
					String distributorURL = rs.getString(4);

					Bundle extBundle = bundleContext.getBundle(extBundleID);

					if (logger.isDebugEnabled())
					{
						StringBuilder trace = new StringBuilder();
						trace.append(extType).append("\t").append(extBundleID).append("\t").append(
								distributorName).append("\t").append(distributorURL);

						if (extBundle == null)
						{
							trace.append(" (was removed, db will be updated)");
						}

						logger.debug(trace.toString());
					}

					if (extBundle != null)
					{ // Is the extension bundle really installed ?
						// Yes, the bundle really exists
						ExtensionProperties ep = createAppProperties(
								extBundleID,
								distributorName,
								distributorURL);
						exts.put(extBundleID, ep);
						extBundleUIDs.put(ep.getUID(), extBundleID);

						ExtensionExecutionPropertiesImpl eep = extExecutionProperties
								.get(extBundleID);
						if (eep == null)
						{ // Is the extension bundle already started ?
							// No, we have to initialize its execution properties
							extExecutionProperties.put(
									extBundleID,
									new ExtensionExecutionPropertiesImpl(ep.getUID(), extBundleID));
						}
					}
					else
					{
						// No, this bundle should have been uninstalled, and ubikit was not aware of
						// that.
						// The bundle reference is removed from the database
						removeExtensionFromDatabase(extBundleID);
					}

				}
			}
		}
		catch (SQLException e)
		{
			logger.error("While getting app collection from the ubikit database", e);
		}
	}



	private ExtensionProperties createAppProperties(
			long extensionBundleID,
			String distributorName,
			String distributorURL)
	{
		try
		{
			// Read the build info file and extract system version number, build unique id and build
			// date.
			JSONObject appMetadata = BundleResourceUtil.getResourceJSONContent(bundleContext
					                                                                   .getBundle(extensionBundleID)
					                                                                   .getEntry(metadataFilename));

			ExtensionPropertiesImpl ep = new ExtensionPropertiesImpl(
					appMetadata.getString("uid"),
					distributorName,
					distributorURL);

			ep.setName(appMetadata.getString("name"));
			ep.setVendor(appMetadata.getString("vendor"));
			ep.setVersion(appMetadata.getString("version"));
			ep.setBuild(appMetadata.getString("build"));
			//ep.setBuildDate(appMetadata.getString("build_date"));

			if (distributorURL == null)
			{
				ep.setOrigin(ExtensionOrigin.PREINSTALLED);
			}
			else
			{
				ep.setOrigin(ExtensionOrigin.DOWNLOADED);
			}

			return ep;
		}
		catch (IOException e)
		{
			logger.error("While opening {} file to read app meta-data.", metadataFilename, e);
		}
		catch (JSONException e)
		{
			logger.error(
					"While reading and parsing {} file to extract app meta-data.",
					metadataFilename,
					e);
		}
		catch (Exception e)
		{
			logger.error(
					"While creating app properties: bundleID={}, distributorName={}, distributorURL={}, metadataFilename={}",
					extensionBundleID, distributorName, distributorURL,
					metadataFilename,
					e);
		}

		return null;
	}



	private void removingExtensionFromRegistry(long bundleUID)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Removing extension from registries");
		}
		extExecutionProperties.remove(bundleUID);
		exts.remove(bundleUID);
		extBundleUIDs.remove(bundleUID);
	}



	private boolean removeExtensionFromDatabase(long bundleUID)
	{
		try
		{
			removeExtension.setLong(1, bundleUID);
			if (databaseProxy.executePreparedUpdate(removeExtension) < 0)
			{
				logger.error("While removing an uninstalled app from the ubikit database: remove "
						             + "query return a negative integer.");
				return false;
			}

			return true;
		}
		catch (SQLException e)
		{
			logger.error("While removing an uninstalled app from the ubikit database.", e);
		}

		return false;
	}
}
