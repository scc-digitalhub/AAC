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

package it.smartcommunitylab.aac.manager;

import java.util.Set;

import it.smartcommunitylab.aac.dto.AccountProfile;
import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.User;

/**
 * @author raman
 *
 */
public class AccountProfileConverter {

	/**
	 * @param user
	 * @return
	 */
	public static AccountProfile toAccountProfile(User user) {
		if (user == null) {
			return null;
		}
		AccountProfile minProfile = new AccountProfile();
		Set<Attribute> attrs =  user.getAttributeEntities();
		if (attrs != null) {
			for (Attribute a : attrs) {
				String account = a.getAuthority().getName();
				minProfile.addAttribute(account, a.getKey(), a.getValue());
			}
		}
		minProfile.setUsername(user.getUsername());
		minProfile.setName(user.getName());
		minProfile.setSurname(user.getSurname());
		minProfile.setUserId(user.getId().toString());		
		
		return minProfile;
	}

}
