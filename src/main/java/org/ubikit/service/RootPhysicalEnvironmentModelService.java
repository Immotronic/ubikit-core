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

package org.ubikit.service;

import org.ubikit.PhysicalEnvironmentItem;

public interface RootPhysicalEnvironmentModelService extends PhysicalEnvironmentModelService
{
	enum HardwareLinkStatus
	{
		NOT_APPLICABLE,
		UNKNOWN,
		DISCONNECTED,
		CONNECTED
	};
	
	/**
	 * Return the model base URL for its web services and content. 
	 * 
	 * @return a string that contains the base URL of this PEM. This URL ends with a slash ("/") and is ready to be concatenated.
	 */
	public String getBaseURL();
	
	/**
	 * Add a physical environment item to the model. A such item models a device like a 
	 * sensor or an actuator (or a device that is both at the same time).
	 * 
	 *  @param item a physical environment item object
	 */
	public void addItem(PhysicalEnvironmentItem item);
	
	/**
	 * Remove a physical environment item from the model given its unique ID and return 
	 * it, if this item exist in the model. Otherwise, do nothing and return null. 
	 * 
	 * @param itemUID a unique item identifier
	 * @return a physical environment item or null
	 */
	public PhysicalEnvironmentItem removeItem(String itemUID);
	
	/**
	 * Clear the whole list of physical environment items.
	 */
	public void clearItems();
	
	/**
	 * Return the current status of the link between a root PEM and the hardware. 
	 * 
	 * @return Non-root PEM returns HardwareLinkStatus.NOT_APPLICABLE. Given the hardware link status, root PEM returns UNKNOWN, DISCONNECTED or CONNECTED.
	 */
	public HardwareLinkStatus getHardwareLinkStatus();
}
