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


import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.event.EventGate;
import org.ubikit.event.impl.EventGateImpl;
import org.ubikit.impl.DatabaseProxyImpl;
import org.ubikit.impl.PhysicalEnvironmentModelManagerImpl;
import org.ubikit.service.HSQLDatabaseService;
import org.ubikit.service.PemRegistryService;
import org.ubikit.service.PhysicalEnvironmentModelService;

public abstract class AbstractPhysicalEnvironmentModel implements PhysicalEnvironmentModelService
{
	/*
	 * ___NOTICEABLE_CONSTANTS_____________________________________________________________________
	 */
	
	/*
	 * ___EXTERNAL_DEPENDENCIES__________________________________________________________________
	 */
	
	private HSQLDatabaseService hsqlDatabaseService;
	private PemRegistryService pemRegistryService;
	
	/*
	 * ___INTERNAL_CLASSES_&_MEMBERS_______________________________________________________________
	 */
	private final String UID; // UID is automatically built from model package name (without the last .impl or .priv part)
	private boolean pemDidValidate;
	private boolean pemDidStart;
	
	private final long bundleUID;
	
	private final EventGateImpl higherAbstractionLevels;
	
	private final Map<String, PhysicalEnvironmentItem> items;
	private final int executorCorePoolSize;
	
	private ScheduledExecutorService executorService;
	private DatabaseProxyImpl databaseProxy;
	
	private boolean isConfiguredForEmbeddedSystem;

	private PhysicalEnvironmentModelManagerImpl physicalEnvironmentModelManager;
	
	final Logger logger = LoggerFactory.getLogger(AbstractPhysicalEnvironmentModel.class);
	
	/*
	 * ___PUBLIC_METHODS___________________________________________________________________________
	 */
	
	/**
	 * Construct a Model object given a model unique identifier and a list of supported 
	 * lower abstraction models unique ID.
	 * 
	 * @param supportedSourceModelUIDs a list of model unique identifiers.
	 */
	public AbstractPhysicalEnvironmentModel(int threadCorePoolSize, BundleContext bundleContext, String[] supportedSourceModelUIDs)
	{
		pemDidValidate = false;
		pemDidStart = false;
		databaseProxy = null;
		isConfiguredForEmbeddedSystem = Boolean.parseBoolean(bundleContext.getProperty("org.ubikit.embedded"));
		
		// Build the PEM unique identifier
		String packageName = this.getClass().getPackage().getName();
		UID = packageName.substring(0, packageName.lastIndexOf("."));
		
		physicalEnvironmentModelManager = new PhysicalEnvironmentModelManagerImpl(supportedSourceModelUIDs, UID+"_ll");
		
		higherAbstractionLevels = new EventGateImpl(UID+"_hl");
		items = Collections.synchronizedMap(new HashMap<String, PhysicalEnvironmentItem>());
		executorCorePoolSize = threadCorePoolSize + 2; // because higherAbstractionLevels & lowerAbstractionModels eventGate will use one thread each in the executorService.
		bundleUID = bundleContext.getBundle().getBundleId();		
	}

	// ---------------------------------------------------------------------------
	
	public void validate(PemRegistryService pemRegistryService, HSQLDatabaseService hsqlDatabaseService)
	{
		this.pemRegistryService = pemRegistryService;
		this.hsqlDatabaseService = hsqlDatabaseService;
		pemDidValidate = true;
		
		try 
		{
			startPemIfFeasible();
		}
		catch(Exception e)
		{
			logger.error("###### ERROR WHILE STARTING A PEM, during @Validate operation #######", e);
		}
	}
	
	// ---------------------------------------------------------------------------
	
	private void startPemIfFeasible() throws Exception
	{
		if(pemDidValidate 
				&& physicalEnvironmentModelManager.allRequiredDependenciesResolved() 
				&& !pemDidStart)
		{
			pemDidStart = true;
			executorService = Executors.newScheduledThreadPool(executorCorePoolSize);
			executorService.execute(higherAbstractionLevels.getEventDelivererTask());
			executorService.execute(physicalEnvironmentModelManager.getEventGate().getEventDelivererTask());
			
			start();
			pemRegistryService.pemDidStart(bundleUID);
		}
	}
	
	// ---------------------------------------------------------------------------
	
	public void invalidate()
	{
		stopPemIfNecessary();
		pemDidValidate = false;
	}
	
	// ---------------------------------------------------------------------------
	
	private void stopPemIfNecessary()
	{
		if(pemDidStart)
		{
			pemDidStart = false;
			stop();
			
			for(PhysicalEnvironmentItem item : items.values())
			{
				AbstractPhysicalEnvironmentItem i = (AbstractPhysicalEnvironmentItem) item;
				i.terminate();
			}
			items.clear();
			
			physicalEnvironmentModelManager.unlinkAll();
			higherAbstractionLevels.unlinkAll();
			
			if(executorService != null)
			{
				if(logger.isDebugEnabled()) {
					logger.debug("Waiting for PEM threads to terminate...");
				}
				executorService.shutdownNow();
				try 
				{
					executorService.awaitTermination(5, TimeUnit.SECONDS);
					executorService = null;
					
					if(logger.isDebugEnabled()) {
						logger.debug("PEM threads have terminated...");
					}
				} 
				catch (InterruptedException e) 
				{
					logger.error("PEM termination was interrupted while stopping PEM threads", e);
				}
			}
			
			stop();
			
			if(pemRegistryService != null) {
				pemRegistryService.pemDidStop(bundleUID);
			}
			
			if(databaseProxy != null) {
				databaseProxy.closeDatabaseConnection();
			}
		}
	}
	
	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	@Override
	public void linkTo(EventGate eventGate)
	{
		higherAbstractionLevels.linkTo(eventGate);
	}

	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	@Override
	public void unlink(EventGate eventGate)
	{
		higherAbstractionLevels.unlink(eventGate);
	}

	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	@Override
	public String getUID()
	{
		return UID;
	}

	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	public PhysicalEnvironmentItem removeItem(String itemUID)
	{
		AbstractPhysicalEnvironmentItem item = (AbstractPhysicalEnvironmentItem) items.remove(itemUID);
		item.terminate();
		return item;
	}

	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	public void clearItems()
	{
		items.clear();
	}

	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	@Override
	public PhysicalEnvironmentItem getItem(String itemUID)
	{
		return items.get(itemUID);
	}

	// ---------------------------------------------------------------------------

	/** 
	 * @See PhysicalEnvironmentModelService Interface description
	 */
	@Override
	public Collection<PhysicalEnvironmentItem> getAllItems()
	{
		return items.values();
	}
	
	// ---------------------------------------------------------------------------	

	/**
	 * Add a newly created physical environment item to the item list.
	 * 
	 * @param item a physical environment item object
	 */
	public void addItem(PhysicalEnvironmentItem item)
	{
		items.put(item.getUID(), item);
	}
	
	// ---------------------------------------------------------------------------

	/**
	 * Post a given event to lower abstraction physical environment models.
	 *  
	 * @param event a physical environment model event object.
	 */
	public EventGate getLowerAbstractionLevelEventGate()
	{
		return physicalEnvironmentModelManager.getEventGate();
	}
	
	// ---------------------------------------------------------------------------

	/**
	 * Notify physical environment model listeners with an event.
	 * 
	 * @param event a physical environment model event object.
	 */
	public EventGate getHigherAbstractionLevelEventGate()
	{
		return higherAbstractionLevels;
	}
	

	/*
	 * ___PROTECTED_METHODS___________________________________________________________________________
	 */

	protected DatabaseProxy getDatabaseConnection() throws SQLException
	{
		if(databaseProxy == null || databaseProxy.isClosed() || !databaseProxy.isValid()) {
			databaseProxy = new DatabaseProxyImpl(hsqlDatabaseService.create(UID, HSQLDatabaseService.Type.PEM), isConfiguredForEmbeddedSystem); 
		}
		
		return databaseProxy;
	}
	
	// ---------------------------------------------------------------------------
	
	protected ScheduledExecutorService getExecutorService()
	{
		return executorService;
	}

	// ---------------------------------------------------------------------------
	
	protected abstract void start() throws Exception;
	protected abstract void stop();

	// ---------------------------------------------------------------------------
	
	protected PhysicalEnvironmentModelService getPhysicalEnvironmentModel(String modelUID)
	{
		return physicalEnvironmentModelManager.getModel(modelUID);
	}

	// ---------------------------------------------------------------------------
	
	public synchronized void bindPhysicalEnvironmentModelService(PhysicalEnvironmentModelService model)
	{
		boolean b = physicalEnvironmentModelManager.bind(model);
		
		if(logger.isDebugEnabled()) { if(b) {
			logger.debug("{} was required by & binded to {}", model.getUID(), getUID());
		}}
		
		if(physicalEnvironmentModelManager.allRequiredDependenciesResolved())
		{
			try {
				startPemIfFeasible();
			}
			catch(Exception e)
			{
				logger.error("###### ERROR WHILE STARTING A PEM, during @bindPhysicalEnvironmentModelService operation#######", e);
			}
		}
	}

	// ---------------------------------------------------------------------------
	
	public synchronized void unbindPhysicalEnvironmentModelService(PhysicalEnvironmentModelService model)
	{
		boolean b = physicalEnvironmentModelManager.unbind(model);
		
		if(logger.isDebugEnabled()) { if(b) {
			logger.debug("{} was unbinded from {}", model.getUID(), getUID());
		}}
		
		if(!physicalEnvironmentModelManager.allRequiredDependenciesResolved())
		{
			stopPemIfNecessary();
		}
	}
}
