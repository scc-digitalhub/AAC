package it.smartcommunitylab.aac.test.oauth;

import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.test.openid.OpenidUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=false" })
@ActiveProfiles("test")
@EnableConfigurationProperties
public class PasswordGrantTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String server = "http://localhost";

    @LocalServerPort
    private int port;

    @Value("${server.contextPath}")
    private String contextPath;

    @Value("${jwt.issuer}")
    private String issuer;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

    @Autowired
    private RegistrationManager registrationManager;

    private static ClientDetails client;

    private static User user;

    private final String testUserName = "passwordtest@test.local";
    private final String testPassword = "passw123";

    private final static String SCOPE = "profile";

    private final static String[] GRANT_TYPES = { "password", "refresh_token" };

    @Before
    public void init() {
        String endpoint = server + ":" + port;
        if (user == null) {
            try {
                user = createUser(testUserName, testPassword, "TestName", "TestSurname");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                user = null;
            }
        }

        if (client == null && user != null) {
            try {
                // use local address as redirect
                // also save it
                client = clientDetailsRepository.saveAndFlush(OpenidUtils.createClient(
                        UUID.randomUUID().toString(),
                        user.getId(),
                        String.join(",", GRANT_TYPES), new String[] { SCOPE },
                        endpoint));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                client = null;
            }
        }

    }

    @After
    public void cleanup() {
    }

    @Test
    public void passwordGrantTestWithBasicAuth()
            throws Exception {

        logger.debug("password grant (with basic auth) with client " + client.getClientId() + " and user "
                + user.getUsername());

        // check context
        Assert.assertNotNull(user);
        Assert.assertNotNull(client);

        String clientId = client.getClientId();

        // exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // build basic auth for client
        String auth = clientId + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "password");
        map.add("username", testUserName);
        map.add("password", testPassword);
        map.add("scope", SCOPE);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 200 with json in body
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        logger.trace(response.getBody());

        // parse
        org.json.JSONObject json = new org.json.JSONObject(response.getBody());

        String accessToken = json.getString("access_token");
        String refreshToken = json.optString("refresh_token");
        String tokenType = json.getString("token_type");
        int expiresIn = json.getInt("expires_in");

        logger.debug("accessToken " + accessToken);
        logger.debug("refreshToken " + refreshToken);

        // basic validation
        Assert.assertTrue(StringUtils.isNotEmpty(accessToken));
        Assert.assertTrue(StringUtils.isNotEmpty(refreshToken));
        Assert.assertEquals("Bearer", tokenType);

        // check scope
        Assert.assertTrue(json.getString("scope").contains(SCOPE));

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);
    }

    @Test
    public void passwordGrantTestWithFormAuth()
            throws Exception {

        logger.debug("password grant (with form auth) with client " + client.getClientId() + " and user "
                + user.getUsername());

        // check context
        Assert.assertNotNull(user);
        Assert.assertNotNull(client);

        String clientId = client.getClientId();

        // exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "password");
        map.add("username", testUserName);
        map.add("password", testPassword);
        map.add("scope", SCOPE);
        // add client credentials in form
        map.add("client_id", clientId);
        map.add("client_secret", client.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 200 with json in body
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        logger.trace(response.getBody());

        // parse
        org.json.JSONObject json = new org.json.JSONObject(response.getBody());

        String accessToken = json.getString("access_token");
        String refreshToken = json.optString("refresh_token");
        String tokenType = json.getString("token_type");
        int expiresIn = json.getInt("expires_in");

        logger.debug("accessToken " + accessToken);
        logger.debug("refreshToken " + refreshToken);

        // basic validation
        Assert.assertTrue(StringUtils.isNotEmpty(accessToken));
        Assert.assertTrue(StringUtils.isNotEmpty(refreshToken));
        Assert.assertEquals("Bearer", tokenType);

        // check scope
        Assert.assertTrue(json.getString("scope").contains(SCOPE));

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);
    }

    @Test
    public void passwordGrantTestWithNoAuth()
            throws Exception {

        logger.debug("password grant (with no auth) with client " + client.getClientId() + " and user "
                + user.getUsername());

        // check context
        Assert.assertNotNull(user);
        Assert.assertNotNull(client);

        String clientId = client.getClientId();

        // exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "password");
        map.add("username", testUserName);
        map.add("password", testPassword);
        map.add("scope", SCOPE);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 401 with json in body
        Assert.assertTrue((response.getStatusCode().is4xxClientError()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        logger.trace(response.getBody());

    }

    @Test
    public void passwordGrantTestWithNoSecret()
            throws Exception {

        logger.debug("password grant (with no secret) with client " + client.getClientId() + " and user "
                + user.getUsername());

        // check context
        Assert.assertNotNull(user);
        Assert.assertNotNull(client);

        String clientId = client.getClientId();

        // exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "password");
        map.add("username", testUserName);
        map.add("password", testPassword);
        map.add("scope", SCOPE);
        map.add("client_id", client.getClientId());
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 401 with json in body
        Assert.assertTrue((response.getStatusCode().is4xxClientError()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        logger.trace(response.getBody());

    }

    @Test
    public void passwordGrantTestWithWrongAuth()
            throws Exception {

        logger.debug("password grant (with wrong auth) with client " + client.getClientId() + " and user "
                + user.getUsername());

        // check context
        Assert.assertNotNull(user);
        Assert.assertNotNull(client);

        String clientId = client.getClientId();

        // exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // build garbage auth for client
        String auth = clientId + ":" + RandomStringUtils.random(5, true, true);
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "password");
        map.add("username", testUserName);
        map.add("password", testPassword);
        map.add("scope", SCOPE);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 401 with json in body
        Assert.assertTrue((response.getStatusCode().is4xxClientError()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        logger.trace(response.getBody());

    }

    /*
     * Helpers
     */
    private User createUser(String userName, String password, String name, String surname) {
        // username == email
        return registrationManager.registerOffline(name, surname, userName, password, null, false, null);

    }
}
