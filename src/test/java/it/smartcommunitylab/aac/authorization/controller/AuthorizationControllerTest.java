package it.smartcommunitylab.aac.authorization.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.authorization.AuthorizationHelper;
import it.smartcommunitylab.aac.authorization.AuthorizationSchemaHelper;
import it.smartcommunitylab.aac.authorization.NotValidResourceException;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeValueDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationResourceDTO;
import it.smartcommunitylab.aac.authorization.model.Authorization;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNode;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeAlreadyExist;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeValue;
import it.smartcommunitylab.aac.authorization.model.AuthorizationUser;
import it.smartcommunitylab.aac.authorization.model.FQname;
import it.smartcommunitylab.aac.authorization.model.Resource;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.ResourceManager;
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
		AuthorizationControllerTestConfig.class }, loader = AnnotationConfigWebContextLoader.class, initializers = ConfigFileApplicationContextInitializer.class)
public class AuthorizationControllerTest {

	private static final String TEST = "TEST";
	
	private final static String BEARER = "Bearer ";
	
	@Autowired
	private WebApplicationContext ctx;
	
	@Autowired
	private ResourceManager resourceManager;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;	
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ResourceRepository resourceRepository;	
	@Autowired
	private UserRepository userRepository;	
	
	private MockMvc mockMvc;
	
	private ObjectMapper jsonMapper;
	private ClientDetails client;	

	private Role testRole = new Role(ROLE_SCOPE.application, "authorization_domain", "carbon.super");

	@Before
	public void setUp() throws Exception {
		jsonMapper = new ObjectMapper();
		mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
		resourceManager.init();
		providerServiceAdapter.init();

		User user = userRepository.findByName("admin");
		user.getRoles().add(testRole);
		userRepository.save(user);
		
		client = clientDetailsRepository.findByClientId(TEST);
		if (client == null) {
			client = createTestClient(user.getId());
		}
	}
	
	@After
	public void tearDown() throws Exception {
		User user = userRepository.findByName("admin");
		user.getRoles().remove(testRole);
		userRepository.save(user);
	}	

	@Test
	public void addRootChild() throws Exception {
		AuthorizationNode node = new AuthorizationNode(new FQname("domain", "A"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/schema")
				.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(node)).header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void addChildAuthorizationNode() throws Exception {
		AuthorizationNode node = new AuthorizationNode(new FQname("domain", "A"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/schema/parent-qname")
				.content(jsonMapper.writeValueAsString(node)).contentType(MediaType.APPLICATION_JSON).header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void getSchema() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/authorization/domain/schema/qname-node").header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}
	
	@Test
	public void getSchemaFail() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/authorization/test/schema/qname-node").header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is4xxClientError());
	}	

	@Test
	public void removeAuthorization() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.delete("/authorization/domain/my-auth").header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void insertAuthorization() throws Exception {
		Resource resource = new Resource(new FQname("domain", "A"),
				Arrays.asList(new AuthorizationNodeValue("A", "a", "a_value")));
		Authorization auth = new Authorization(new AuthorizationUser("subject", "type"), "action", resource,
				new AuthorizationUser("entity", "type"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain")
				.content(jsonMapper.writeValueAsString(auth)).contentType(MediaType.APPLICATION_JSON).header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void validateAuthorization() throws Exception {
		Resource resource = new Resource(new FQname("domain", "A"),
				Arrays.asList(new AuthorizationNodeValue("A", "a", "a_value")));
		Authorization auth = new Authorization(new AuthorizationUser("subject", "type"), "action", resource,
				new AuthorizationUser("entity", "type"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/validate")
				.content(jsonMapper.writeValueAsString(auth)).contentType(MediaType.APPLICATION_JSON).header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andExpect(MockMvcResultMatchers.content().string("false"));
	}

	@Test
	public void validateResource() throws Exception {
		AuthorizationNodeValueDTO nodeValue = new AuthorizationNodeValueDTO();
		nodeValue.setName("a");
		nodeValue.setQname("A");
		nodeValue.setValue("a_value");
		AuthorizationResourceDTO resource = new AuthorizationResourceDTO();
		resource.setQnameRef("A");
		resource.setValues(Arrays.asList(nodeValue));
		ObjectMapper jsonMapper = new ObjectMapper();
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/schema/validate")
				.content(jsonMapper.writeValueAsString(resource)).contentType(MediaType.APPLICATION_JSON).header("Authorization", getToken());
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}
	
	private String getToken() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.post("/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret() + "&grant_type=client_credentials&scope=authorization.manage authorization.schema.manage");
		String response = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
		Map responseMap = jsonMapper.readValue(response, Map.class);
		String token = BEARER + (String)responseMap.get("access_token");
		System.err.println(token);
		return token;		
	}
	
	private String getOauthToken() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.post("/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret() + "&grant_type=client_credentials");		
		String response = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
		Map responseMap = jsonMapper.readValue(response, Map.class);
		String token = (String)responseMap.get("access_token");
		System.err.println("OT: " + token);
		return token;
	}	
	
	private ClientDetails createTestClient(long developerId) throws Exception {
		ClientDetailsEntity entity = new ClientDetailsEntity();
		ClientAppInfo info = new ClientAppInfo();
		info.setName(TEST);
		entity.setAdditionalInformation(info.toJson());
		entity.setClientId(TEST);
		entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
		entity.setAuthorizedGrantTypes("password,client_credentials,implicit");
		entity.setDeveloperId(developerId);
		entity.setClientSecret(UUID.randomUUID().toString());
		entity.setClientSecretMobile(UUID.randomUUID().toString());
		
		String resourcesId = "";
		it.smartcommunitylab.aac.model.Resource r = resourceRepository.findByServiceIdAndResourceType("carbon.super-AACAuthorization-1.0.0", "authorization.manage");
		resourcesId += r.getResourceId();
		r = resourceRepository.findByServiceIdAndResourceType("carbon.super-AACAuthorization-1.0.0", "authorization.schema.manage");
		resourcesId += "," + r.getResourceId();
		entity.setResourceIds(resourcesId);
		
		entity.setName(TEST);
		entity.setScope("authorization.manage,authorization.schema.manage");
		
		entity = clientDetailsRepository.save(entity);
		return entity;
	}		
	
}

@TestConfiguration
@EnableWebMvc // without it mockMvc post thrown an HTTP 415 ERROR
@ComponentScan(basePackages = { "it.smartcommunitylab.aac"})
class AuthorizationControllerTestConfig {

	@Bean
	public AuthorizationController authorizationController() {
		return new AuthorizationController();
	}

	@Bean
	public AuthorizationHelper authorizationHelper() {
		return new AuthorizationHelper() {

			@Override
			public boolean validate(Authorization auth) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void remove(Authorization auth) {
				// TODO Auto-generated method stub

			}

			@Override
			public Authorization insert(Authorization auth) throws NotValidResourceException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void remove(String authorizationId) {
				// TODO Auto-generated method stub

			}
		};
	}

	@Bean
	public AuthorizationSchemaHelper authorizationSchemaHelper() {
		return new AuthorizationSchemaHelper() {

			@Override
			public boolean isValid(Resource res) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public AuthorizationNode getNode(FQname qname) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getChildren(AuthorizationNode node) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getAllChildren(AuthorizationNode node) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AuthorizationSchemaHelper addRootChild(AuthorizationNode child)
					throws AuthorizationNodeAlreadyExist {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AuthorizationSchemaHelper addChild(AuthorizationNode parent, AuthorizationNode child)
					throws AuthorizationNodeAlreadyExist {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AuthorizationSchemaHelper addChild(FQname parentQname, AuthorizationNode child)
					throws AuthorizationNodeAlreadyExist {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getChildren(FQname qName) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getAllChildren(FQname qname) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

}
