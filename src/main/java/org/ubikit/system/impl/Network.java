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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Network 
{
	private static NetworkInterface eth0 = null;
	private static String hostName = null;
	private static int httpPort = 0;
	
	final static Logger logger = LoggerFactory.getLogger(Network.class);
	
	public static synchronized NetworkInterface discoverLocalNetworkInterface()
	{
		try 
		{
			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			while(netInterfaces.hasMoreElements())
			{
				NetworkInterface ni = netInterfaces.nextElement();

				if(!ni.isLoopback() && !ni.isVirtual() && !ni.isPointToPoint() && ni.isUp())
				{
					Enumeration<InetAddress> addresses = ni.getInetAddresses();
					while(addresses.hasMoreElements())
					{
						InetAddress a = addresses.nextElement();
						if(a.isSiteLocalAddress()) {
							eth0 = ni;
							return eth0;
						}
					}
				}
			}
		}
		catch (SocketException e) 
		{
			logger.error("[Network] The LAN network interface could NOT be found.", e);
		}
		
		return eth0;
	}
	
	public static synchronized NetworkInterface getLocalNetworkInterface()
	{
		if(eth0 == null) {
			discoverLocalNetworkInterface();
		}
		
		return eth0;
	}
	
	public static synchronized List<InetAddress> getInetAddresses()
	{
		List<InetAddress> res = new ArrayList<InetAddress>();
		NetworkInterface ni = getLocalNetworkInterface();
		if(ni != null)
		{
			Enumeration<InetAddress> addresses = ni.getInetAddresses();
			while(addresses.hasMoreElements())
			{
				res.add(addresses.nextElement());
			}
		}
		
		return res;
	}
	
	public static synchronized String getLocalHostName()
	{
		if(hostName == null)
		{
			try 
			{
				hostName = InetAddress.getLocalHost().getHostName();
				int firstDotPosition = hostName.indexOf('.');
				if(firstDotPosition > 0) 
				{
					hostName = hostName.substring(0, firstDotPosition); // because some OS or router with embedded DNS provide a fully qualified hostname.
				}
			} 
			catch (UnknownHostException e) 
			{
				logger.error("[Network] Cannot determine the host name.", e);
			}
		}
		
		return hostName;
	}
	
	static void setHttpPort(int httpPort)
	{
		Network.httpPort = httpPort;
	}
	
	public static int getHttpPort()
	{
		return httpPort;
	}
}
