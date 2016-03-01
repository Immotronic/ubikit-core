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

import java.util.Collection;

import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.PhysicalEnvironmentModelObserver;
import org.ubikit.event.EventGate;

public interface PhysicalEnvironmentModelService 
{
	enum HardwareLinkStatus
	{
		NOT_APPLICABLE,
		UNKNOWN,
		DISCONNECTED,
		CONNECTED
	};
	
	/**
	 * Connect the given model event listener to this model service. This method is used by
	 * higher abstraction models (if any) and the Ubikit core.
	 * 
	 * @param eventGate the EventGate object of the entity that wish to listener events from 
	 * this model.
	 */
	public void linkTo(EventGate eventGate);
	
	/**
	 * Disconnect the given model event listener from this model service.
	 * 
	 * @param eventGatethe EventGate object of the entity that wish to stop listening events
	 * from this model.
	 */
	public void unlink(EventGate eventGate);
	
	/**
	 * Return the model unique identifier
	 * 
	 * @return a model unique identifier.
	 */
	public String getUID();
	
	/**
	 * Get a physical environment item by its UID.
	 * BE CAREFUL : long term reference onto a PhysicalEnvironmentItem instance MUST NEVER BE KEPT : These instances are managed
	 * by the PEM. They can be removed at any time. Keeping a reference on them could lead to unexpected behavior.
	 * 
	 * @param itemUID a unique item identifier
	 * 
	 * @return a physical environment item if itemUID match an existing item, null otherwise.
	 */
	public PhysicalEnvironmentItem getItem(String itemUID);
	
	/**
	 * Return the whole list of physical environment items.
	 * BE CAREFUL : long term reference onto items from that collection MUST NEVER BE KEPT : Items in that collection are managed 
	 * by the PEM. They can be removed at any time. Keeping a reference on them could lead to unexpected behavior.
	 * 
	 * @return a collection of physical environment items
	 */
	public Collection<PhysicalEnvironmentItem> getAllItems();
	
	/**
	 * Give some diagnostic informations (for instance: statistics, running properties, etc.) about the physical environment model.
	 * @return A object that implements PhysicalEnvironmentModelInformations interface.
	 */
	public PhysicalEnvironmentModelInformations getInformations();
	
	/**
	 * This method is used to give the reference to the diagnostic observer for this physical environment model
	 * 
	 *  @param observer a reference onto a PhysicalEnvironmentModelObserver instance.
	 */
	public void setObserver(PhysicalEnvironmentModelObserver observer);
}
