/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
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

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profile.model.BasicProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author mirko perillo
 * 
 */
public class BasicProfileConverter {

	public static List<BasicProfile> toBasicProfile(List<User> users) throws Exception {
		List<BasicProfile> minProfiles = new ArrayList<BasicProfile>();
		try {
			for (User temp : users) {
				minProfiles.add(BasicProfileConverter.toBasicProfile(temp));
			}
		} catch (Exception e) {
			throw e;
		}

		return minProfiles;
	}

	public static BasicProfile toBasicProfile(User user)
			throws Exception {
		if (user == null) {
			return null;
		}
		BasicProfile minProfile = new BasicProfile();
		minProfile.setName(user.getName());
		minProfile.setSurname(user.getSurname());
		minProfile.setUserId(user.getId().toString());
		return minProfile;
	}
}
