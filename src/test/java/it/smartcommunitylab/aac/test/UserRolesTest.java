package it.smartcommunitylab.aac.test;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.RegistrationRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.roles.dto.RoleModel;

//TODO rewrite tests
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//@EnableConfigurationProperties
public class UserRolesTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String TESTAPP = "testapp";
	private static final String TENANTUSERNAME = "testusertenant";
	private static final String USERNAME = "testuserroles";
	private static final String USERNAME2 = "testuserroles2";
	private static final String CUSTOM_ROLE = "ROLE_CUSTOM";
	private static final String TESTTENANT = "test.tenant";

	@Autowired
	private RegistrationManager registrationManager;	
	
	@Autowired
	private RoleManager roleManager;		

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RegistrationRepository regRepository;
	
	@Value("${api.contextSpace}")
	private String apiManagerContext;

	private Role role1 = new Role(null, null, Config.R_ADMIN);
	private Role role2 = new Role(null, TESTAPP, Config.R_USER);
	private Role role3 = new Role(TESTTENANT, TESTAPP, Config.R_USER);
	private Role role4 = new Role(TESTTENANT, TESTAPP, CUSTOM_ROLE);
	private User user;
	private User tenantuser;
	
    @Before
    public void init() {
        logger.debug("create user "+USERNAME);
        try {
            Registration reg = registrationManager.registerOffline("NAME", "SURNAME", USERNAME, "password", null, false, null);
            user = userRepository.findOne(Long.parseLong(reg.getUserId()));

        } catch (AlreadyRegisteredException e) {
            user = userRepository.findByUsername(USERNAME);
        }
        logger.trace("user "+String.valueOf(user));
        user.setRoles(Sets.newHashSet(role1));
        userRepository.save(user);
    }
	
    @After
    public void cleanup() {
        Registration reg1 =registrationManager.getUserByEmail(USERNAME); 
        if (reg1 != null) {
            long userId = Long.parseLong(reg1.getUserId());
            logger.debug("delete user "+USERNAME + " with id "+String.valueOf(userId));
            regRepository.delete(reg1);
            userRepository.delete(userId);
        }
        
        Registration reg2 =registrationManager.getUserByEmail(USERNAME2); 
        if (reg2 != null) {
            long userId = Long.parseLong(reg2.getUserId());
            logger.debug("delete user "+USERNAME2 + " with id "+String.valueOf(userId));
            regRepository.delete(reg2);
            userRepository.delete(userId);
        }
        
        Registration regT =registrationManager.getUserByEmail(TENANTUSERNAME); 
        if (regT != null) {
            long userId = Long.parseLong(regT.getUserId());
            logger.debug("delete user "+TENANTUSERNAME + " with id "+String.valueOf(userId));
            regRepository.delete(regT);
            userRepository.delete(userId);
        }
        
        
    }
	
//	@After
//	public void deleteUser() {
//		registrationRepository.deleteAll();
//		roleRepository.deleteAll();
//		userRepository.deleteAll();
//	}	

//	@Test
//	public void testNewRoles() {
//		
//		User created = registrationManager.registerOffline(USERNAME2, USERNAME2, USERNAME2, USERNAME2, Config.DEFAULT_LANG, false, null);
//		roleManager.addRole(created, role4);
//		
//		Assert.assertTrue(roleManager.hasRole(created, role4));
//	}
//	
//	@Test
//	public void testChangeRoles() {
//		Long id = user.getId();
//		
//		user = userRepository.findOne(id);
//		
//		Assert.assertTrue(roleManager.hasRole(user, role1));
//
//		roleManager.removeRole(user, role1);
//		roleManager.addRole(user, role2);
//		
//		user = userRepository.findOne(id);
//		
//		Assert.assertFalse(roleManager.hasRole(user, role1));
//		Assert.assertTrue(roleManager.hasRole(user, role2));
//		
//		roleManager.updateRoles(user, Sets.newHashSet(role1), Sets.newHashSet(role2));
//		
//		user = userRepository.findOne(id);
//		
//		Assert.assertTrue(roleManager.hasRole(user, role1));
//		Assert.assertFalse(roleManager.hasRole(user, role2));		
//	}
//	
//	@Test
//	public void testFindByRole() {
////		Pageable pageable = new PageRequest(0, 5);
////		List<User> users = userRepository.findByPartialRole(role1.getRole(), role1.getScope(), pageable);
//		List<User> users = roleManager.findUsersByRole(role1.getRole(), role1.getContext(), 0, 5);
//		Assert.assertEquals(2, users.size());
//		
////		pageable = new PageRequest(0, 1);
////		users = userRepository.findByPartialRole(role1.getRole(), role1.getScope(), pageable);
//		users = roleManager.findUsersByRole(role1.getRole(), role1.getContext(), 0, 1);
//		Assert.assertEquals(1, users.size());		
//		
////		pageable = new PageRequest(1, 1);
////		users = userRepository.findByPartialRole(role1.getRole(), role1.getScope(), pageable);
//		users = roleManager.findUsersByRole( role1.getRole(), role1.getContext(), 1, 1);
//		Assert.assertEquals(1, users.size());		
//		
//		roleManager.addRole(user, role2);
////		pageable = new PageRequest(0, 5);
////		users = userRepository.findByFullRole(role2.getRole(), role2.getScope(), TESTAPP, pageable);
//		users = roleManager.findUsersByRole(role2.getRole(), role2.getContext(), role2.getSpace(), 0, 5);
//		Assert.assertEquals(1, users.size());	
//		
////		users = userRepository.findByFullRole(role1.getRole(), role2.getScope(), TESTAPP, pageable);
//		users = roleManager.findUsersByRole( role3.getRole(), role3.getContext(), role2.getSpace(), 0, 5);
//		Assert.assertEquals(0, users.size());			
//		
//	}
//	
//	private void createTenantProvider() {
//		tenantuser = registrationManager.registerOffline("NAME", "SURNAME", TENANTUSERNAME, "password", null, false, null);
//		Role providerRole = new Role(apiManagerContext, TESTTENANT, Config.R_PROVIDER);
//		roleManager.addRole(tenantuser, providerRole);
//	}
//	
//	@Test
//	public void testTenantRoles() {
//		createTenantProvider();
//		User created = registrationManager.registerOffline(USERNAME2, USERNAME2, USERNAME2, USERNAME2, Config.DEFAULT_LANG, false, null);
//
//		// update with roleModel
//		RoleModel model = new RoleModel();
//		model.setAddRoles(Collections.singletonList(CUSTOM_ROLE));
//		model.setUser(USERNAME2);
//		roleManager.updateLocalRoles(model, apiManagerContext, TESTTENANT);
//		List<User> list = userRepository.findByAttributeEntities(Config.IDP_INTERNAL, "email", USERNAME2);
//		Assert.assertEquals(1, list.size());
//		created = list.get(0);
//		Assert.assertTrue(created.hasRole(CUSTOM_ROLE, apiManagerContext, TESTTENANT));
//		
//		list = roleManager.findUsersByContext(apiManagerContext, TESTTENANT, 0, 100);
//		Assert.assertEquals(2, list.size());
//		list = roleManager.findUsersByContext(apiManagerContext, TESTTENANT, 1, 100);
//		Assert.assertEquals(0, list.size());
//		
//	}
	
}
