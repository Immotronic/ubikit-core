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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.DatabaseProxy;
import org.ubikit.system.DatabaseManager;



// ThreadSafe
public class DatabaseProxyImpl implements DatabaseProxy, DatabaseManager
{
	private static final String CHECKPOINT_STATEMENT = "CHECKPOINT";
	private static final String SHUTDOWN_STATEMENT = "SHUTDOWN";
	private static final String GET_AUTO_INCREMENT_IDENTIFIER_STATEMENT = "CALL IDENTITY();";
	private final PreparedStatement getAutoIncrementIdentifierStatement;

	private Connection connection;
	private final boolean isConfiguredForEmbeddedSystem;

	final Logger logger = LoggerFactory.getLogger(DatabaseProxyImpl.class);




	public DatabaseProxyImpl(Connection connection, boolean isConfiguredForEmbeddedSystem)
	{
		this.connection = connection;
		this.isConfiguredForEmbeddedSystem = isConfiguredForEmbeddedSystem;
		try
		{
			this.connection.setAutoCommit(false);
		}
		catch (SQLException e)
		{
			logger.error("Error while setAutoCommit(false): ", e);
		}

		getAutoIncrementIdentifierStatement = getPreparedStatement(GET_AUTO_INCREMENT_IDENTIFIER_STATEMENT);

		if (logger.isDebugEnabled())
		{
			logger.debug("new connection ({}) was assigned to this database proxy {}",connection, this);
		}
	}






	@Override
	public void closeDatabaseConnection()
	{
		if (connection != null)
		{
			try
			{
				synchronized (this)
				{
					executeUpdate(SHUTDOWN_STATEMENT);
					connection.close();
					connection = null;
				}
			}
			catch (Exception e)
			{
				logger.error("Error while closing database", e);
			}
		}
	}






	@Override
	public void checkpoint()
	{
		if (connection != null)
		{
			try
			{
				synchronized (this)
				{
					executeUpdate(CHECKPOINT_STATEMENT);
				}
			}
			catch (Exception e)
			{
				logger.error("Error while flushing database", e);
			}
		}
	}






	@Override
	public boolean isClosed()
	{
		if (connection != null)
		{
			try
			{
				synchronized (this)
				{
					return connection.isClosed();
				}
			}
			catch (SQLException e)
			{
				logger.error(
					"Error while determining if database connection is closed or not",
					e);
				closeDatabaseConnection();
			}
		}

		return true;
	}






	@Override
	public boolean isValid()
	{
		if (connection != null)
		{
			try
			{
				synchronized (this)
				{
					return connection.isValid(0);
				}
			}
			catch (SQLException e)
			{}
		}

		return false;
	}






	@Override
	public ResultSet executeQuery(String sql)
	{
		try
		{
			synchronized (this)
			{
				Statement statement = connection.createStatement();
				return statement.executeQuery(sql);
			}
		}
		catch (Exception e)
		{
			logger.error("Error while executeQuery({}) using connection {}",sql,  connection, e);
		}

		return null;
	}






	@Override
	public int executeUpdate(String sql)
	{
		try
		{
			int res;

			synchronized (this)
			{
				Statement statement = connection.createStatement();
				res = statement.executeUpdate(sql);
				connection.commit();
			}

			if (isConfiguredForEmbeddedSystem)
			{
				try
				{
					Runtime.getRuntime().exec("sync");
				}
				catch (Exception e)
				{
					logger.error("While trying to sync a commit onto the disk.", e);
				}
			}

			return res;
		}
		catch (Exception e)
		{
			logger.error("Error while executeUpdate({}) using connection ",sql, connection, e);
		}

		return -1;
	}






	@Override
	public PreparedStatement getPreparedStatement(String sql)
	{
		try
		{
			synchronized (this)
			{
				return connection.prepareStatement(sql);
			}
		}
		catch (Exception e)
		{
			logger.error("Error while getPreparedStatement({}) using connection {}",sql, connection, e);
		}

		return null;
	}






	@Override
	public ResultSet executePreparedQuery(PreparedStatement statement)
	{
		try
		{
			synchronized (this)
			{
				return statement.executeQuery();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while executePreparedQuery({}) using connection {}", statement, connection, e);
		}

		return null;
	}






	@Override
	public int executePreparedUpdate(PreparedStatement statement)
	{
		try
		{
			int res;

			synchronized (this)
			{
				res = statement.executeUpdate();
				connection.commit();
			}

			if (isConfiguredForEmbeddedSystem)
			{
				try
				{
					Runtime.getRuntime().exec("sync");
				}
				catch (Exception e)
				{
					logger.error("While trying to sync a commit onto the disk.", e);
				}
			}
			return res;
		}
		catch (Exception e)
		{
			logger.error("Error while executePreparedUpdate({}) using connection {}",statement, connection, e);
		}

		return -1;
	}






	@Override
	public int executePreparedInsertAndGetAutoIncrementId(PreparedStatement statement)
	{
		int res = -1;
		ResultSet rs = null;

		try
		{
			synchronized (this)
			{
				res = statement.executeUpdate();
				connection.commit();
				rs = this.getAutoIncrementIdentifierStatement.executeQuery();
				if (rs.next())
				{
					res = rs.getInt(1);
				}
			}

			if (isConfiguredForEmbeddedSystem)
			{
				try
				{
					Runtime.getRuntime().exec("sync");
				}
				catch (Exception e)
				{
					logger.error("While trying to sync a commit onto the disk.", e);
				}
			}

			return res;
		}
		catch (Exception e)
		{
			logger.error("Error while executePreparedUpdate({}) using connection {}",statement, connection, e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
					logger.warn("While closing th GetIdentifier result set.");
				}
			}
		}

		return -1;
	}






	@Override
	public int[] executePreparedBatch(PreparedStatement statement)
	{
		try
		{
			int[] res;

			synchronized (this)
			{
				res = statement.executeBatch();
				connection.commit();
			}

			if (isConfiguredForEmbeddedSystem)
			{
				try
				{
					Runtime.getRuntime().exec("sync");
				}
				catch (Exception e)
				{
					logger.error("While trying to sync a commit onto the disk.", e);
				}
			}
			return res;
		}
		catch (Exception e)
		{
			logger.error("Error while executePreparedBatch({}) using connection {}",statement, connection, e);
		}

		return null;
	}






	@Override
	public Connection getRowDatabaseConnection()
	{
		return connection;
	}
}
