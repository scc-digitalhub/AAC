package it.smartcommunitylab.aac.test.openid;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.UUID;

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
import org.springframework.web.client.RestClientException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.openid.endpoint.UserInfoEndpoint;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.test.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
public class UserInfoTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String server = "http://localhost";

    @LocalServerPort
    private int port;

    @Value("${server.contextPath}")
    private String contextPath;

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${application.url}")
    private String applicationURL;

    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationManager registrationManager;

    private String sessionId;

    private ClientDetails client;

    private User user;

    private final String testUserName = "userinfotest@test.local";
    private final String testPassword = "passw123";

    private final static String[] SCOPES = { "openid", "profile", "email" };

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

        logger.trace("user is " + String.valueOf(user));

        if (client == null && user != null) {
            try {

                // use local address as redirect
                // also save it
                client = clientDetailsRepository.saveAndFlush(OpenidUtils.createClient(
                        UUID.randomUUID().toString(),
                        user.getId(),
                        "authorization_code", SCOPES,
                        endpoint));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                client = null;
            }
        }

        logger.trace("client is " + String.valueOf(client));

        if (StringUtils.isEmpty(sessionId)) {
            // login and validate session
            sessionId = TestUtils.login(restTemplate, endpoint, testUserName, testPassword);
        }

        logger.trace("sessionId is " + String.valueOf(sessionId));

    }

    @After
    public void cleanup() {
    }

    @Test
    public void fetchUserInfoAsJsonViaGet()
            throws RestClientException, UnsupportedEncodingException, NoSuchAlgorithmException,
            InvalidKeySpecException, ParseException, JOSEException {
        logger.debug("fetchUserInfo");

        // check context
        Assert.assertNotNull(user);
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        logger.trace(client.toString());
        logger.trace(client.getScope().toString());

        // fetch accessToken
        JSONObject token = OpenidUtils.getTokenViaAuthCode(restTemplate, server + ":" + port + contextPath, client,
                sessionId, SCOPES);

        logger.trace(token.toString());

        String accessToken = token.getString("access_token");
        String idToken = token.getString("id_token");
        int expiresIn = token.getInt("expires_in");

        // call userInfo endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        // request json
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(
                server + ":" + port + contextPath + UserInfoEndpoint.USERINFO_URL,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        logger.debug(response.getStatusCode().toString());
        logger.debug(response.getHeaders().getContentType().toString());
        logger.trace(response.getBody());

        // expect 200 with JSON
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        Assert.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);

        // parse
        JSONObject json = new JSONObject(response.getBody());
        SignedJWT jws = SignedJWT.parse(idToken);
        JSONObject claims = new JSONObject(jws.getPayload().toJSONObject().toJSONString());

        /*
         * validate claims see
         * https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
         */
        // openid == {sub}
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("sub")));

        // should match with id_token
        Assert.assertEquals(claims.getString("sub"), json.getString("sub"));

        // we requested scope {profile}
        // per-spec : profile =
        // name, family_name, given_name, middle_name, nickname, preferred_username,
        // profile, picture, website, gender, birthdate, zoneinfo, locale, and
        // updated_at
        // do note that only non - empty should appear
        //
        // validate base claims
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("name")));
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("given_name")));
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("family_name")));
        // validate username
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("preferred_username")));

        // we requested scope {email}
        // per-spec : email = email, email_verified
        Assert.assertTrue(StringUtils.isNotBlank(json.getString("email")));
        // note this field shoud be boolean
        Assert.assertTrue(StringUtils.isNotBlank(json.optString("email_verified")));

        // also validate that NO claims are empty
        for (Object key : json.keySet()) {
            Assert.assertFalse(json.isNull((String) key));
        }
    }

    /*
     * Helpers
     */
    private User createUser(String userName, String password, String name, String surname) {
        // username == email
        return registrationManager.registerOffline(name, surname, userName, password, null, false, null);

    }
}
