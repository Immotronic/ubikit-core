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

import java.util.Map;

import org.osgi.service.cm.ConfigurationException;

/**
 * An up-to-date view of configuration data for a given client object. Thanks to this view, a client
 * object can retrieve value of configuration properties and could know if values have changed since
 * its last reading.
 * 
 * It could also register a listener to be notified of configuration updates
 * 
 * @author Lionel Balme <lbalme@immotronic.fr>
 *
 */
public interface ConfigurationView
{
	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public Object get(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a String object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 * 
	 */
	public String getString(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Boolean object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public boolean getBoolean(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Byte object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public byte getByte(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Short object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public short getShort(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Integer object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public int getInteger(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Long object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public long getLong(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Float object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public float getFloat(Enum<?> key);






	/**
	 * Get the value of the configuration property identified by 'key' argument.
	 * 
	 * @param key
	 *            the identifier of the property to read.
	 * @return The property value
	 * 
	 * @throws IllegalArgumentException
	 *             if the given property is not a Double object.
	 * @throws RuntimeException
	 *             if the view is empty when getting a value.
	 */
	public double getDouble(Enum<?> key);






	/**
	 * Return true if the property identified by 'key' has changed since the last call to this
	 * method. If its value did not change, it return false.
	 * 
	 * @param key
	 *            the identifier of the property to check for change.
	 * @return true if the property value has change since the last check. False otherwise.
	 */
	public boolean hasChanged(Enum<?> key);






	/**
	 * Update values of a set of properties.
	 * 
	 * @param newValues
	 *            a set of properties and their associated new values. For each value, its actual
	 *            class must be of the one expected by the given property. For each value of class
	 *            String, it must be convertible into an object of the class expected by the
	 *            associated property.
	 * 
	 * @throws IllegalArgumentException
	 *             if newValues argument is null.
	 * @throws ConfigurationException
	 *             if some given properties do not exist in configuration or if some of given values
	 *             are not of the expected type, or are not convertible into objects of the expected
	 *             type.
	 */
	public void set(Map<Enum<?>, Object> newValues) throws ConfigurationException;






	/**
	 * Update the value of a single property.
	 * 
	 * @param property
	 *            the property to update
	 * @param newValue
	 *            the property new value. 'newValue' actual class must be of the one expected by the
	 *            given property. If 'newValue' class is String, it must be convertible into an
	 *            object of the class expected by the given property.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of property argument or value argument is null.
	 * @throws ConfigurationException
	 *             if the given property does not exist in configuration or if the given value is
	 *             not of the expected type, or is not convertible into an object of the expected
	 *             type.
	 */
	public void set(Enum<?> property, Object newValue) throws ConfigurationException;

}
