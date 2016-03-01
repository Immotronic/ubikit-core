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



/**
 * This interface specify a configurable object. A configurable object is notified when a
 * configuration update occurs. Then, it is expected that this configurable object reconfigure
 * itself according to the new configuration state.
 * 
 * To be notified, a configuration observer need to be associated to a ConfigurationView. This
 * association must be setup through ConfigurationManager.createView() method.
 * 
 * @author Lionel Balme <lbalme@immotronic.fr>
 *
 */
public interface Configurable
{
	/**
	 * This methods is called when a configuration update occurs.
	 * 
	 * @param updatedProperties
	 *            A set of identifiers of configuration properties that have been modified in the
	 *            last configuration update.
	 * 
	 * @throws ConfigurationException
	 *             if one of new property values is invalid, then the implementation of this method
	 *             SHOULD throws a ConfigurationException with a message that explains which value
	 *             is invalid and why.
	 */
	public void
		configurationUpdate(Enum<?>[] updatedProperties, ConfigurationView view) throws ConfigurationException;
}
