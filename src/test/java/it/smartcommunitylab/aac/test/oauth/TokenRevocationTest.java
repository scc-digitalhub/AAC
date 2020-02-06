package it.smartcommunitylab.aac.test.oauth;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.test.openid.OpenidUtils;
import it.smartcommunitylab.aac.test.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=true" })
@ActiveProfiles("test")
@EnableConfigurationProperties
public class TokenRevocationTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String server = "http://localhost";

    @LocalServerPort
    private int port;

    @Value("${server.contextPath}")
    private String contextPath;

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    private static String sessionId;

    private static ClientDetails client;

    private final static String SCOPE = "profile";

    private final static String[] GRANT_TYPES = { "authorization_code", "refresh_token" };

    @Before
    public void init() {
        String endpoint = server + ":" + port;
        if (client == null) {
            try {

                User admin = userRepository.findByUsername(adminUsername);
                // use local address as redirect
                // also save it
                client = clientDetailsRepository.saveAndFlush(OpenidUtils.createClient(
                        UUID.randomUUID().toString(),
                        admin.getId(),
                        String.join(",", GRANT_TYPES), new String[] { SCOPE },
                        endpoint));
            } catch (Exception e) {
                e.printStackTrace();
                client = null;
            }
        }

        if (StringUtils.isEmpty(sessionId)) {
            // login and validate session
            sessionId = TestUtils.login(restTemplate, endpoint, adminUsername, adminPassword);
        }
    }

    @After
    public void cleanup() {
    	// workaround for intermittent issue with logout
        sessionId = null;
    }

    @Test
    public void test() throws Exception {
        // fetch valid token
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token.toString());
        
        Thread.sleep(1000);
        
        // fetch valid token
        JSONObject token2 = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token2.toString());
    }
    
//    @Test
    public void accessTokenRevokeWithBasicAuth() throws Exception {

        logger.debug("accessTokenRevokeWithBasicAuth session " + sessionId);

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        long now = new Date().getTime() / 1000;
        String clientId = client.getClientId();

        // fetch valid token
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");
        Assert.assertTrue(StringUtils.isNotBlank(accessToken));

        // build basic auth for client
        String auth = client.getClientId() + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);

        // call introspection endpoint to ensure token is active
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authHeader);
        // request json
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("token", accessToken);
        map.add("token_type_hint", "access_token");
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(
                server + ":" + port + contextPath + TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL,
                HttpMethod.POST, entity,
                String.class);

        // expect 200 with JSON
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        JSONObject json = new JSONObject(response.getBody());
        logger.trace(json.toString());

        // active should be true for valid tokens
        Assert.assertTrue(json.getBoolean("active"));

        // call revoke on token
        // post as form data
        map = new LinkedMultiValueMap<String, String>();
        map.add("token", accessToken);
        map.add("token_type_hint", "access_token");
        entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response2 = restTemplate.exchange(
                server + ":" + port + contextPath + TokenRevocationEndpoint.TOKEN_REVOCATION_URL,
                HttpMethod.POST, entity,
                String.class);

        // expect 200 with JSON
        Assert.assertTrue((response2.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response2.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // call introspection endpoint to ensure token is disabled
        // post as form data
        map = new LinkedMultiValueMap<String, String>();
        map.add("token", accessToken);
        map.add("token_type_hint", "access_token");
        entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response3 = restTemplate.exchange(
                server + ":" + port + contextPath + TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL,
                HttpMethod.POST, entity,
                String.class);

        // expect 200 with JSON
        Assert.assertTrue((response3.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response3.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        json = new JSONObject(response3.getBody());
        logger.trace(json.toString());

        // active should be false for revoked token
        Assert.assertFalse(json.getBoolean("active"));
    }

//    @Test
    public void refreshTokenRevokeWithBasicAuth() throws Exception {

        logger.debug("refreshTokenRevokeWithBasicAuth session " + sessionId);

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        long now = new Date().getTime() / 1000;
        String clientId = client.getClientId();

        // fetch valid token
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");
        String refreshToken = token.getString("refresh_token");

        Assert.assertTrue(StringUtils.isNotBlank(accessToken));
        Assert.assertTrue(StringUtils.isNotBlank(refreshToken));

        // build basic auth for client
        String auth = client.getClientId() + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);

        // call introspection endpoint to ensure token is active
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authHeader);
        // request json
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("token", accessToken);
        map.add("token_type_hint", "access_token");
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(
                server + ":" + port + contextPath + TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL,
                HttpMethod.POST, entity,
                String.class);

        // expect 200 with JSON
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        JSONObject json = new JSONObject(response.getBody());
        logger.trace(json.toString());

        // active should be true for valid tokens
        Assert.assertTrue(json.getBoolean("active"));

        // call revoke on refresh token
        // will remove associated accessToken
        // post as form data
        map = new LinkedMultiValueMap<String, String>();
        map.add("token", refreshToken);
        map.add("token_type_hint", "refresh_token");
        entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response2 = restTemplate.exchange(
                server + ":" + port + contextPath + TokenRevocationEndpoint.TOKEN_REVOCATION_URL,
                HttpMethod.POST, entity,
                String.class);

        // expect 200 with JSON
        Assert.assertTrue((response2.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response2.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // call introspection endpoint to ensure token is disabled
        // post as form data
        map = new LinkedMultiValueMap<String, String>();
        map.add("token", accessToken);
        map.add("token_type_hint", "access_token");
        entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response3 = restTemplate.exchange(
                server + ":" + port + contextPath + TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL,
                HttpMethod.POST, entity,
                String.class);

        // expect 200 with JSON
        Assert.assertTrue((response3.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response3.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        json = new JSONObject(response3.getBody());
        logger.trace(json.toString());

        // active should be false for revoked token
        Assert.assertFalse(json.getBoolean("active"));
    }
}
