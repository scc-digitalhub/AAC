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

/**
 * Container of custom authority handlers.
 * 
 * @author raman
 * 
 */
public class NativeAuthorityHandlerContainer {

	private Map<String, NativeAuthorityHandler> handlerMap = null;

	public NativeAuthorityHandlerContainer(Map<String, NativeAuthorityHandler> handlerMap) {
		super();
		this.handlerMap = handlerMap;
	}

	/**
	 * 
	 * @param authority
	 * @return handler for the specific authority. If not present, the default
	 *         {@link DefaultAuthorityHandler} is returned.
	 */
	public NativeAuthorityHandler getAuthorityHandler(String authority) {
		if (handlerMap.containsKey(authority)) {
			return handlerMap.get(authority);
		}
		return null;
	}

}
