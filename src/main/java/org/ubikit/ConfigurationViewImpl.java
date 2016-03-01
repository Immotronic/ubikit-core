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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ConfigurationViewImpl implements ConfigurationView
{
	final Logger logger = LoggerFactory.getLogger(ConfigurationViewImpl.class);

	private static class Value
	{
		private Object value = null;
		private boolean changed = false;






		private Value(Object value)
		{
			this.value = value;
			changed = true;
		}






		private boolean update(Object value)
		{
			if ((this.value == null && value != null)
				|| (this.value != null && !this.value.equals(value)))
			{
				this.value = value;
				changed = true;
			}

			return changed;
		}






		private boolean hasChanged()
		{
			boolean res = changed;
			changed = false;
			return res;
		}






		private Object read()
		{
			changed = false;
			return value;
		}
	}

	private final String name;
	private final AbstractConfigurationManager manager;
	private final Map<Enum<?>, Value> currentConfiguration;
	private Configurable configurable = null;






	/**
	 * Create an empty configuration view. It will be filled with property & values on a first call
	 * to update() method.
	 * 
	 * @param manager
	 *            the configuration manager that creates this view
	 * @param name
	 *            a name for the view. This name is use to enhance logging information only.
	 * 
	 *            This constructor is for the use of AbstractConfigurationManager class ONLY.
	 */
	public ConfigurationViewImpl(AbstractConfigurationManager manager, String name)
	{
		if (manager == null)
		{
			throw new IllegalArgumentException("'manager' argument MUST not be null.");
		}

		this.manager = manager;
		this.name = (name == null) ? "Anonymous" : name;
		currentConfiguration = new HashMap<Enum<?>, Value>();
	}






	/**
	 * Update the current configuration view with fresh values.
	 * 
	 * This method is for the use of AbstractConfigurationManager class ONLY.
	 * 
	 * @param configuration
	 *            a configuration.
	 */
	public synchronized void
		update(Map<Enum<?>, Object> configuration) throws ConfigurationException
	{
		if (configuration == null)
		{
			throw new IllegalArgumentException("'configuration' argument cannot be null.");
		}

		Iterator<Entry<Enum<?>, Object>> itr = configuration.entrySet().iterator();
		List<Enum<?>> changeList = new ArrayList<Enum<?>>();

		while (itr.hasNext())
		{
			Entry<Enum<?>, Object> entry = itr.next();
			Enum<?> key = entry.getKey();
			Object value = entry.getValue();

			Value v = currentConfiguration.get(key);
			if (v == null)
			{
				currentConfiguration.put(key, new Value(value));
				changeList.add(key);
			}
			else
			{
				if (v.update(value))
				{
					changeList.add(key);
				}
			}
		}

		/*
		 * If a Configurable object is set AND there are some changes in configuration, the
		 * Configurable object is notified.
		 */
		if (configurable != null)
		{
			Enum<?>[] changes = changeList.toArray(new Enum<?>[0]);
			configurable.configurationUpdate(changes, this);
		}
	}






	/**
	 * Close a configuration view by clearing its current configuration.
	 * AbstractConfigurationManager will call that method when a client object release the view.
	 * 
	 * This method is for the use of AbstractConfigurationManager class ONLY.
	 */
	public synchronized void close()
	{
		currentConfiguration.clear();
	}






	@Override
	public synchronized Object get(Enum<?> key)
	{
		return getValue(key).read();
	}






	@Override
	public synchronized String getString(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof String))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a String but a " + v.getClass().getSimpleName());
		}

		return ((String) v);
	}






	@Override
	public synchronized boolean getBoolean(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Boolean))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Boolean but a " + v.getClass().getSimpleName());
		}

		return ((Boolean) v).booleanValue();
	}






	@Override
	public synchronized byte getByte(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Byte))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Byte but a " + v.getClass().getSimpleName());
		}

		return ((Byte) v).byteValue();
	}






	@Override
	public synchronized short getShort(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Short))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Short but a " + v.getClass().getSimpleName());
		}

		return ((Short) v).shortValue();
	}






	@Override
	public synchronized int getInteger(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Integer))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Integer but a " + v.getClass().getSimpleName());
		}

		return ((Integer) v).intValue();
	}






	@Override
	public synchronized long getLong(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Long))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Long but a " + v.getClass().getSimpleName());
		}

		return ((Long) v).longValue();
	}






	@Override
	public synchronized float getFloat(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Float))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Float but a " + v.getClass().getSimpleName());
		}

		return ((Float) v).floatValue();
	}






	@Override
	public synchronized double getDouble(Enum<?> key)
	{
		Object v = getValue(key).read();
		if (!(v instanceof Double))
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' is not a Double but a " + v.getClass().getSimpleName());
		}

		return ((Double) v).doubleValue();
	}






	@Override
	public synchronized boolean hasChanged(Enum<?> key)
	{
		if (currentConfiguration.isEmpty())
		{
			return false;
		}

		return getValue(key).hasChanged();
	}






	public synchronized void setObserver(Configurable observer)
	{
		this.configurable = observer;
	}






	public String getName()
	{
		return name;
	}






	private Value getValue(Enum<?> key)
	{
		if (currentConfiguration.isEmpty())
		{
			throw new RuntimeException("Configuration view is still empty. Try again later.");
		}

		Value v = currentConfiguration.get(key);
		if (v == null)
		{
			throw new IllegalArgumentException("Property '"
				+ key + "' does not exist in configuration");
		}

		return v;
	}






	@Override
	public void set(Map<Enum<?>, Object> newValues) throws ConfigurationException
	{
		manager.updateProperties(newValues);
	}






	@Override
	public void set(Enum<?> property, Object newValue) throws ConfigurationException
	{
		if (newValue == null)
		{
			throw new ConfigurationException(
				property.toString(),
				"Candidate value is null. This is not allowed.");
		}

		Map<Enum<?>, Object> newValues = new HashMap<Enum<?>, Object>();
		newValues.put(property, newValue);

		manager.updateProperties(newValues);
	}
}
