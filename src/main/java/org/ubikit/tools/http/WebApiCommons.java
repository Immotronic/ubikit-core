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

package org.ubikit.tools.http;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebApiCommons 
{	
	final static Logger logger = LoggerFactory.getLogger(WebApiCommons.class);
	
	public final class Errors 
	{
		public static final int not_yet_implemented = 0;
		public static final int invalid_credentials = 1;
		public static final int internal_error = 2;
		public static final int malformed_json_string = 3;
		public static final int invalid_query = 4; // query does NOT contains valid parameters and values or some required parameters are missing.
		public static final int unknown_app = 5;
		public static final int app_control_error = 6;
	}
	
	public static String errorMessage(int error_code)
	{
		String err_id;
		switch(error_code) {
		
		case Errors.invalid_credentials:
			err_id = "err_invalid_credentials";
			break;
		case Errors.internal_error:
			err_id = "err_internal_error";
			break;
		case Errors.malformed_json_string:
			err_id = "err_malformed_json_string";
			break;
		case Errors.not_yet_implemented:
			err_id = "err_not_yet_implemented";
			break;
		case Errors.invalid_query:
			err_id = "err_invalid_query";
			break;
		case Errors.unknown_app:
			err_id = "err_unknown_app";
			break;
		case Errors.app_control_error:
			err_id = "err_app_control";
			break;
		default:
			err_id = "err_unknown_error";
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("[WebApiCommon] Building errorMessage: {\"status\":\"error\", \"code\":\"{}\"}", err_id);
		}
		
		return "{\"status\":\"error\", \"code\":\""+err_id+"\"}";
	}
	
	public static String errorMessage(String reason, int error_code)
	{
		return "{\"status\":\"error\", \"code\":\""+error_code+"\", \"reason\":\""+reason+"\"}";
	}
	
	public static String okMessage(JSONObject data)
	{
		if(data == null) {
			return "{\"status\":\"ok\"}";
		}
		
		try {
			data.put("status", "ok");	
		} 
		catch (JSONException e) 
		{
			if(logger.isDebugEnabled()) {
				logger.error("[WebApiCommons] Error while building an ok message ", e);
			}
			
			return errorMessage(Errors.internal_error);
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("[WebApiCommons] building okMessage: "+data);
		}
		
		return data.toString();
	}
	
	// Suppress default constructor for noninstantiability
	private WebApiCommons()
	{ 
		throw new AssertionError();
	}
}
