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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractPhysicalEnvironmentItem implements PhysicalEnvironmentItem
{
	private final String UID;
	private final String htmlUID;
	private final Map<String, String> properties;
	private Type type;
	private boolean configurable;
	private final Set<String> capabilities;
	private final JSONObject configuration;
	
	public static String getHTMLIDFromUID(String UID)
	{
		if(UID == null) {
			throw new IllegalArgumentException("UID parameter MUST NOT be null");
		}
		
		return "ID"+UID.replaceAll("[.:/#]", "");
	}
	
	protected AbstractPhysicalEnvironmentItem(String UID, Type type, boolean configurable, JSONObject configuration)
	{
		if(UID == null) {
			throw new IllegalArgumentException("uid parameter MUST NOT be null");
		}
		
		this.UID = UID;
		htmlUID = getHTMLIDFromUID(UID);
		this.type = type;
		this.configurable = configurable;
		properties = Collections.synchronizedMap(new HashMap<String, String>());
		capabilities = new HashSet<String>();
		this.configuration = configuration;
	}

	@Override
	public String getUID() 
	{
		return UID;
	}
	
	@Override
	public String getHTMLID()
	{
		return htmlUID;
	}

	@Override
	public String getPropertyValue(String propertyName) 
	{
		return properties.get(propertyName);
	}

	@Override
	public Type getType() 
	{
		return type;
	}
	
	public void setType(Type type) 
	{
		if(type == null) {
			throw new IllegalArgumentException("type parameter MUST NOT be null");
		}
		
		this.type = type;
	}
	
	@Override
	public boolean isConfigurable()
	{
		return configurable;
	}
	
	public void setConfigurable(boolean configurable) 
	{
		this.configurable = configurable;
	}

	@Override
	public void setPropertyValue(String propertyName, String propertyValue) 
	{
		properties.put(propertyName, propertyValue);
		propertiesHaveBeenUpdated(new String[] { propertyName});
	}
	
	@Override
	public JSONObject getPropertiesAsJSONObject()
	{
		JSONObject o = new JSONObject();
		try 
		{
			for(String key : properties.keySet())
			{
				o.put(key, properties.get(key));
			}
		}
		catch (JSONException e) {
			throw new RuntimeException("Cannot serialize item properties.", e);
		}
		
		return o;
	}
	
	@Override
	public void setPropertiesFromJSONObject(JSONObject properties)
	{
		 // update need to be notified ONLY if properties are changed. Not if this function is used as initializer
		boolean updateNotificationNeeded = !this.properties.isEmpty();
		
		if(properties != null)
		{
			Iterator<?> it = properties.keys();
			while(it.hasNext()) 
			{
				String key = (String) it.next();
				try 
				{
					this.properties.put(key, properties.getString(key));
				} 
				catch (JSONException e) 
				{
					throw new RuntimeException("Cannot deserialize item properties.", e);
				}
			}
			
			if(updateNotificationNeeded) {
				propertiesHaveBeenUpdated(JSONObject.getNames(properties));
			}
		}
	}
	
	protected void addCapability(String capability)
	{
		capabilities.add(capability);
	}
	
	@Override
	public String[] getCapabilities()
	{
		return capabilities.toArray(new String[0]);
	}
	
	@Override
	public JSONArray getCapabilitiesAsJSON()
	{
		JSONArray res = new JSONArray();
		for(String s : capabilities)
		{
			res.put(s);
		}
		
		return res;
	}
	
	@Override
	public boolean hasCapability(String capability)
	{
		return capabilities.contains(capability);
	}
	
	@Override
	public JSONObject getConfigurationAsJSON()
	{
		return this.configuration;
	}
	
	
	protected abstract void propertiesHaveBeenUpdated(String[] propertiesName);
	
	protected abstract void terminate();
}
