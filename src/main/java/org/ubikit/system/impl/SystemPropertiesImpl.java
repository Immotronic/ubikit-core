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

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.system.SystemProperties;
import org.ubikit.tools.BundleResourceUtil;



/**
 * Compute and serve system properties such as uptime, version number, build UID, build date and
 * ubikit host name.
 * 
 * This class is thread-safe.
 * 
 * @author lionel
 *
 */
public final class SystemPropertiesImpl implements SystemProperties
{
	private static final String BUILD_INFO_FILENAME = "/build.info.json";
	private static SystemPropertiesImpl INSTANCE = null;

	private NetworkInterface eth0 = null;
	private String systemVersion = null;
	private String systemBuildID = null;
	//private String systemBuildDate = null;
	private long systemStartDate;
	private String hostname = null;
	private String hostAddresses = null;

	final Logger logger = LoggerFactory.getLogger(SystemPropertiesImpl.class);






	/**
	 * Return the singleton SystemProperties instance.
	 * 
	 * @param bundleContext
	 *            the BundleContext object of the Ubikit-Core OSGi bundle.
	 * 
	 * @return An object that implements the SystemProperties interface
	 */
	public static SystemProperties getInstance(BundleContext bundleContext)
	{
		if (INSTANCE == null)
		{
			INSTANCE = new SystemPropertiesImpl(bundleContext);
		}

		return INSTANCE;
	}






	public static void clearInstance()
	{
		INSTANCE = null;
	}






	private SystemPropertiesImpl(BundleContext bundleContext)
	{
		// Memorize current time at instance creation. This time reference will be used to compute
		// the system up time
		systemStartDate = System.currentTimeMillis();

		Network.setHttpPort(Integer.parseInt(bundleContext
			.getProperty("org.osgi.service.http.port")));

		try
		{
			// Read the build info file and extract system version number, build unique id and build
			// date.
			JSONObject buildInfo = BundleResourceUtil.getResourceJSONContent(bundleContext
				.getBundle()
				.getEntry(BUILD_INFO_FILENAME));
			systemVersion = buildInfo.getString("version");
			systemBuildID = buildInfo.getString("build");
			//systemBuildDate = buildInfo.getString("build_date");
		}
		catch (IOException e)
		{
			logger.error(
				"Build Information were NOT found. Check if JAR contains {} file at its root.",
				BUILD_INFO_FILENAME,
				e);
		}
		catch (JSONException e)
		{
			logger
				.error(
					"Build Information file does NOT contain valid JSON data. Check the {} file content.",
					BUILD_INFO_FILENAME,
					e);
		}

		// Determine the ubikit host name & ip address
		hostname = Network.getLocalHostName();
		eth0 = Network.discoverLocalNetworkInterface();
	}






	@Override
	public long getSystemUptime()
	{
		return (System.currentTimeMillis() - systemStartDate);
	}






	@Override
	public String getSystemVersion()
	{
		return systemVersion;
	}






	@Override
	public String getSystemBuildID()
	{
		return systemBuildID;
	}






	/*@Override
	public String getSystemBuildDate()
	{
		return systemBuildDate;
	}*/






	@Override
	public String getSystemHostname()
	{
		return hostname;
	}






	@Override
	public String getSystemHostAddresses()
	{
		try
		{
			if (eth0 != null)
			{
				if (eth0.isUp())
				{
					if (hostAddresses == null)
					{
						String res = "";
						Enumeration<InetAddress> addresses = eth0.getInetAddresses();
						while (addresses.hasMoreElements())
						{
							res += addresses.nextElement().getHostAddress()
								+ "\n";
						}

						if (!res.isEmpty())
						{
							hostAddresses = res;
						}
						else
						{
							hostAddresses = null;
						}
					}
				}
				else
				{
					hostAddresses = null;
				}
			}
			else
			{
				hostAddresses = null;
			}
		}
		catch (SocketException e)
		{
			logger.warn("Exception while checking network", e);
		}

		return hostAddresses;
	}






	@Override
	public String getSystemHostAddresse()
	{
		try
		{
			if (eth0 != null)
			{
				if (eth0.isUp())
				{
					if (hostAddresses == null)
					{
						Enumeration<InetAddress> addresses = eth0.getInetAddresses();
						while (addresses.hasMoreElements())
						{
							InetAddress inetaddress = addresses.nextElement();
							if (inetaddress.isSiteLocalAddress())
							{
								return inetaddress.getHostAddress();
							}
						}
					}
				}
			}
		}
		catch (SocketException e)
		{
			logger.warn("Exception while checking network", e);
		}

		return null;
	}






	@Override
	public int getSystemHTTPPort()
	{
		return Network.getHttpPort();
	}






	@Override
	public boolean isNetworkUp()
	{
		if (eth0 == null)
		{
			eth0 = Network.discoverLocalNetworkInterface();
		}

		try
		{
			if (eth0 != null
				&& eth0.isUp())
			{
				return true; // Network interface is up and an IP address has been assigned.
			}
		}
		catch (SocketException e)
		{
			logger.warn("Exception while checking network", e);
		}

		eth0 = null;
		hostAddresses = null;
		return false;
	}






	/**
	 * Returns a String filled with all Ethernet addresses of the machine separated by an
	 * underscore.
	 * 
	 * @return a String filled with all Ethernet addresses of the machine separated by an
	 *         underscore.
	 * @throws SocketException
	 *             if network interfaces can not be acceded.
	 */
	@Override
	public String getSystemEthernetAddress()
	{
		try
		{
			StringBuffer sbAltInterfaces = new StringBuffer();

			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

			if (nis != null)
			{
				int nb_mac = 0;

				while (nis.hasMoreElements()
					&& nb_mac < 3)
				{
					NetworkInterface ni = nis.nextElement();

					byte[] mac = ni.getHardwareAddress();
					if (mac != null)
					{
						if (nb_mac != 0) sbAltInterfaces.append("_");

						for (byte b : mac)
						{
							sbAltInterfaces.append(String.format("%02X", b));
						}
						nb_mac++;
					}
				}

				if (logger.isDebugEnabled())
				{
					logger.debug("read MAC addresses: {}", sbAltInterfaces.toString());
				}
				return sbAltInterfaces.toString();
			}
		}
		catch (SocketException e)
		{
			logger.error("While getting Ethernet addresses of the system.", e);
		}

		return null;
	}
}
