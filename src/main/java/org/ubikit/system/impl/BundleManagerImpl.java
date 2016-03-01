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

package org.ubikit.system.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.system.BundleManager;
import org.ubikit.system.ExtensionManagerService.InstallationStatus;



/**
 * Thread-safe
 * 
 * @author lionel
 *
 */
public class BundleManagerImpl implements BundleManager
{
	private enum BundleControlOperation
	{
		START,
		STOP,
		UNINSTALL
	}

	private static BundleManager INSTANCE = null;
	private BundleContext bundleContext;

	final Logger logger = LoggerFactory.getLogger(BundleManagerImpl.class);






	public static BundleManager getInstance(BundleContext bundleContext)
	{
		if (INSTANCE == null)
		{
			INSTANCE = new BundleManagerImpl(bundleContext);
		}

		return INSTANCE;
	}






	private BundleManagerImpl(BundleContext bundleContext)
	{
		this.bundleContext = bundleContext;
	}






	@Override
	public synchronized boolean start(long bundleUID)
	{
		return controlApp(BundleControlOperation.START, bundleUID);
	}






	@Override
	public synchronized boolean stop(long bundleUID)
	{
		boolean res = controlApp(BundleControlOperation.STOP, bundleUID);

		logger.debug("End of stop(bundleUID): res={}", res);

		return res;
	}






	@Override
	public synchronized boolean uninstall(long bundleUID)
	{
		return controlApp(BundleControlOperation.UNINSTALL, bundleUID);
	}






	@Override
	public synchronized InstallationStatus install(long bundleUID, String bundleURL)
	{
		return InstallationStatus.FAILED; // Not yet implemented
	}






	private boolean controlApp(BundleControlOperation op, long bundleUID)
	{
		try
		{
			Bundle bundle = bundleContext.getBundle(bundleUID);

			if (bundle != null)
			{
				int bundleState = bundle.getState();
				switch (op)
				{
					case START:
						if (bundleState == Bundle.INSTALLED
							|| bundleState == Bundle.RESOLVED)
						{

							logger.debug("Starting bundle {}", bundleUID);

							bundle.start();
							return true;
						}

						if (logger.isDebugEnabled())
						{
							logger.debug(
								"Cannot start bundle {}: current bundle status is {}",
								bundleUID,
								displayBundleState(bundleState));
						}
						return false;

					case STOP:
						if (bundleState == Bundle.ACTIVE)
						{
							if (logger.isDebugEnabled())
							{
								logger.debug("Stopping bundle {}", bundleUID);
							}
							bundle.stop();
							return true;
						}

						if (logger.isDebugEnabled())
						{
							logger.debug(
								"Cannot stop bundle {}: current bundle status is {}",
								bundleUID,
								displayBundleState(bundleState));
							logger.debug("controlApp() returning false.");
						}
						return false;

					case UNINSTALL:
						if (bundleState == Bundle.INSTALLED
							|| bundleState == Bundle.RESOLVED)
						{
							logger.debug("Uninstalling bundle {}", bundleUID);

							bundle.uninstall();
							return true;
						}

						if (logger.isDebugEnabled())
						{
							logger.debug(
								"Cannot uninstall bundle {}: current bundle status is {}",
								bundleUID,
								displayBundleState(bundleState));
						}
						return false;

					default:
						logger.error("Unmanaged app control operation: {} (operation requested "
							+ "for bundle ID {})", op, bundleUID);
						break;
				}
			}
			else
			{
				logger.error(
					"Trying control a bundle that does NOT actually exists (bundle ID {})",
					bundleUID);
			}
		}
		catch (BundleException e)
		{
			logger.error("While trying to {} bundle {}", op, bundleUID, e);
		}

		return false;
	}






	private static String displayBundleState(int bundleState)
	{
		switch (bundleState)
		{
			case Bundle.INSTALLED:
				return "INSTALLED";
			case Bundle.RESOLVED:
				return "RESOLVED";
			case Bundle.STARTING:
				return "STARTING";
			case Bundle.ACTIVE:
				return "ACTIVE";
			case Bundle.STOPPING:
				return "STOPPING";
			case Bundle.UNINSTALLED:
				return "UNINSTALLED";
		}

		return "UNKNOWN";
	}
}
