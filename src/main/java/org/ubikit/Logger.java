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

import java.text.SimpleDateFormat;

public final class Logger 
{
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS | ");
	private static final String DEBUG =		" // DEBUG // [";
	private static final String INFO =		"     INFO    [";
	private static final String WARNING =	" # WARNING   [";
	private static final String ERROR =		" ## ERROR ## [";
	private static final String SEP_1 = "]-[";
	private static final String SEP_2 = "]: ";
	private static final String EXCEPTION = " (Exception stack below)";
	
	public enum Level
	{
		DEBUG,
		INFO,
		WARNING,
		ERROR
	}
	
	public static void debug(LoggerConfigurator lc, Object object, String message)
	{
		log(lc, Level.DEBUG, object, message, null);
	}
	
	public static void info(LoggerConfigurator lc, Object object, String message)
	{
		log(lc, Level.INFO, object, message, null);
	}
	
	public static void warn(LoggerConfigurator lc, Object object, String message)
	{
		log(lc, Level.WARNING, object, message, null);
	}
	
	public static void warn(LoggerConfigurator lc, Object object, String message, Exception exception)
	{
		log(lc, Level.WARNING, object, message, exception);
	}
	
	public static void error(LoggerConfigurator lc, Object object, String message)
	{
		log(lc, Level.ERROR, object, message, null);
	}
	
	public static void error(LoggerConfigurator lc, Object object, String message, Exception exception)
	{
		log(lc, Level.ERROR, object, message, exception);
	}
	
	private static void log(LoggerConfigurator lc, Level level, Object object, String message, Exception exception)
	{
		if(lc == null || level == null) throw new IllegalArgumentException("When calling Logger.log(), LoggerConfigurator and Level params MUST NOT be null");
		
		if(message != null)
		{
			StringBuffer sb = null;
			
			switch(level)
			{
				case DEBUG:
					if(lc.debug())
					{
						sb = new StringBuffer(dateFormat.format(System.currentTimeMillis()));
						sb.append(DEBUG);
						sb.append(lc.bundleName());
						sb.append(SEP_1);
						if(object != null) {
							sb.append(object.getClass().getSimpleName());
							sb.append("@");
							sb.append(Integer.toHexString(System.identityHashCode(object)));
						}
						
						sb.append(SEP_2);
						sb.append(message);
						System.err.println(sb);
					}
					break;
					
				default:
					sb = new StringBuffer(dateFormat.format(System.currentTimeMillis()));
					
					switch(level)
					{
						case INFO: sb.append(INFO); break;
						case WARNING: sb.append(WARNING); break;
						case ERROR: sb.append(ERROR); break;
						default: break;
					}
					
					sb.append(lc.bundleName());
					sb.append(SEP_1);
					
					if(object != null) {
						sb.append(object.getClass().getSimpleName());
						sb.append("@");
						sb.append(Integer.toHexString(System.identityHashCode(object)));
					}
					
					sb.append(SEP_2);
					sb.append(message);
					
					if(exception != null) {
						sb.append(EXCEPTION);
					}
					
					System.err.println(sb);
					if(exception != null) {
						exception.printStackTrace(System.err);
					}
					break;
			}
		}
	}
}
