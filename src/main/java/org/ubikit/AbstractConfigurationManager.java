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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.impl.ConfigurationProperties;



/**
 * This class is a base class to implement configuration manager in applications and PEMs.
 * 
 * This class is Thread-Safe.
 * 
 * @author Lionel Balme <lbalme@immotronic.fr>
 *
 */
public abstract class AbstractConfigurationManager implements ManagedService, ConfigurationManager
{
	/**
	 * Extension of configuration file. '.cfg' is required to make FileInstall bundle aware of the
	 * nature of these files.
	 */
	private static final String configurationFileExtension = ".cfg";
	private static final String felixFileInstallProperty = "felix.fileinstall.filename";

	final Logger logger = LoggerFactory.getLogger(AbstractConfigurationManager.class);

	private final BundleContext bundleContext;
	private final ConfigurationAdmin configurationAdminService;
	private final String applicationID;
	private final String servicePID;
	private final Map<Enum<?>, String> properties;
	private final List<ConfigurationViewImpl> views;

	private ServiceRegistration<ManagedService> service = null;
	private Map<Enum<?>, Object> currentConfiguration = null;
	private String configurationFilePath = null;

	private final Lock lock;
	private final Condition notReady;

	private volatile boolean ready;






	/**
	 * This constructor MUST be called by the subclass one.
	 * 
	 * @param application
	 *            a reference onto the application main class
	 */
	protected AbstractConfigurationManager(	AbstractApplication application,
											String prefix,
											Enum<?>[] properties)
	{
		lock = new ReentrantLock();
		notReady = lock.newCondition();
		ready = false;

		if (application == null)
		{
			throw new IllegalArgumentException("'application' argument cannot be null.");
		}

		if (properties == null
			|| properties.length == 0)
		{
			throw new IllegalArgumentException("'properties' argument cannot be null nor empty");
		}

		this.properties = new HashMap<Enum<?>, String>();

		if (prefix == null)
		{
			prefix = "";
		}

		for (Enum<?> property : properties)
		{
			this.properties.put(property, prefix
				+ property.name().replaceAll("_", "."));
		}

		this.bundleContext = application.getBundleContext();
		this.configurationAdminService = application.getConfigurationAdminService();
		this.applicationID = application.getAppUniqueID();
		this.servicePID = this.applicationID.replaceAll("/", ".");
		this.views = new CopyOnWriteArrayList<ConfigurationViewImpl>();

		logger.trace("Constructor(): Locking...");
		lock.lock();
		logger.trace("Constructor(): Locked.");
		try
		{
			registerService();
			while (!ready)
			{
				logger.trace("Constructor(): Waiting for initial update.");
				if (!notReady.await(3, TimeUnit.SECONDS))
				{
					logger.trace("Constructor(): Waiting: timeout occured.");
					throw new RuntimeException(
						"OSGi ConfigurationAdmin service did not provide an "
							+ "initial configuration in a reasonable amount of time.");
				}

				logger.trace("Constructor(): Waiting: done.");
				logger.info("Initial configuration was set and is ready to be used");
			}
		}
		catch (InterruptedException e)
		{
			logger.error("Initialization failed. Constructor execution was interrupted before "
				+ "the initial configuration was set by OSGi ConfigurationAdmin service");
		}
		finally
		{
			lock.unlock();
			logger.trace("Constructor(): Unlocked.");
		}
	}






	/**
	 * This method aims to provide a default configuration map for a given application or PEM.
	 * 
	 * Basically, a configuration is a list of property names associated with a value. A value must
	 * be of a simple type (e.g. String, Boolean, Byte, Short, Integer, Long, Float or Double).
	 * 
	 * Each key of this map is a property name. Each value of this map is a property value.
	 * 
	 * @return a configuration map.
	 */
	protected abstract Map<Enum<?>, Object> defaultConfiguration();






	@Override
	public final synchronized void stop()
	{
		unregisterService();
		for (ConfigurationViewImpl view : views)
		{
			view.setObserver(null);
			((ConfigurationViewImpl) view).close();
		}

		views.clear();
	}






	@Override
	public final synchronized ConfigurationView
		createView(Configurable observer, String name) throws ConfigurationException
	{
		ConfigurationViewImpl view = new ConfigurationViewImpl(this, name);
		view.setObserver(observer);
		views.add(view);

		if (currentConfiguration != null)
		{
			view.update(currentConfiguration);
		}
		else
		{
			logger.error("BUG FIX NEEDED: currentConfiguration si null in createView()");

			throw new RuntimeException(
				"BUG FIX NEEDED: currentConfiguration si null in createView()");
		}

		logger.debug("Configuration view {} has been created.", name);

		return view;
	}






	@Override
	public final void releaseView(ConfigurationView view)
	{
		if (view instanceof ConfigurationViewImpl)
		{
			views.remove(view);
			((ConfigurationViewImpl) view).setObserver(null);
			((ConfigurationViewImpl) view).close();

			logger.debug("Configuration view {} has been released.", ((ConfigurationViewImpl) view)
				.getName());
		}
		else
		{
			throw new IllegalArgumentException("'view' argument MUST be a object created by "
				+ "AbstractConfigurationManager.createView() method.");
		}
	}






	final synchronized void
		updateProperties(Map<Enum<?>, Object> newValues) throws ConfigurationException
	{
		updateConfiguration(newValues);
	}






	/**
	 * This method is for the use of the OSGi ConfigAdmin service only.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public final synchronized void updated(Dictionary configuration) throws ConfigurationException
	{
		logger.debug("A configuration update occured for {}.", servicePID);

		if (configuration == null)
		{
			logger.trace("Generating a default configuration...");

			Map<Enum<?>, Object> defaultConfiguration = defaultConfiguration();

			if (defaultConfiguration == null
				|| defaultConfiguration.isEmpty())
			{
				logger.info("No default configuration available."
					+ " Aborting the configuration task.");

				unregisterService();

				throw new ConfigurationException("", "No default configuration available for "
					+ applicationID);
			}
			else
			{
				try
				{
					Path configurationFilePath = Paths.get(ConfigurationProperties
						.getInstance()
						.getConfigurationDirectory()
						.toString(), servicePID
						+ configurationFileExtension);

					File configurationFile = configurationFilePath.toFile();
					if (configurationFile.exists())
					{
						logger.error("A configuration file for already exist in {}. Is "
							+ "fileInstall bundle correctly watch this directory ?"
							+ " Aborting the configuration task.", ConfigurationProperties
							.getInstance()
							.getConfigurationDirectory()
							.toString());

						unregisterService();

						throw new ConfigurationException(
							"",
							"A configuration file exists but has not been taken into account. "
								+ "Check FileInstall configuration, it must monitor '"
								+ ConfigurationProperties
									.getInstance()
									.getConfigurationDirectory()
									.toString() + "' directory.");
					}
					else if (!configurationFile.createNewFile())
					{
						logger.error("Cannot create {} configuration file in {} directory."
							+ " Check permissions. Aborting the configuration task.", servicePID
							+ configurationFileExtension, ConfigurationProperties
							.getInstance()
							.getConfigurationDirectory()
							.toString());

						unregisterService();

						throw new ConfigurationException(
							"",
							"Cannot create the configuration file. Check permissions in "
								+ ConfigurationProperties
									.getInstance()
									.getConfigurationDirectory()
									.toString() + "' directory.");
					}
					else
					{
						StringBuilder configurationContent = new StringBuilder();

						Iterator<Entry<Enum<?>, Object>> itr = defaultConfiguration
							.entrySet()
							.iterator();

						while (itr.hasNext())
						{
							Entry<Enum<?>, Object> entry = itr.next();
							String key = properties.get(entry.getKey());
							String value = "";

							try
							{
								value = entry.getValue().toString();
							}
							catch (NullPointerException e)
							{
								logger.error("In {} default configuration, {} MUST NOT be null."
									+ " Aborting the configuration task.", key);

								unregisterService();
								throw new ConfigurationException(key, "Cannot be null");
							}

							configurationContent.append(key).append(" = ").append(value).append(
								"\n");
						}

						try
						{
							FileOutputStream stream = new FileOutputStream(configurationFile);

							stream.write(configurationContent.toString().getBytes(
								Charset.forName("UTF-8")));

							stream.flush();
							stream.close();

							logger
								.trace("A default configuration has been successfully generated.");
						}
						catch (IOException e)
						{
							logger.error("Cannot write the default configuration."
								+ " Aborting the configuration task.", e);

							unregisterService();

							throw new ConfigurationException(
								"",
								"Cannot write the default configuration for "
									+ applicationID);
						}
					}
				}
				catch (Exception e)
				{
					logger.error("Cannot create a default configuration."
						+ " Aborting the configuration task.", e);

					unregisterService();

					throw new ConfigurationException(
						"",
						"Cannot create a default configuration for "
							+ applicationID + " for an unexpected reason",
						e);
				}
			}
		}

		/*
		 * INVARIANT: a default configuration exits, it contains no null value. A current
		 * configuration as Map<String, Object> MAY exits. 'configuration' argument is null or
		 * contains the latest configuration update provided by the OSGi ConfigAdmin service.
		 * 
		 * Because configurations are loaded using the FileInstall bundle that store all property
		 * values as java.lang.String, all values in the 'configuration' dictionary are of type
		 * java.lang.String.
		 */

		logger.trace("Updating the current configuration and notifying property listeners...");

		boolean forceUpdate = false;
		boolean configurationChanged = false;

		if (configuration == null)
		{
			logger.debug("No configuration available from OSGi ConfigAdmin service, using default "
				+ "configuration");
		}
		else
		{
			configurationFilePath = (String) configuration.get(felixFileInstallProperty);

			logger.info(
				"Configuration for {} is bound to {} configuration file.",
				applicationID,
				configurationFilePath);
		}

		if (currentConfiguration == null)
		{
			/*
			 * If currentConfiguration is null, it means that this configuration manager is just
			 * starting. Then, all configuration property listeners have to be updated, even if
			 * values in provided configuration are the same in the default configuration.
			 */
			logger.trace("Initial configuration update. All property listeners will be notified.");

			currentConfiguration = defaultConfiguration();
			forceUpdate = true;
		}

		Iterator<Entry<Enum<?>, Object>> itr = currentConfiguration.entrySet().iterator();
		while (itr.hasNext())
		{
			Entry<Enum<?>, Object> entry = itr.next();

			Enum<?> property = entry.getKey();
			String key = properties.get(property);
			Object currentValue = entry.getValue();
			Object newValue = null;
			if (configuration == null)
			{
				newValue = currentValue.toString();
			}
			else
			{
				newValue = configuration.get(key);
			}

			if (newValue == null
				|| currentValue == null)
			{
				logger.error(
					"A configuration update for {} has set a null value. This is unauthorized.",
					key);

				throw new ConfigurationException(key, "Cannot be null.");
			}

			/*
			 * INVARIANT: 'key' contains a property name and 'currentValue' is not null.
			 * 'currentValue' contains an object and its class could be known using reflection.
			 */

			if (update(property, newValue, forceUpdate))
			{
				configurationChanged = true;
			}
		} // while (itr.hasNext())

		if (configurationChanged)
		{
			logger.debug("Configuration for {} did change. Views need an update.", servicePID);

			for (ConfigurationViewImpl view : views)
			{
				logger.debug("Updating {} view.", view.getName());
				view.update(currentConfiguration);
			}
			if (logger.isDebugEnabled()
				&& views.isEmpty())
			{
				logger.debug("No view to update.");
			}
		}
		else
		{
			logger.debug("No actual configuration change after the update performed by the OSGi "
				+ "ConfigurationAdmin Service.");
		}

		/*
		 * If this update is the initial update of the configuration, right after a system startup,
		 * the notReady Condition is signal to let the constructor exits.
		 */
		if (forceUpdate)
		{
			logger.trace("updated(): Locking...");
			lock.lock();
			logger.trace("updated(): Locked.");
			try
			{
				logger.trace("updated(): Set the ready flag to 'true'.");
				ready = true;
				logger.trace("updated(): Initial configuration update, signaling the constructor.");
				notReady.signal();
				logger.trace("updated(): Constructor signaled.");
			}
			catch (IllegalMonitorStateException e)
			{
				logger.error("BUG FIX NEEDED, An exception occured while signaling the notReady "
					+ "Condition.", e);
			}
			finally
			{
				lock.unlock();
				logger.trace("updated(): Unlocked.");
			}
		}
	}






	private void registerService()
	{
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(Constants.SERVICE_PID, servicePID);

		logger.info("Registration of the ManagedService for {}.", applicationID);
		service = bundleContext.registerService(ManagedService.class, this, properties);
	}






	private void unregisterService()
	{
		if (service != null)
		{
			service.unregister();
			service = null;

			logger.info("The ManagedService for {} has been unregistered.", applicationID);
		}
	}






	private void updateConfiguration(Map<Enum<?>, Object> newValues)
	{
		// Create a new dictionary of configuration information
		Dictionary<String, Object> newConfigurationContent = new Hashtable<String, Object>();

		// Populate this dictionary with current configuration information
		Iterator<Entry<Enum<?>, Object>> itr = currentConfiguration.entrySet().iterator();
		while (itr.hasNext())
		{
			Entry<Enum<?>, Object> entry = itr.next();
			newConfigurationContent.put(properties.get(entry.getKey()), entry.getValue());
		}

		// Add to configuration information the FileInstall entry to keep the configuration file
		// synchronized with ConfigurationAdmin state.
		newConfigurationContent.put(felixFileInstallProperty, configurationFilePath);

		// Taking configuration changes into account
		Iterator<Entry<Enum<?>, Object>> itr2 = newValues.entrySet().iterator();
		while (itr2.hasNext())
		{
			Entry<Enum<?>, Object> entry = itr2.next();
			newConfigurationContent.put(properties.get(entry.getKey()), entry.getValue());
		}

		// Notify the OSGi ConfigurationAdmin Service of configuration changes for this servicePID
		try
		{
			Configuration configuration = configurationAdminService.getConfiguration(servicePID);
			configuration.update(newConfigurationContent);
			logger.info("A configuration update for {} was committed", servicePID);
		}
		catch (IOException e)
		{
			logger.error("Configuration update failed for {}.", servicePID, e);
		}
	}






	private boolean
		update(Enum<?> key, Object newValue, boolean forceUpdate) throws ConfigurationException
	{
		if (key == null
			|| newValue == null)
		{
			throw new IllegalArgumentException(
				"Neither 'key/property' nor 'value' arguments can be null");
		}

		Object currentValue = currentConfiguration.get(key);
		Object newValueObject = null;

		if (currentValue.getClass().equals(newValue.getClass()))
		{
			newValueObject = newValue;
		}
		else if (newValue instanceof String)
		{
			if (currentValue instanceof Boolean)
			{
				if (!"true".equalsIgnoreCase((String) newValue)
					&& !"false".equalsIgnoreCase((String) newValue))
				{
					logger.warn("New value for {} [Boolean] is not valid. It should be 'true' or "
						+ "'false' but worth '{}'. It was interpreted as 'false'", key, newValue);

					newValue = "false";
				}

				newValueObject = Boolean.valueOf((String) newValue);
			}
			else if (currentValue instanceof Byte)
			{
				try
				{
					newValueObject = Byte.valueOf((String) newValue);
				}
				catch (Exception e)
				{
					logger.error("New value for {} must be a byte, but worth '{}'", key, newValue);
					throw new ConfigurationException(key.toString(), "Value must be a byte.", e);
				}
			}
			else if (currentValue instanceof Short)
			{
				try
				{
					newValueObject = Short.valueOf((String) newValue);
				}
				catch (Exception e)
				{
					logger.error("New value for {} must be a short, but worth '{}'", key, newValue);
					throw new ConfigurationException(key.toString(), "Value must be a short.", e);
				}
			}
			else if (currentValue instanceof Integer)
			{
				try
				{
					newValueObject = Integer.valueOf((String) newValue);
				}
				catch (Exception e)
				{
					logger.error(
						"New value for {} must be an integer, but worth '{}'",
						key,
						newValue);
					throw new ConfigurationException(key.toString(), "Value must be a integer.", e);
				}
			}
			else if (currentValue instanceof Long)
			{
				try
				{
					newValueObject = Long.valueOf((String) newValue);
				}
				catch (Exception e)
				{
					logger.error("New value for {} must be a long, but worth '{}'", key, newValue);
					throw new ConfigurationException(key.toString(), "Value must be a long.", e);
				}
			}
			else if (currentValue instanceof Float)
			{
				try
				{
					newValueObject = Float.valueOf((String) newValue);
				}
				catch (Exception e)
				{
					logger.error("New value for {} must be a float, but worth '{}'", key, newValue);
					throw new ConfigurationException(key.toString(), "Value must be a float.", e);
				}
			}
			else if (currentValue instanceof Double)
			{
				try
				{
					newValueObject = Double.valueOf((String) newValue);
				}
				catch (Exception e)
				{
					logger
						.error("New value for {} must be a double, but worth '{}'", key, newValue);

					throw new ConfigurationException(key.toString(), "Value must be a double.", e);
				}
			}
		}

		if (newValueObject != null)
		{
			if (forceUpdate
				|| !currentValue.equals(newValueObject))
			{
				currentConfiguration.put(key, newValueObject);

				logger.debug("Property {} [{}] has a new value: '{}'.", key, newValueObject
					.getClass()
					.getSimpleName(), newValueObject);

				return true;
			}
		}
		else
		{
			logger.debug("Wrong class for property {}: {} (expected {}).", key, newValue
				.getClass()
				.getSimpleName(), currentValue.getClass().getSimpleName());

			throw new ConfigurationException(key.toString(), "Wrong class for property "
				+ key + ": " + newValue.getClass().getSimpleName() + " (expected: "
				+ currentValue.getClass().getSimpleName() + ")");
		}

		return false;
	}
}
