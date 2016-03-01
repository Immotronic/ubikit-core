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

import org.osgi.service.cm.ConfigurationException;



public interface ConfigurationManager
{
	/**
	 * Stop a configuration manager. It will no longer monitor configuration updates, all views will
	 * be released.
	 * 
	 * For better performance and memory usage, this method should be called when the owning
	 * application or PEM stops.
	 */
	public void stop();






	/**
	 * Create a view of the configuration. A configuration view is used by client objects that wish
	 * to read or to be notified of configuration updates.
	 * 
	 * Right after a call to createView(), the returned ConfigurationView object contains a
	 * configuration and the given Configurable object has already been notified through its
	 * configurationUpdate() method.
	 * 
	 * @param configurable
	 *            a configurable object that need to be notified when changes occurs in
	 *            configuration.
	 * 
	 * @param name
	 *            a name for the view. This name is use to enhance logging information only.
	 * 
	 * @throws ConfigurationException
	 *             if configuration contains values that are invalid for the given Configurable
	 *             object.
	 * 
	 * @return a configuration view
	 */
	public ConfigurationView
		createView(Configurable configurable, String name) throws ConfigurationException;






	/**
	 * Release a view of the configuration. When a client object is no more interested by monitoring
	 * configuration updates, it should release its configuration view for better overall
	 * performance and memory usage.
	 * 
	 * @param view
	 *            the view to release.
	 */
	public void releaseView(ConfigurationView view);
}
