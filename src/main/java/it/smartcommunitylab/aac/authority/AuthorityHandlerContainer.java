/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
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

package it.smartcommunitylab.aac.authority;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Container of custom authority handlers.
 * 
 * @author raman
 * 
 */
public class AuthorityHandlerContainer {

	private Map<String, AuthorityHandler> handlerMap = null;

	@Autowired
	DefaultAuthorityHandler defaultHandler;

	public AuthorityHandlerContainer(Map<String, AuthorityHandler> handlerMap) {
		super();
		this.handlerMap = handlerMap;
	}

	/**
	 * 
	 * @param authority
	 * @return handler for the specific authority. If not present, the default
	 *         {@link DefaultAuthorityHandler} is returned.
	 */
	public AuthorityHandler getAuthorityHandler(String authority) {
		if (handlerMap.containsKey(authority)) {
			return handlerMap.get(authority);
		}
		return defaultHandler;
	}

}
