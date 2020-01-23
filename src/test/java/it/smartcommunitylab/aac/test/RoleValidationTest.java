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

package it.smartcommunitylab.aac.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.smartcommunitylab.aac.model.Role;

/**
 * @author raman
 *
 */
//DISABLED TODO update
//@RunWith(SpringJUnit4ClassRunner.class)
public class RoleValidationTest {

	
	/**
	 * 
	 */
	private static final String R_SLASH = "abcdef123._ad/12asdad";
	/**
	 * 
	 */
	private static final String R_COLON = "abcdef123._ad:12asdad";
	private static final String RVALID = "abcdef123._ad-12asdad";

	@Test
	public void testRoleValidator() {
		// correct role
		try {
			new Role(null, null, RVALID);
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
		// incorrect role:empty
		try {
			new Role(null, null, "");
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
		// incorrect role: no ':' symbol allowed
		try {
			new Role(null, null, R_COLON);
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
		// incorrect role: no '/' symbol allowed
		try {
			new Role(null, null, R_SLASH);
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void testSpaceValidator() {
		// correct role
		try {
			new Role(null, RVALID, RVALID);
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
		// incorrect role: no ':' symbol allowed
		try {
			new Role(null, R_COLON, RVALID);
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
		// incorrect role: no '/' symbol allowed
		try {
			new Role(null, R_SLASH, RVALID);
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
		// incorrect role: empty space
		try {
			new Role(RVALID, null, RVALID);
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
	}

	@Test
	public void testContextValidator() {
		// correct role
		try {
			new Role(RVALID, RVALID, RVALID);
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
		// incorrect role: no ':' symbol allowed
		try {
			new Role(R_COLON, RVALID, RVALID);
			Assert.assertTrue(false);
		} catch (Exception e) {
		}
		// correct role: '/' symbol allowed in context
		try {
			new Role(R_SLASH, RVALID, RVALID);
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
}
