/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */

package it.smartcommunitylab.aac.repository;

import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author raman
 *
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AttributeRepository attributeRepository;

	@Override
	public List<User> getUsersByAttributes(List<Attribute> list) {
		Map<Long,User> userMap = new HashMap<Long, User>();
		for (Attribute a : list) {
			List<User> attrUsers = userRepository.findByAttributeEntities(a.getAuthority().getName(), a.getKey(), a.getValue());
			if (attrUsers != null) {
				for (User u : attrUsers) {
					userMap.put(u.getId(), u);
				}
			}
		}
		return new ArrayList<User>(userMap.values());
	}

}
