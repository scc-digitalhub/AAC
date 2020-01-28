package it.smartcommunitylab.aac.test.oauth;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import com.nimbusds.jose.JOSEException;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.test.openid.OpenidUtils;
import it.smartcommunitylab.aac.test.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=false" })
@ActiveProfiles("test")
@EnableConfigurationProperties
public class TokenIntrospectionTest {
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

    private final static String GRANT_TYPE = "authorization_code";

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
                        GRANT_TYPE, new String[] { SCOPE },
                        endpoint));
            } catch (Exception e) {
                // TODO Auto-generated catch block
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
    }

    @Test
    public void tokenIntrospectionWithBasicAuth() throws RestClientException, UnsupportedEncodingException,
            NoSuchAlgorithmException, InvalidKeySpecException, ParseException, JOSEException {

        logger.debug("tokenIntrospectionWithBasicAuth");

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        long now = new Date().getTime() / 1000;
        String clientId = client.getClientId();

        // fetch valid token to introspect
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");

        // build basic auth for client
        String auth = client.getClientId() + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);

        // call introspection endpoint
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

        logger.debug(response.getStatusCode().toString());
        logger.debug(response.getHeaders().getContentType().toString());
        logger.trace(response.getBody());

        // expect 200 with JSON
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        JSONObject json = new JSONObject(response.getBody());

        /*
         * verify claims https://tools.ietf.org/html/rfc7662#section-2.2
         */
        // REQUIRED
        // active should be true for valid tokens
        Assert.assertTrue(json.getBoolean("active"));

        // OPTIONAL
        // scope should contain the requested one
        Assert.assertTrue(json.getString("scope").contains(SCOPE));

        // client_id should match
        Assert.assertEquals(client.getClientId(), json.getString("client_id"));

        // token_type is Bearer
        Assert.assertEquals("Bearer", json.getString("token_type"));

        // token expires in the future
        Assert.assertTrue(json.getInt("exp") > now);
        // token issued recently (12 hours)
        Assert.assertTrue(json.getInt("iat") > (now - 43200));
        // nbf is after iat
        Assert.assertTrue(json.getInt("nbf") >= json.getInt("iat"));

        // subject is not empty
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("sub")));

        // issuer matches
        Assert.assertEquals(issuer, json.getString("iss"));

        // audience should contain or match ourselves
        String[] audiences = toStringArray(json.optJSONArray("aud"));
        if (audiences != null) {
            // multiple audiences
            Assert.assertTrue(Arrays.asList(audiences).contains(clientId));
        } else {
            Assert.assertEquals(clientId, json.getString("aud"));
        }

        // do not check jti
    }

    // DISABLED not supported
//    @Test
//    public void tokenIntrospectionWithFormPost() throws RestClientException, UnsupportedEncodingException,
//            NoSuchAlgorithmException, InvalidKeySpecException, ParseException, JOSEException {
//        logger.debug("tokenIntrospectionWithFormPost");
//
//        // check client and session
//        Assert.assertNotNull(client);
//        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));
//
//        long now = new Date().getTime() / 1000;
//        String clientId = client.getClientId();
//
//        // fetch valid token to introspect
//        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port, client,
//                sessionId, SCOPES);
//
//        logger.trace(token.toString());
//
//        // fetch accessToken
//        String accessToken = token.getString("access_token");
//
//        // call introspection endpoint
//        HttpHeaders headers = new HttpHeaders();
//        // request json
//        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
//
//        // post as form data
//        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
//        map.add("client_id", client.getClientId());
//        map.add("client_secret", client.getClientSecret());
//        map.add("token", accessToken);
//        map.add("token_type_hint", "access_token");
//        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
//
//        // note: TESTrestTemplate does not follow redirects
//        ResponseEntity<String> response = restTemplate.exchange(
//                server + ":" + port + "/aac/token_introspection",
//                HttpMethod.POST, entity,
//                String.class);
//
//        logger.debug(response.getStatusCode().toString());
//        logger.debug(response.getHeaders().getContentType().toString());
//        logger.trace(response.getBody());
//
//        // expect 200 with JSON
//        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
//        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
//
//        // parse
//        JSONObject json = new JSONObject(response.getBody());
//
//        /*
//         * verify claims https://tools.ietf.org/html/rfc7662#section-2.2
//         */
//        // REQUIRED
//        // active should be true for valid tokens
//        Assert.assertTrue(json.getBoolean("active"));
//
//        // OPTIONAL
//        // scope should contain the requested one
//        Assert.assertTrue(json.getString("scope").contains("profile"));
//
//        // client_id should match
//        Assert.assertEquals(client.getClientId(), json.getString("client_id"));
//
//        // token_type is Bearer
//        Assert.assertEquals("Bearer", json.getString("token_type"));
//
//        // token expires in the future
//        Assert.assertTrue(json.getInt("exp") > now);
//        // token issued recently (12 hours)
//        Assert.assertTrue(json.getInt("iat") > (now - 43200));
//        // nbf is after iat
//        Assert.assertTrue(json.getInt("nbf") >= json.getInt("iat"));
//
//        // subject is not empty
//        Assert.assertTrue(StringUtils.isNotBlank(json.getString("sub")));
//
//        // issuer matches
//        Assert.assertEquals(issuer, json.getString("iss"));
//
//        // audience should contain or match ourselves
//        String[] audiences = toStringArray(json.optJSONArray("aud"));
//        if (audiences != null) {
//            // multiple audiences
//            Assert.assertTrue(Arrays.asList(audiences).contains(clientId));
//        } else {
//            Assert.assertEquals(clientId, json.getString("aud"));
//        }
//
//        // do not check jti
//    }

    @Test
    public void tokenIntrospectionInactive() {
        logger.debug("tokenIntrospectionInactive");

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        long now = new Date().getTime() / 1000;
        String clientId = client.getClientId();

        // use random invalid token to introspect
        String accessToken = RandomStringUtils.random(25, true, true);

        // build basic auth for client
        String auth = client.getClientId() + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);

        // call introspection endpoint
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

        logger.debug(response.getStatusCode().toString());
        logger.debug(response.getHeaders().getContentType().toString());
        logger.trace(response.getBody());

        // expect 200 with JSON
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        JSONObject json = new JSONObject(response.getBody());

        /*
         * verify claims https://tools.ietf.org/html/rfc7662#section-2.2
         */
        // REQUIRED
        // active should be false for valid tokens
        Assert.assertFalse(json.getBoolean("active"));

        // REQUIRED
        // no other claims in response to avoid information leak
        Assert.assertTrue(json.keySet().size() == 1);
    }

    @Test
    public void tokenIntrospectionUnauthorized() throws RestClientException, UnsupportedEncodingException,
            NoSuchAlgorithmException, InvalidKeySpecException, ParseException, JOSEException {
        logger.debug("tokenIntrospectionUnauthorized");

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        long now = new Date().getTime() / 1000;

        // fetch valid token to introspect
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");

        // call introspection endpoint
        HttpHeaders headers = new HttpHeaders();
        // bogus auth
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + RandomStringUtils.random(25, true, true));
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

        logger.debug(response.getStatusCode().toString());
        logger.debug(response.getHeaders().getContentType().toString());
        logger.trace(response.getBody());

        // expect 401
        Assert.assertTrue((response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)));

    }

    @Test
    public void tokenIntrospectionUnauthenticated() throws RestClientException, UnsupportedEncodingException,
            NoSuchAlgorithmException, InvalidKeySpecException, ParseException, JOSEException {
        logger.debug("tokenIntrospectionUnauthorized");

        // check client and session
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        long now = new Date().getTime() / 1000;

        // fetch valid token to introspect
        JSONObject token = OAuthUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, new String[] { SCOPE });

        logger.trace(token.toString());

        // fetch accessToken
        String accessToken = token.getString("access_token");

        // call introspection endpoint
        HttpHeaders headers = new HttpHeaders();
        // no auth
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

        logger.debug(response.getStatusCode().toString());
        logger.debug(response.getHeaders().getContentType().toString());
        logger.trace(response.getBody());

        // expect 401
        Assert.assertTrue((response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)));

    }

    /*
     * Helpers
     */

    private String[] toStringArray(org.json.JSONArray array) {
        if (array == null) {
            return null;
        }

        String[] arr = new String[array.length()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = array.optString(i);
        }
        return arr;
    }
}
