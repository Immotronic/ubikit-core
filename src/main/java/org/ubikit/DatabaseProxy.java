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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;



/**
 * DatabaseProxy is a THREAD SAFE means to an APP or a PEM to interact with its database.
 */
public interface DatabaseProxy
{
	/**
	 * Check if the database proxy is in a valid state. (e.g. if a database connection is set, open
	 * and valid)
	 * 
	 * @return
	 */
	public boolean isValid();






	/**
	 * Force the database to perform a checkpoint.
	 */
	public void checkpoint();






	/**
	 * Performs the given SELECT SQL statement and return a ResultSet object that contains resulting
	 * data.
	 * 
	 * This method return null if the SQL statement string is not valid (with syntax error, grammar
	 * error, or containing reference to unknown tables or fields).
	 * 
	 * @param sql
	 *            a valid SELECT SQL string
	 * 
	 * @return Queried data from database as a ResultSet object, or null if an error occurs while
	 *         performing the query.
	 */
	public ResultSet executeQuery(String sql);






	/**
	 * Performs the given SQL statement that alter table contents or table structure (such as
	 * UPDATE, INSERT, DELETE, ALTER TABLE, etc.)
	 * 
	 * The SQL statement is automatically committed. This method return the number of rows affected
	 * by this statement or -1 if an error occurs while performing this statement.
	 * 
	 * @param sql
	 *            a valid SQL statement
	 * 
	 * @return The number of rows affected by this statement or -1 if an error occurs while
	 *         performing this statement
	 */
	public int executeUpdate(String sql);






	/**
	 * Precompiles the given SQL statement into a PreparedStatement object.
	 * 
	 * @see java.sql.PreparedStatement
	 * 
	 * @param sql
	 *            a valid SQL statement
	 * 
	 * @return a PreparedStatement object, or null if an error occurs while compiling the given SQL
	 *         statement
	 */
	public PreparedStatement getPreparedStatement(String sql);






	/**
	 * Performs a precompiled SELECT SQL statement.
	 * 
	 * @param statement
	 *            a valid precompiled SQL statement
	 * 
	 * @return Queried data from database as a ResultSet object, or null if an error occurs while
	 *         performing the query.
	 */
	public ResultSet executePreparedQuery(PreparedStatement statement);






	/**
	 * Performs a precompiled SQL statement that insert a new row in a table where the raw ID is an
	 * auto-increment integer.
	 * 
	 * The SQL statement is automatically committed. This method return the ID of the row inserted
	 * by this statement or -1 if an error occurs while performing this statement.
	 * 
	 * @param statement
	 *            a valid precompiled SQL statement
	 * 
	 * @return The auto-incremented ID of the inserted row or -1 if an error occurs while performing
	 *         this statement
	 */
	public int executePreparedInsertAndGetAutoIncrementId(PreparedStatement statement);






	/**
	 * Performs a precompiled SQL statement that alter table contents or table structure. (such as
	 * UPDATE, INSERT, DELETE, ALTER TABLE, etc.)
	 * 
	 * The SQL statement is automatically committed. This method return the number of rows affected
	 * by this statement or -1 if an error occurs while performing this statement.
	 * 
	 * @param statement
	 *            a valid precompiled SQL statement
	 * 
	 * @return The number of rows affected by this statement or -1 if an error occurs while
	 *         performing this statement
	 */
	public int executePreparedUpdate(PreparedStatement statement);






	/**
	 * 
	 * @param statement
	 * 
	 * @return an array of update counts containing one element for each command in the batch. The
	 *         elements of the array are ordered according to the order in which commands were added
	 *         to the batch. Null if an error occurs while performing the batch.
	 */
	public int[] executePreparedBatch(PreparedStatement statement);






	/**
	 * Return the row Connection object to the database. That allows the developper to use native
	 * java.sql API to interact with the database.
	 * 
	 * <b>Note: The auto-commit feature has been disabled for that connection. Any statement
	 * that update the database content need to be manually committed.</b>
	 * 
	 * @return the row Connection object
	 */
	public Connection getRowDatabaseConnection();
}
