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

import org.json.JSONArray;
import org.json.JSONObject;

public interface PhysicalEnvironmentItem {
	
	public enum Type {
		SENSOR,
		ACTUATOR,
		SENSOR_AND_ACTUATOR,
		OTHER
	};
	
	public enum Property {
		CustomName,
		Location
	};
	
	enum JSONValueField {
		label,
		value,
		timestamp,
		unit, 
		uiValue
	};
	
	enum JSONValueSpecialUnit {
		date,
		time,
		datetime,
		duration,
	}

	/**
	 * Return the unique identifier of this item.
	 * @return A string that represent the unique identifier of this item.
	 */
	public String getUID();
	
	/**
	 * Return a string that can be used as an HTML element ID for this item. This string is built from the item UID.
	 * @return A string that represent an HTML element ID for this item based on the item UID.
	 */
	public String getHTMLID();

	/**
	 * Return the value of the given textual property associated to this item.
	 * 
	 * @param propertyName the name of the property to set the value.
	 * 
	 * @return the value of the given textual property associated to this item or null if the
	 * property has never been set yet.
	 */
	public String getPropertyValue(String propertyName);

	/**
	 * Return the coarse-grained type of the item. Could be one of 
	 *   PhysicalEnvironmentItem.Type.SENSOR,
	 *   PhysicalEnvironmentItem.Type.ACTUATOR,
	 *   PhysicalEnvironmentItem.Type.SENSOR_AND_ACTUATOR
	 *   PhysicalEnvironmentItem.Type.OTHER
	 *    
	 * @return the item coarse-grained type
	 */
	public Type getType();
	
	/**
	 * Return true if this item is configurable, false otherwise.
	 * @return true if this item is configurable, false otherwise.
	 */
	public boolean isConfigurable();
	
	/**
	 * Return an object that host the item value. This object should be casted with the
	 * adequate subclass to access the actual value.
	 * @return an Object instance
	 */
	public Object getValue();
	
	/**
	 * Returned object format : { "dataTypeName" : { object} | [ array of object ] }
	 * dataTypeName is mandatory to give the client the opportunity to know how to decode these data.
	 *  
	 * @return an instance of JSONObject class that contains last known data from this item.
	 */
	public JSONObject getValueAsJSON();

	/**
	 * Set a textual property to this item. 
	 * 
	 * @param propertyName the name of the property to set the value.
	 */
	public void setPropertyValue(String propertyName, String propertyValue);
	
	/**
	 * Serialize this item properties in a JSON object
	 * 
	 * @return a JSON object that is a serialization of properties associated to this item.
	 */
	public JSONObject getPropertiesAsJSONObject();
	
	/**
	 * Fill the properties of this item with data contained in the given JSON object.
	 * 
	 * @param properties a JSON object containing a set of textual key-value pairs.
	 */
	public void setPropertiesFromJSONObject(JSONObject properties);
	
	/**
	 * Return an array of strings that describe the item capabilities. These strings should each 
	 * contains a capability ID that must have a meaning for a given PEM.
	 * 
	 * @return an array of capability IDs
	 */
	public String[] getCapabilities();
	
	/**
	 * Return true if this item has the given capability. Return false otherwise.
	 * @param capability A capability identifier
	 * @return true if this item as the given capability, false if this item does not support the given capability.
	 */
	public boolean hasCapability(String capability);
	
	/**
	 * Return an JSON array of strings that describe this item capabilities. These strings should each 
	 * contains a capability ID that must have a meaning for a given PEM.
	 * 
	 * @return an JSON array of capability IDs
	 */
	public JSONArray getCapabilitiesAsJSON();
	
	/**
	 * Return the internal configuration of this item as a JSON object.
	 * @return a JSON object that contains the item internal configuration
	 */
	public JSONObject getConfigurationAsJSON();
}
