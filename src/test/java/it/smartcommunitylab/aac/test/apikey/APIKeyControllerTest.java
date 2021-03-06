package it.smartcommunitylab.aac.test.apikey;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.apikey.manager.APIKeyManager;
import it.smartcommunitylab.aac.dto.APIKey;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.RegistrationRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
public class APIKeyControllerTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String USERNAME = "testuser";

    private static final String TEST = "TEST";
    private static final String TEST2 = "TEST2";

    private final static String BEARER = "Bearer ";

    @Autowired
    private RegistrationManager registrationManager;

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private ClientDetailsRepository clientDetailsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RegistrationRepository regRepository;
    @Autowired
    private APIKeyManager keyManager;

    private MockMvc mockMvc;

    private ClientDetails client;
    private ClientDetails client2;
    private User user;
    private ObjectMapper jsonMapper = new ObjectMapper();
    private String token;
    private String token2;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();

        logger.debug("create user");
        Registration reg = registrationManager.registerOffline("NAME", "SURNAME", USERNAME, "password", null, false, null);
        user = userRepository.findOne(Long.parseLong(reg.getUserId()));
        userRepository.save(user);

        client = clientDetailsRepository.findByClientId(TEST);
        if (client == null) {
            client = createTestClient(TEST, user.getId());
        }
        token = getToken(client);

        client2 = clientDetailsRepository.findByClientId(TEST2);
        if (client2 == null) {
            client2 = createTestClient(TEST2, user.getId());
        }
        token2 = getToken(client2);
    }

    @After
    public void tearDown() throws Exception {
        if (user != null) {
            logger.debug("delete user");
            regRepository.delete(regRepository.findByEmail(USERNAME));
            userRepository.delete(user);
            user = null;
        }
        if (client != null) {
            List<APIKey> clientKeys = keyManager.getClientKeys(client.getClientId());
            clientKeys.forEach(k -> keyManager.deleteKey(k.getApiKey()));
            clientDetailsRepository.delete(clientDetailsRepository.findByClientId(client.getClientId()));
            client = null;
        }
        if (client2 != null) {
            clientDetailsRepository.delete(clientDetailsRepository.findByClientId(client2.getClientId()));
        }
    }

    @Test
    public void createKey() throws Exception {

        APIKey key = new APIKey();
        key.setAdditionalInformation(Collections.singletonMap("Key", "Value"));
        // scopes
        String[] scopes = new String[] { "authorization.manage" };
        key.setScope(scopes);
        key.setValidity(3000);
        RequestBuilder request = MockMvcRequestBuilders.post("/apikey/client")
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(key))
                .header("Authorization", token);
        ResultActions result = mockMvc.perform(request);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        String string = result.andReturn().getResponse().getContentAsString();
        APIKey resKey = jsonMapper.readValue(string, APIKey.class);
        // additional info is the same
        Assert.assertEquals("Value", resKey.getAdditionalInformation().get("Key"));
        // validity is in the future
        int now = (int) (System.currentTimeMillis() / 1000);
        Assert.assertTrue(resKey.getExpirationTime() > now);
        // scope is the same
        Assert.assertTrue(Arrays.equals(scopes, resKey.getScope()));
    }

    @Test
    public void validateKey() throws Exception {
        // create key
        APIKey key = new APIKey();
        key.setValidity(3);
        RequestBuilder request = MockMvcRequestBuilders.post("/apikey/client")
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(key))
                .header("Authorization", token);
        ResultActions result = mockMvc.perform(request);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        String string = result.andReturn().getResponse().getContentAsString();
        APIKey resKey = jsonMapper.readValue(string, APIKey.class);

        // search key
        RequestBuilder validate = MockMvcRequestBuilders.get("/apikeycheck/{apiKey}", resKey.getApiKey())
                .header("Authorization", token);

        result = mockMvc.perform(validate);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        string = result.andReturn().getResponse().getContentAsString();
        resKey = jsonMapper.readValue(string, APIKey.class);
        Assert.assertEquals(resKey.getClientId(), client.getClientId());

        Thread.sleep(5000L);
        validate = MockMvcRequestBuilders.get("/apikeycheck/{apiKey}", resKey.getApiKey())
                .header("Authorization", token);
        result = mockMvc.perform(validate);
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    public void checkAccess() throws Exception {
        // create key
        APIKey key = new APIKey();
        key.setValidity(300);
        RequestBuilder request = MockMvcRequestBuilders.post("/apikey/client")
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(key))
                .header("Authorization", token);
        ResultActions result = mockMvc.perform(request);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        String string = result.andReturn().getResponse().getContentAsString();
        APIKey resKey = jsonMapper.readValue(string, APIKey.class);

        // search keys
        RequestBuilder search = MockMvcRequestBuilders.get("/apikey/client/me")
                .header("Authorization", token);
        result = mockMvc.perform(search);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        string = result.andReturn().getResponse().getContentAsString();
        TypeReference<List<APIKey>> tr = new TypeReference<List<APIKey>>() {
        };
        List<APIKey> list = jsonMapper.readValue(string, tr);
        Assert.assertEquals(1, list.size());

        // search keys
        RequestBuilder search2 = MockMvcRequestBuilders.delete("/apikey/client/{apiKey}", resKey.getApiKey())
                .header("Authorization", token2);
        result = mockMvc.perform(search2);
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void manageKey() throws Exception {
        // create key
        APIKey key = new APIKey();
        key.setValidity(3000);
        RequestBuilder request = MockMvcRequestBuilders.post("/apikey/client")
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(key))
                .header("Authorization", token);
        ResultActions result = mockMvc.perform(request);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        String string = result.andReturn().getResponse().getContentAsString();
        APIKey resKey = jsonMapper.readValue(string, APIKey.class);

        // search keys
        RequestBuilder search = MockMvcRequestBuilders.get("/apikey/client/me")
                .header("Authorization", token);
        result = mockMvc.perform(search);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        string = result.andReturn().getResponse().getContentAsString();
        TypeReference<List<APIKey>> tr = new TypeReference<List<APIKey>>() {
        };
        List<APIKey> list = jsonMapper.readValue(string, tr);
        Assert.assertEquals(1, list.size());

        // update key, clients can not update validity on their own
        key.setAdditionalInformation(Collections.singletonMap("Key", "Value"));
        RequestBuilder update = MockMvcRequestBuilders.put("/apikey/client/{apiKey}", resKey.getApiKey())
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(key))
                .header("Authorization", token);
        result = mockMvc.perform(update);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        string = result.andReturn().getResponse().getContentAsString();
        resKey = jsonMapper.readValue(string, APIKey.class);
        Assert.assertEquals(resKey.getAdditionalInformation(), Collections.singletonMap("Key", "Value"));

        // validate
        Thread.sleep(4000L);
        RequestBuilder validate = MockMvcRequestBuilders.get("/apikeycheck/{apiKey}", resKey.getApiKey())
                .header("Authorization", token);
        result = mockMvc.perform(validate);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        // delete
        RequestBuilder delete = MockMvcRequestBuilders.delete("/apikey/client/{apiKey}", resKey.getApiKey())
                .header("Authorization", token);
        result = mockMvc.perform(delete);
        result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        // validate
        validate = MockMvcRequestBuilders.get("/apikeycheck/{apiKey}", resKey.getApiKey())
                .header("Authorization", token);
        result = mockMvc.perform(validate);
        result.andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @SuppressWarnings("unchecked")
    private String getToken(ClientDetails client) throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post(
                "/oauth/token?" + "client_id=" + client.getClientId() + "&client_secret=" + client.getClientSecret()
                        + "&grant_type=client_credentials&scope=authorization.manage%20apikey.client.me");

        ResultActions res = mockMvc.perform(request);
        res.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        String response = res.andReturn().getResponse().getContentAsString();
        Map<String, Object> responseMap = jsonMapper.readValue(response, Map.class);
        String token = BEARER + (String) responseMap.get("access_token");
        return token;
    }

    private ClientDetails createTestClient(String client, long developerId) throws Exception {
        ClientDetailsEntity entity = new ClientDetailsEntity();
        ClientAppInfo info = new ClientAppInfo();
        info.setName(client);
        entity.setAdditionalInformation(info.toJson());
        entity.setClientId(client);
        entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
        entity.setAuthorizedGrantTypes("password,client_credentials,implicit");
        entity.setDeveloperId(developerId);
        entity.setClientSecret(UUID.randomUUID().toString());
//        entity.setClientSecretMobile(UUID.randomUUID().toString());
        entity.setScope("authorization.manage,apikey.client.me");
        String resourcesId = "" + serviceManager.findServiceIdsByScopes(
                new HashSet<>(Arrays.asList(new String[] { "authorization.manage", "apikey.client.me" })));
        entity.setResourceIds(resourcesId);
        entity.setName(client);

        entity = clientDetailsRepository.save(entity);
        return entity;
    }

}
