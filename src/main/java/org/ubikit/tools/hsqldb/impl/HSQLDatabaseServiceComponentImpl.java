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

package org.ubikit.tools.hsqldb.impl;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.service.HSQLDatabaseService;

@Component
@Provides
@Instantiate
public class HSQLDatabaseServiceComponentImpl implements HSQLDatabaseService
{	
	private static final String defaultDbBaseFolder = "db/"; // Trailing slash is mandatory
	private static final String defaultPemDbSubfolder = "pems/"; // Trailing slash is mandatory
	private static final String defaultAppDbSubfolder = "apps/"; // Trailing slash is mandatory
	private static final String defaultSystemDbSubfolder = "system/"; // Trailing slash is mandatory
	
	private String dbBaseFolder;
	private boolean isConfiguredForEmbeddedSystem;
	private DatabasePasswordManager passwordManager;
	
	final Logger logger = LoggerFactory.getLogger(HSQLDatabaseServiceComponentImpl.class);
	
	public HSQLDatabaseServiceComponentImpl(BundleContext bundleContext)
	{
		// Getting the default base folder far database that is defined in the
		// OSGi conf/config.properties file, if any.
		dbBaseFolder = bundleContext.getProperty("org.ubikit.database.folder");
		isConfiguredForEmbeddedSystem = Boolean.parseBoolean(bundleContext.getProperty("org.ubikit.embedded"));
		
		passwordManager = new DatabasePasswordManagerImpl();
		
		if(dbBaseFolder == null) {
			// If config.properties file does not contains any default base folder
			// the hard coded default one is used.
			dbBaseFolder = defaultDbBaseFolder;
		}
		
		if(!dbBaseFolder.endsWith("/"))
		{
			// The base folder string must ends with a "/". If this is not the case
			// one is added.
			dbBaseFolder += "/";
		}
	}
	
	@Validate
	public synchronized void start()
	{
		File databaseFolder =new File(dbBaseFolder);
		if(!databaseFolder.exists()) // Check if the base folder for database exists.
		{
			// If the base folder does not exist, it is created
			if(!databaseFolder.mkdirs()) 
			{
				// If the base folder cannot be created, an error message is generated into the logs
				logger.error("Database folder connot be created. Check permissions.");
			}
			else {
				new File(dbBaseFolder+defaultPemDbSubfolder).mkdir();
				new File(dbBaseFolder+defaultAppDbSubfolder).mkdir();
				new File(dbBaseFolder+defaultSystemDbSubfolder).mkdir();
			}
		}
		else if(!databaseFolder.canRead() || !databaseFolder.canWrite()) 
		{
			// If the base folder exists, reading and writing permissions are checks. If such
			// access are not allowed, an error message is generated into the logs.
			logger.error("Database folder is not readable or writeable. Check permissions.");
		}
		 
		try
		{
			Class.forName("org.hsqldb.jdbc.JDBCDriver"); // Loading HSQLDB JDBC driver
			if(logger.isDebugEnabled()) {
				logger.debug("HSQLDB driver just started");
			}
		}
		catch(ClassNotFoundException e)
		{
			logger.error("HSQLDB driver was not loaded properly", e);
		}
	}
	
	@Invalidate
	public synchronized void stop()
	{
		if(logger.isDebugEnabled()) {
			logger.debug("HSQLDB driver was stopped");
		}
	}
	
	@Override
	public Connection create(String databaseFilename, HSQLDatabaseService.Type type) throws SQLException
	{
		Connection conn = null;
		String dbSubFolder;
		
		switch(type) {
			case APP:
				dbSubFolder = defaultAppDbSubfolder;
				break;
			case PEM:
				dbSubFolder = defaultPemDbSubfolder;
				break;
			default:
				dbSubFolder = defaultSystemDbSubfolder;
				break;
		}
		
		String writeDelayPolicy = "";
		if(isConfiguredForEmbeddedSystem) {
			writeDelayPolicy = ";hsqldb.write_delay=false";
		}
		String dbConnectionDescriptor = "jdbc:hsqldb:" + dbBaseFolder + dbSubFolder + databaseFilename + ";shutdown=true"+writeDelayPolicy;
		
		try
		{
			String[] credentials = passwordManager.generateUserNameAndPassword(databaseFilename);
			conn = DriverManager.getConnection(dbConnectionDescriptor, credentials[0], credentials[1]);
			if(logger.isDebugEnabled()) {
				logger.debug("new database connection created: {}", dbConnectionDescriptor);
			}
		}
		catch(SQLException e)
		{
			logger.error("While creating a connection to jdbc:hsqldb: {}", dbConnectionDescriptor);
			throw e;
		}
		
		return conn;
	}
}
