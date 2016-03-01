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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/*
 * TODO: Replace this LogWriter by directly listening events from the framework, bundles and
 * services and log them using SFL4J. See implementation of Felix Log Service to code samples.
 */

@Component
@Provides
@Instantiate
public class LogWriterComponentImpl implements LogListener
{
	@Requires
	LogReaderService reader;

	final Logger logger = LoggerFactory.getLogger("org.osgi");






	@Validate
	public void start()
	{
		reader.addLogListener(this);
	}






	@Override
	public void logged(LogEntry entry)
	{
		switch (entry.getLevel())
		{
			case LogService.LOG_DEBUG:
				if (logger.isDebugEnabled())
				{
					logger.debug(
						"[Bundle {}] {}",
						entry.getBundle().getSymbolicName(),
						entry.getMessage());
				}
				break;
			case LogService.LOG_ERROR:
				logger.error(
					"[B: {}, S: {}] {}",
					entry.getBundle().getSymbolicName(),
					entry.getServiceReference().toString(),
					entry.getMessage(),
					entry.getException());
				break;
			case LogService.LOG_INFO:
				logger.info(
					"[B: {}, S: {}] {}",
					entry.getBundle().getSymbolicName(),
					entry.getServiceReference().toString(),
					entry.getMessage());
				break;
			case LogService.LOG_WARNING:
				logger.warn(
					"[B: {}, S: {}] {}",
					entry.getBundle().getSymbolicName(),
					entry.getServiceReference().toString(),
					entry.getMessage(),
					entry.getException());
				break;
		}
	}
}
