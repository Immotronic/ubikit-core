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

package org.ubikit.impl;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class aims to read from the OSGi 'config.properties' file the configuration properties for
 * Ubikit, parse them and verify their validity. Then, it delivers values of those properties to
 * client code.
 * 
 * A singleton instance of this class is created by the Ubikit bundle activator. Client code gets
 * this instance using the getInstance() static method.
 * 
 * @author Lionel Balme <lbalme@immotronic.fr>
 *
 */
public final class ConfigurationProperties
{
	final Logger logger = LoggerFactory.getLogger(ConfigurationProperties.class);

	private final Path configurationDirectory;
	private final boolean embedded;
	private final String rootApp;
	private final int httpServicePort;

	/**
	 * This enum contains property keys to read in 'config.properties' file. In these key names,
	 * each undersore match a dot in the configuration file. (e.g. 'org_ubikit_embedded' refer to
	 * the 'org.ubikit.embedded' property in 'config.properties' file)
	 * 
	 * @author Lionel Balme <lbalme@immotronic.fr>
	 */
	private static enum Property
	{
		/**
		 * Need to worth true if Ubikit is running into a gateway
		 */
		embedded("org.ubikit.embedded"),

		/**
		 * Need to contain the application ID of the application that provides the main user
		 * interface.
		 */
		rootApp("org.ubikit.rootApp"),

		/**
		 * Need to contains an absolute OR relative path to the directory that hosts configuration
		 * files for applications & PEMs.
		 */
		configurationDir("org.ubikit.configuration.dir"),

		/**
		 * The port number on which the HTTP service is listening.
		 */
		httpServicePort("org.osgi.service.http.port");

		private final String string;






		private Property(String string)
		{
			this.string = string;
		}






		public String toString()
		{
			return string;
		}
	};






	/**
	 * Return the instance of ConfigurationProperties created by the bundle activator, or throw an
	 * exception if this instance has not been created yet.
	 * 
	 * @return the ConfigurationProperties object.
	 * @throws RuntimeException
	 *             if no instance of ConfigurationProperties exists.
	 * 
	 *             However, this SHOULD never happen given that this instance is created when Ubikit
	 *             bundle start. By design, client objects of ConfigurationProperties are always
	 *             instantiated after Ubikit bundle start.
	 * 
	 *             If that exception happen, something went probably wrong during the client object
	 *             instantiation.
	 */
	public static ConfigurationProperties getInstance()
	{
		if (Activator.configurationProperties == null)
		{
			throw new RuntimeException(
				"ConfigurationProperties object has not been instantied yet.");
		}

		return Activator.configurationProperties;
	}






	/**
	 * Construct an instance of ConfigurationProperties. This task need to be done in the start()
	 * method of the Ubikit bundle activator.
	 * 
	 * @param bundleContext
	 *            BundleContext object
	 */
	ConfigurationProperties(BundleContext bundleContext)
	{
		/*
		 * Reading and parsing configuration_dir property
		 */
		String configurationDirectoryProperty = bundleContext.getProperty(Property.configurationDir
			.toString());

		Path configurationDirectory = null;

		try
		{
			configurationDirectory = Paths.get(configurationDirectoryProperty);
			File f = configurationDirectory.toFile();
			if (f.isDirectory())
			{
				logger.info(
					"Using {} as directory for configuration files.",
					configurationDirectory.toString());
			}
			else
			{
				configurationDirectory = null;
				logger.error(
					"In 'config.properties' file, {} contains a path to a directory that does "
						+ "not exist ({})",
					Property.configurationDir.toString(),
					configurationDirectory);
			}

		}
		catch (InvalidPathException | NullPointerException e)
		{
			configurationDirectory = null;
			logger.error(
				"In 'config.properties' file, {} contains '{}' that is a invalid path string.",
				Property.configurationDir.toString(),
				configurationDirectory);
		}

		this.configurationDirectory = configurationDirectory;

		/*
		 * Reading and parsing embedded property
		 */
		String embeddedProperty = bundleContext.getProperty(Property.embedded.toString());
		this.embedded = Boolean.valueOf(embeddedProperty).booleanValue();

		logger.info("System is set for {} mode", (this.embedded ? "embedded" : "development"));

		/*
		 * Reading rootApp property
		 */
		this.rootApp = bundleContext.getProperty(Property.rootApp.toString());
		if (this.rootApp == null)
		{
			logger.error("No root application has been specified in the 'config.properties' file. "
				+ "The {} property must be set.", Property.rootApp.toString());
		}
		else
		{
			logger.info("Root application is {}", this.rootApp);
		}

		/*
		 * Reading httpServicePort property
		 */
		String httpServicePortProperty = bundleContext.getProperty(Property.httpServicePort
			.toString());

		int httpServicePort = 0;

		try
		{
			httpServicePort = Integer.valueOf(httpServicePortProperty).intValue();
			if (httpServicePort < 1
				&& httpServicePort > 65535)
			{
				throw new RuntimeException("Invalid port number. Must be 0 < x < 65536.");
			}
			logger.info("HTTP service is listening on port {}", httpServicePort);
		}
		catch (Exception e)
		{
			logger.error("No valid port specification found in 'config.properties' file for the "
				+ "HTTP service. The {} property must be set with an integer between 1 to "
				+ "65535", Property.httpServicePort.toString());
		}

		this.httpServicePort = httpServicePort;
	}






	/**
	 * Return the path to the directory that hosts configuration files for applications & PEMs.
	 * 
	 * @return a path to the configuration directory
	 * @throws RuntimeException
	 *             if the property in the OSGi framework 'config.properties' does not contain a
	 *             valid value.
	 */
	public Path getConfigurationDirectory()
	{
		if (configurationDirectory == null)
		{
			throw new RuntimeException("Configuration directory is not configured. See logs.");
		}

		return configurationDirectory;
	}






	/**
	 * Return the value of the embedded property. This property must worth true if Ubikit is running
	 * in a gateway or false if running on a development machine.
	 * 
	 * @return true if Ubikit runs in a gateway, false otherwise.
	 */
	public boolean getEmbedded()
	{
		return this.embedded;
	}






	/**
	 * Return the application ID of the root application. This application is in charge to provide
	 * the main user interface and specific services for a given gateway type.
	 * 
	 * @return the application ID of the root application.
	 */
	public String getRootApp()
	{
		return this.rootApp;
	}






	/**
	 * Return the value of the httpServicePort property. Its value should be a valid port number. If
	 * httpServicePort worth 0, it means that the 'org.osgi.service.http.port' property in
	 * 'config.properties' file has not been set or contains an invalid value.
	 * 
	 * @return the port number the HTTP service listens to.
	 */
	public int getHttServicePort()
	{
		return this.httpServicePort;
	}
}
