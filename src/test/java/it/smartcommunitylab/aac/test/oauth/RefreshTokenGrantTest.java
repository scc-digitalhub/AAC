package it.smartcommunitylab.aac.test.oauth;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import it.smartcommunitylab.aac.test.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=true" })
@ActiveProfiles("test")
public class RefreshTokenGrantTest extends OAuth2BaseTest {
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

    private static String sessionId;

    public final static String[] SCOPES = { "profile", "email" };

    public final static String[] GRANT_TYPES = { "authorization_code", "refresh_token" };

    @Before
    public void init() {
        String endpoint = server + ":" + port;
        super.init();

        if (StringUtils.isEmpty(sessionId)) {
            // login and validate session
            sessionId = TestUtils.login(restTemplate, endpoint, getUserName(), getUserPassword());
        }
    }

    @After
    public void cleanup() {
        // workaround for intermittent issue with logout
        sessionId = null;
    }

    @Test
    public void refreshTokenWithBasicAuth() throws Exception {

        ClientAppBasic client = getClient();

        logger.debug(
                "refresh_token grant (form+secret_basic) with client " + client.getClientId() + " and user session "
                        + sessionId);

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        // fetch valid token
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, SCOPES);

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");
        String refreshToken = token.getString("refresh_token");

        logger.debug("accessToken " + accessToken);
        logger.debug("refreshToken " + refreshToken);

        Assert.assertTrue(StringUtils.isNotBlank(accessToken));
        Assert.assertTrue(StringUtils.isNotBlank(refreshToken));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // build basic auth for client
        String auth = client.getClientId() + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);
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

        String accessToken2 = json.getString("access_token");
        String refreshToken2 = json.optString("refresh_token");
        String tokenType = json.getString("token_type");
        int expiresIn = json.getInt("expires_in");

        logger.debug("accessToken " + accessToken2);
        logger.debug("refreshToken " + refreshToken2);

        // basic validation
        Assert.assertTrue(StringUtils.isNotEmpty(accessToken2));
        Assert.assertTrue(StringUtils.isNotEmpty(refreshToken2));
        Assert.assertEquals("Bearer", tokenType);

        // check scope
        for (String s : SCOPES) {
            Assert.assertTrue(json.getString("scope").contains(s));
        }

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);

    }

    @Test
    public void refreshTokenRestrictScope() throws Exception {

        ClientAppBasic client = getClient();

        logger.debug(
                "refresh_token grant (form+secret_basic) restrict scope with client " + client.getClientId()
                        + " and user session "
                        + sessionId);

        String scope = SCOPES[0];

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        // fetch valid token
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, SCOPES);

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");
        String refreshToken = token.getString("refresh_token");

        logger.debug("accessToken " + accessToken);
        logger.debug("refreshToken " + refreshToken);

        Assert.assertTrue(StringUtils.isNotBlank(accessToken));
        Assert.assertTrue(StringUtils.isNotBlank(refreshToken));

        String[] scopes = org.springframework.util.StringUtils.delimitedListToStringArray(token.getString("scope"), " ");
        logger.debug("scopes " + Arrays.toString(scopes));

        for (String s : SCOPES) {
            Assert.assertTrue(ArrayUtils.contains(scopes, s));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // build basic auth for client
        String auth = client.getClientId() + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);
        // restrict scopes for new token
        map.add("scope", scope);

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

        String accessToken2 = json.getString("access_token");
        String refreshToken2 = json.optString("refresh_token");
        String tokenType = json.getString("token_type");
        int expiresIn = json.getInt("expires_in");

        logger.debug("accessToken " + accessToken2);
        logger.debug("refreshToken " + refreshToken2);

        // basic validation
        Assert.assertTrue(StringUtils.isNotEmpty(accessToken2));
        Assert.assertTrue(StringUtils.isNotEmpty(refreshToken2));
        Assert.assertEquals("Bearer", tokenType);

        // check scope
        String[] scopes2 = org.springframework.util.StringUtils.delimitedListToStringArray(json.getString("scope"), " ");
        logger.debug("scopes " + Arrays.toString(scopes2));

        for (String s : SCOPES) {
            if (s.equals(scope)) {
                Assert.assertTrue(ArrayUtils.contains(scopes2, s));
            } else {
                Assert.assertTrue(!ArrayUtils.contains(scopes2, s));
            }
        }

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);

    }

    /*
     * Helpers
     */
    @Override
    protected String[] getScopes() {
        return SCOPES;
    }

    @Override
    protected String[] getGrantTypes() {
        return GRANT_TYPES;
    }
}
