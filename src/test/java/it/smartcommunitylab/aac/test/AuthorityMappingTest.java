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
package it.smartcommunitylab.aac.test;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Test Class
 * 
 * @author raman
 * 
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/applicationContext.xml")
public class AuthorityMappingTest {
	
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;
	@Autowired
	private UserRepository userRepository;

	@Before
	public void init() {
		List<User> users = userRepository.findByFullNameLike("mario rossi");
		if (users != null) {
			userRepository.delete(users);
		}
	}
	
	@Test
	public void testUsers() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("openid.ext1.value.name", "mario");
		req.addParameter("openid.ext1.value.surname", "rossi");
		req.addParameter("openid.ext1.value.email", "mario.rossi@gmail.com");
		
		providerServiceAdapter.updateUser("google", new HashMap<String, String>(), req);
		List<User> users = userRepository.findByFullNameLike("mario rossi");
		Assert.assertNotNull(users);
		Assert.assertEquals(1, users.size());
		
		providerServiceAdapter.updateUser("googlelocal", new HashMap<String, String>(), req);
		users = userRepository.findByFullNameLike("mario rossi");
		Assert.assertNotNull(users);
		Assert.assertEquals(1, users.size());
		
		req = new MockHttpServletRequest();
		req.addParameter("openid.ext1.value.name", "mario");
		req.addParameter("openid.ext1.value.surname", "rossi");
		req.addParameter("openid.ext1.value.email", "mario.rossi2@gmail.com");
		
		providerServiceAdapter.updateUser("google", new HashMap<String, String>(), req);
		users = userRepository.findByFullNameLike("mario rossi");
		Assert.assertNotNull(users);
		Assert.assertEquals(2, users.size());

	}
}
