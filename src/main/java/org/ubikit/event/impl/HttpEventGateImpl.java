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

package org.ubikit.event.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventInstanciator;
import org.ubikit.event.HttpEventGate;
import org.ubikit.system.impl.Network;

import com.google.gson.Gson;

public class HttpEventGateImpl extends EventGateImpl implements HttpEventGate
{	
	private final String hostName;
	private final EventInstanciator eventInstanciator;
	private final Gson gson = new Gson();
	private final HttpService httpService;
	
	final Logger logger = LoggerFactory.getLogger(HttpEventGateImpl.class);
	
	private final HttpServlet servlet = new HttpServlet() 
	{
		private static final long serialVersionUID = -4663359837836146218L;

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
		{
			String eventClassName = req.getContentType().replace("application/vnd.", "").replace(";charset=utf-8", "");
			
			StringBuffer sb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = req.getReader();
				while ((line = reader.readLine()) != null) {
					logger.debug(line);
					sb.append(line);
				}
			} 
			catch (IOException e) 
			{
				sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot get event data");
				return;
			}
			
			AbstractEvent event = eventInstanciator.instanciateEvent(eventClassName, sb.toString());
			if(event != null)
			{
				logger.info("Incoming message from {}: {}",event.getSenderNetworkAddress(), eventClassName);
				HttpEventGateImpl.this.postLocalEvent(event);
			}
			else
			{
				logger.warn("{}: The received event cannot be instanciated", getName());
			}
		}
		
		private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException
		{
			resp.setStatus(statusCode);
			resp.getWriter().write(message);
			resp.getWriter().close();
		}
	};
	
	public HttpEventGateImpl(HttpService httpService, EventInstanciator eventInstanciator, String name, Mode mode) throws NamespaceException
	{
		super(name);
		
		if(httpService == null) throw new IllegalArgumentException("httpService cannot be null.");
		if(eventInstanciator == null) throw new IllegalArgumentException("eventInstanciator cannot be null.");
		if(name == null) throw new IllegalArgumentException("name cannot be null.");
		if(mode == null) throw new IllegalArgumentException("mode cannot be null.");
		
		this.eventInstanciator = eventInstanciator;
		this.httpService = httpService;
		
		String hostName = null;
		switch(mode)
		{
			case DNS:
				hostName = org.ubikit.system.impl.Network.getLocalHostName()+".local";
				break;
			case IPv4:
			case IPv6:
				List<InetAddress> addresses = Network.getInetAddresses();
				Iterator<InetAddress> itr = addresses.listIterator();
				while(itr.hasNext())
				{
					InetAddress a = itr.next();
					if(mode == Mode.IPv4 && a instanceof Inet4Address)
					{
						hostName = a.getHostAddress();
						break;
					}
					else if(mode == Mode.IPv6 && a instanceof Inet6Address)
					{
						hostName = a.getHostAddress();
						break;
					}
				}
				break;
		}
		
		hostName += ":"+Network.getHttpPort();
		
		this.hostName = hostName;
		
		try 
		{
			httpService.registerServlet(HttpEventGate.eventGateBaseURL+name, servlet, null, null);
		} 
		catch (ServletException e) 
		{
			logger.error("{}: Cannot register the HTTP event gate servlet", getName());
		}
	}
	
	public void terminate()
	{
		httpService.unregister(HttpEventGate.eventGateBaseURL+getName());
	}
	
	@Override
	public int postHttpEvent(URL eventGateURL, AbstractEvent event) throws IOException 
	{
		HttpURLConnection connection = null;
		((AbstractEventImpl)event).setSenderNetworkAddress(hostName);
		
		try
		{
			connection = (HttpURLConnection) eventGateURL.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/vnd."+event.getClass().getName()+";charset=utf-8");
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			
			OutputStream output = null;
			try 
			{
				output = connection.getOutputStream();
				output.write(gson.toJson(event).getBytes("UTF-8"));
			} 
			finally 
			{
				if (output != null) 
					try { output.close(); } catch (IOException e) {}
			}
			
			// Get response code.
			int code = connection.getResponseCode();
			
			if (code != 200) 
			{
				logger.warn("{}: The distant event gate answered with a {} response code.", getName(), code);
			}
			
			return code;
		}
		finally
		{
			if (connection != null)
				connection.disconnect();
		}
	}
}
