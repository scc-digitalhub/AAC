package it.smartcommunitylab.aac.test;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.controller.RolesController;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AACRolesControllerTestConfig.class }, loader = AnnotationConfigWebContextLoader.class, initializers = ConfigFileApplicationContextInitializer.class)
public class AACRolesControllerTest {

	private static final String TEST_ROLE = "TEST/TEST:TEST";

	@Autowired
	private WebApplicationContext ctx;

	private MockMvc mockMvc;
	private ObjectMapper jsonMapper = new ObjectMapper();
	@Autowired
	private ResourceRepository resourceRepository;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private RegistrationManager registrationManager;	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private ResourceManager resourceManager;

	private ClientDetails client;
	private String token;
	private User user;

	private String userToken;

	private static final String USERNAME = "testuser";
	private final static String BEARER = "Bearer ";
	private static final String TEST = "TEST";

	@Before
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
		resourceManager.init();
		providerServiceAdapter.init();
		roleManager.init();

		try {
			user = registrationManager.registerOffline("NAME", "SURNAME", USERNAME, "password", null, false, null);
			user.getRoles().add(new Role(TEST, TEST, Config.R_PROVIDER));
			userRepository.save(user);		
		} catch (AlreadyRegisteredException e) {
			user = userRepository.findByUsername(USERNAME);
		}
		client = clientDetailsRepository.findByClientId(TEST);
		if (client == null) {
			client = createTestClient(TEST, user.getId());
		}
		token = getToken(client);
		userToken = getUserToken(client);
	}
	
	@Test
	public void testGetRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/me")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}
	
	@Test
	public void testGetAllRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/user/" + user.getId())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}	
	
	@Test
	public void testGetClientRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/client")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}		
	
	@Test
	public void testAddDeleteRoles() throws Exception {
		int size = getRoleSize();
		
		RequestBuilder request = MockMvcRequestBuilders.put("/userroles/user/{userId}?roles={roles}", user.getId(), TEST_ROLE)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		int addSize = getRoleSize();
		assertEquals(size + 1, addSize);
		
		request = MockMvcRequestBuilders.delete("/userroles/user/{userId}?roles={roles}", user.getId(), TEST_ROLE)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", token).content(TEST_ROLE);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());		

		int deleteSize = getRoleSize();
		assertEquals(size, deleteSize);		
	}	
	
	private int getRoleSize() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/user/" + user.getId())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", token);
		String result = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
		List resultList = mapper.readValue(result, List.class);
		return resultList.size();
	}
	
	
	@SuppressWarnings("unchecked")
	private String getToken(ClientDetails client) throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.post(
				"/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret()
						+ "&grant_type=client_credentials&scope=user.roles.me,user.roles.read,client.roles.read.all,user.roles.read.all,user.roles.write");
		
		ResultActions res = mockMvc.perform(request);
		res.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		String response = res.andReturn().getResponse().getContentAsString();
		Map<String,Object> responseMap = jsonMapper.readValue(response, Map.class);
		String token = BEARER + (String) responseMap.get("access_token");
		return token;
	}
	
	@SuppressWarnings("unchecked")
	private String getUserToken(ClientDetails client) throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.post(
				"/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret()
						+ "&grant_type=password&scope=user.roles.me&username="+USERNAME+"&password=password");
		
		ResultActions res = mockMvc.perform(request);
		res.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		String response = res.andReturn().getResponse().getContentAsString();
		Map<String,Object> responseMap = jsonMapper.readValue(response, Map.class);
		String token = BEARER + (String) responseMap.get("access_token");
		return token;
	}

	private ClientDetails createTestClient(String client, long developerId) throws Exception {
		ClientDetailsEntity entity = new ClientDetailsEntity();
		ClientAppInfo info = new ClientAppInfo();
		info.setIdentityProviders(Collections.singletonMap("internal", 1));
		info.setName(client);
		entity.setAdditionalInformation(info.toJson());
		entity.setClientId(client);
		entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
		entity.setAuthorizedGrantTypes("password,client_credentials,implicit");
		entity.setDeveloperId(developerId);
		entity.setClientSecret(UUID.randomUUID().toString());
		entity.setClientSecretMobile(UUID.randomUUID().toString());
		entity.setScope("user.roles.me,user.roles.read,client.roles.read.all,user.roles.read.all,user.roles.write");
		entity.setName(client);

		entity = clientDetailsRepository.save(entity);
		return entity;
	}
}

@TestConfiguration
@EnableWebMvc
@ComponentScan(basePackages = { "it.smartcommunitylab.aac"})
class AACRolesControllerTestConfig {

	@Bean
	public RolesController rolesController() {
		return new RolesController();
	}

	
}
