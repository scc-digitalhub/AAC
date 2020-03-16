package it.smartcommunitylab.aac.test;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

public class OAuth2AwareControllerTest {

	@Autowired
	protected WebApplicationContext ctx;
	@Autowired
	protected ClientDetailsRepository clientDetailsRepository;
	@Autowired
	protected RegistrationManager registrationManager;	
	@Autowired
	protected UserRepository userRepository;

	protected ObjectMapper mapper = new ObjectMapper();

	protected ClientDetails client;
	protected String token;
	protected User user;

	protected String userToken;

	protected MockMvc mockMvc;

	protected final static String BEARER = "Bearer ";
    protected final static String TEST = "TEST";
    
	protected String getScopes() {
		return "user.roles.me,user.roles.read,client.roles.read.all,user.roles.read.all,user.roles.write";
	}
	
	/**
	 * Common setup for the test: init components, create user, create test client, and tokens
	 * @throws Exception
	 */
	public void setUp(String clientId, String username, String password) throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();

		try {
			Registration reg = registrationManager.registerOffline("NAME", "SURNAME", username, password, null, false, null);
	        user = userRepository.findOne(Long.parseLong(reg.getUserId()));

		} catch (AlreadyRegisteredException e) {
			user = userRepository.findByUsername(username);
		}
		addRoleToTestUser(TEST, TEST, Config.R_PROVIDER);
		
		client = clientDetailsRepository.findByClientId(clientId);
		if (client == null) {
			client = createTestClient(clientId, user.getId());
		}
		
		token = getToken(client);
		userToken = getUserToken(client, username, password);
	}
	
	protected void addRoleToTestUser(String context, String space, String role) {
		user.getRoles().add(new Role(context, space, role));
		userRepository.save(user);		
	}
	
	@SuppressWarnings("unchecked")
	private String getToken(ClientDetails client) throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.post(
				"/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret()
						+ "&grant_type=client_credentials&scope="+getScopes());
		
		ResultActions res = mockMvc.perform(request);
		res.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		String response = res.andReturn().getResponse().getContentAsString();
		Map<String,Object> responseMap = mapper.readValue(response, Map.class);
		String token = BEARER + (String) responseMap.get("access_token");
		return token;
	}
	
	@SuppressWarnings("unchecked")
	private String getUserToken(ClientDetails client, String username, String password) throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.post(
				"/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret()
						+ "&grant_type=password&scope="+getScopes()+"&username="+username+"&password="+password);
		
		ResultActions res = mockMvc.perform(request);
		res.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		String response = res.andReturn().getResponse().getContentAsString();
		Map<String,Object> responseMap = mapper.readValue(response, Map.class);
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
		entity.setScope(getScopes());
		entity.setName(client);

		entity = clientDetailsRepository.save(entity);
		return entity;
	}
}
