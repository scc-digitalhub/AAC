package it.smartcommunitylab.aac.test;

import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.RoleManager.ROLE;
import it.smartcommunitylab.aac.manager.RoleManager.SCOPE;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.RegistrationRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableConfigurationProperties
public class UserRolesTest {

	private static final String USERNAME = "testuser";

	@Autowired
	private RegistrationManager registrationManager;	
	
	@Autowired
	private RoleManager roleManager;		
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RegistrationRepository registrationRepository;		

	private Role role1 = new Role(SCOPE.system, ROLE.developer, null);
	private Role role2 = new Role(SCOPE.application, ROLE.user, null);	
	private User user;
	
	@Before
	public void createUser() {
		user = registrationManager.registerOffline("NAME", "SURNAME", USERNAME, "password", null);
		user.setRoles(Sets.newHashSet(role1));
		userRepository.save(user);		
	}
	
	@After
	public void deleteUser() {
		Registration registration = registrationRepository.findByEmail(USERNAME);
		registrationRepository.delete(registration);
		userRepository.delete(user);
	}	
	
	@Test
	public void test() {
		Long id = user.getId();
		
		user = userRepository.findOne(id);
		
		Assert.assertTrue(roleManager.hasRole(user, role1));

		roleManager.removeRole(user, role1);
		roleManager.addRole(user, role2);
		
		user = userRepository.findOne(id);
		
		Assert.assertFalse(roleManager.hasRole(user, role1));
		Assert.assertTrue(roleManager.hasRole(user, role2));
		
		roleManager.updateRoles(user, Sets.newHashSet(role1));
		
		user = userRepository.findOne(id);
		
		Assert.assertTrue(roleManager.hasRole(user, role1));
		Assert.assertFalse(roleManager.hasRole(user, role2));		
	}
	
}
