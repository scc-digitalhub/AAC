/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.manager;

import javax.servlet.http.HttpServletRequest;

/**
 * 2 Factor mobile authentication interface. Implements redirect and callback operations
 * @author raman
 *
 */
public interface MobileAuthManager {

	String provider();
	
	/**
	 * Make a new request for the 2factor app authentication. Redirect to mobile app and if no 
	 * certificate has already been issued, ask for a new certificate
	 * @param request
	 * @param response
	 * @throws SecurityException
	 */
	String init2FactorCheck(HttpServletRequest request, String redirect) throws SecurityException;

	/**
	 * Process response from mobile app.
	 * @param request
	 * @return
	 */
	void callback2FactorCheck(HttpServletRequest request) throws SecurityException;

}