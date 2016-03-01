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

package org.ubikit.tools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

public final class BundleResourceUtil 
{
	public static byte[] getResourceBinaryContent(URL resourceURL) throws IOException
	{
		DataInputStream dis = null;
		ArrayList<Byte> bytes = null;
		
		try
		{
			dis = new DataInputStream(resourceURL.openStream());
			bytes = new ArrayList<Byte>(3072); // 3ko of initial size
			while (true) 
			{
				bytes.add(dis.readByte());
			}	
		}
		finally
		{
			if(dis != null) {
				try 
				{
					dis.close();
				} 
				catch (IOException e) 
				{ }
			}
			
			if(bytes != null)
			{
				int size = bytes.size();
				if(size > 0)
				{
					int offset = 0;
					byte[] bytes_a = new byte[size];
					Iterator<Byte> it = bytes.iterator();
					while(it.hasNext())
					{
						bytes_a[offset] = it.next().byteValue();
						offset++;
					}
					
					return bytes_a;
				}
			}
		}
	}
	
	public static JSONObject getResourceJSONContent(URL resourceURL) throws IOException, JSONException 
	{
		JSONObject res = null;  
		BufferedReader br = null;
		
		try
        {
			String rawData = "";
			
			br = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "UTF-8"));
			for (String s = br.readLine(); s != null; s = br.readLine())
			{
				rawData += s;
			}
			
			res = new JSONObject(rawData);
        }
		finally
		{
			if (br != null) {
				br.close();
			}
		}
		
		return res;
	}
	
	public static String getResourceTextualContent(URL resourceURL) throws IOException
	{
		String res = null;  
		BufferedReader br = null;
		
		try
        {
			String rawData = "";
			br = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "UTF-8"));
			for (String s = br.readLine(); s != null; s = br.readLine())
			{
				rawData += s;
			}
			
			res = rawData;
        }
		finally
		{
			if (br != null) {
				br.close();
			}
		}
		
		return res;
	}
	
	private BundleResourceUtil()
	{ 
		throw new AssertionError();
	}
}
