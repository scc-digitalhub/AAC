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

package it.smartcommunitylab.aac.authority;

import java.util.Map;

import it.smartcommunitylab.aac.Config;

/**
 * @author raman
 *
 */
public class FBAuthorityHandler extends DefaultAuthorityHandler {

	@Override
	public String extractUsername(Map<String, String> map) {
		if (map.get(Config.USER_ATTR_USERNAME) != null) return map.get(Config.USER_ATTR_USERNAME);
		return map.get("id") + "@facebook";
	}

}
