package it.smartcommunitylab.aac.test.oauth;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.JOSEException;

import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.test.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=false" })
@ActiveProfiles("test")
@EnableConfigurationProperties
public class AuthorizationCodeGrantTest extends OAuth2BaseTest {
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

    private static String sessionId;

    public final static String[] SCOPES = { "profile" };

    public final static String[] GRANT_TYPES = { "authorization_code", "refresh_token" };

    @Before
    public void init() {
        String endpoint = server + ":" + port;
        super.init();

        if (StringUtils.isEmpty(sessionId)) {
            logger.error("session is null, create");

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
    public void authCodeGrantWithFormAuthSecretBasic()
            throws RestClientException, UnsupportedEncodingException, ParseException, JOSEException,
            NoSuchAlgorithmException, InvalidKeySpecException, JSONException {

        ClientAppBasic client = getClient();
        
        logger.debug("auth_code grant (form+secret_basic) with client " + client.getClientId() + " and user session "
                + sessionId);

        // check client
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        // request token
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", sessionId));

        String clientId = client.getClientId();
        String state = RandomStringUtils.random(5, true, true);
        // redirect does not need urlEncoding because there are no parameters
        String redirectURL = server + ":" + port;

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(
                server + ":" + port + contextPath + "/eauth/authorize?"
                        + "client_id=" + clientId
                        + "&redirect_uri=" + redirectURL
                        + "&scope=" + String.join(" ", SCOPES)
                        + "&response_type=code"
                        + "&response_mode=query"
                        + "&state=" + state,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to pre-authorize
        Assert.assertTrue((response.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // expect something like
        // http://localhost:41379/aac/eauth/pre-authorize?client_id=e9f50bd4-d6ba-4896-b96c-a9bd45cc2973&redirect_uri=http%253A%252F%252Flocalhost%253A41379&scope=openid&response_type=code&response_mode=query&nonce=123as
        String locationURL = response.getHeaders().getFirst(HttpHeaders.LOCATION);

        logger.trace(locationURL);

        // extract parameters from redirect to validate
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(locationURL)
                .build()
                .getQueryParams();

        Assert.assertEquals(parameters.getFirst("client_id"), clientId);
        Assert.assertEquals(parameters.getFirst("redirect_uri"), redirectURL);
        for (String s : SCOPES) {
            Assert.assertTrue(parameters.getFirst("scope").contains(s));
        }
        Assert.assertEquals(parameters.getFirst("response_type"), "code");
        Assert.assertEquals(parameters.getFirst("response_mode"), "query");
        Assert.assertEquals(parameters.getFirst("state"), state);

        // call pre-auth to fetch code
        ResponseEntity<String> response2 = restTemplate.exchange(locationURL,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to location with query string
        Assert.assertTrue((response2.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response2.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // extract query parameters
        locationURL = response2.getHeaders().getFirst(HttpHeaders.LOCATION);
        logger.trace(locationURL);

        parameters = UriComponentsBuilder.fromUriString(locationURL)
                .build()
                .getQueryParams();

        String code = parameters.getFirst("code");
        Assert.assertEquals(parameters.getFirst("state"), state);

        logger.debug("received auth_code " + code);

        // exchange code for tokens
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // build basic auth for client
        String auth = clientId + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectURL);
        map.add("code", code);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response3 = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 200 with json in body
        Assert.assertTrue((response3.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response3.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        logger.trace(response3.getBody());

        // parse
        org.json.JSONObject json = new org.json.JSONObject(response3.getBody());

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
        for (String s : SCOPES) {
            Assert.assertTrue(json.getString("scope").contains(s));
        }

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);

    }

    @Test
    public void authCodeGrantWithFormAuthSecretPost()
            throws RestClientException, UnsupportedEncodingException, ParseException, JOSEException,
            NoSuchAlgorithmException, InvalidKeySpecException, JSONException {

        ClientAppBasic client = getClient();

        logger.debug("auth_code grant (form+secret_post) with client " + client.getClientId() + " and user session "
                + sessionId);

        // check client
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        // request token
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", sessionId));

        String clientId = client.getClientId();
        String state = RandomStringUtils.random(5, true, true);
        // redirect does not need urlEncoding because there are no parameters
        String redirectURL = server + ":" + port;

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(
                server + ":" + port + contextPath + "/eauth/authorize?"
                        + "client_id=" + clientId
                        + "&redirect_uri=" + redirectURL
                        + "&scope=" + String.join(" ", SCOPES)
                        + "&response_type=code"
                        + "&response_mode=query"
                        + "&state=" + state,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to pre-authorize
        Assert.assertTrue((response.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // expect something like
        // http://localhost:41379/aac/eauth/pre-authorize?client_id=e9f50bd4-d6ba-4896-b96c-a9bd45cc2973&redirect_uri=http%253A%252F%252Flocalhost%253A41379&scope=openid&response_type=code&response_mode=query&nonce=123as
        String locationURL = response.getHeaders().getFirst(HttpHeaders.LOCATION);

        logger.trace(locationURL);

        // extract parameters from redirect to validate
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(locationURL)
                .build()
                .getQueryParams();

        Assert.assertEquals(parameters.getFirst("client_id"), clientId);
        Assert.assertEquals(parameters.getFirst("redirect_uri"), redirectURL);
        for (String s : SCOPES) {
            Assert.assertTrue(parameters.getFirst("scope").contains(s));
        }
        Assert.assertEquals(parameters.getFirst("response_type"), "code");
        Assert.assertEquals(parameters.getFirst("response_mode"), "query");
        Assert.assertEquals(parameters.getFirst("state"), state);

        // call pre-auth to fetch code
        ResponseEntity<String> response2 = restTemplate.exchange(locationURL,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to location with query string
        Assert.assertTrue((response2.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response2.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // extract query parameters
        locationURL = response2.getHeaders().getFirst(HttpHeaders.LOCATION);
        logger.trace(locationURL);

        parameters = UriComponentsBuilder.fromUriString(locationURL)
                .build()
                .getQueryParams();

        String code = parameters.getFirst("code");
        Assert.assertEquals(parameters.getFirst("state"), state);

        logger.debug("received auth_code " + code);

        // exchange code for tokens
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectURL);
        map.add("code", code);
        // add client credentials in form
        map.add("client_id", clientId);
        map.add("client_secret", client.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response3 = restTemplate.postForEntity(
                server + ":" + port + contextPath + "/oauth/token",
                entity,
                String.class);

        // expect 200 with json in body
        Assert.assertTrue((response3.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response3.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        org.json.JSONObject json = new org.json.JSONObject(response3.getBody());

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
        for (String s : SCOPES) {
            Assert.assertTrue(json.getString("scope").contains(s));
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
