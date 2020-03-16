package it.smartcommunitylab.aac.test.oauth;

import java.util.Date;
import java.util.UUID;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.test.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=false" })
@ActiveProfiles("test")
@EnableConfigurationProperties
public class ImplicitGrantTest extends OAuth2BaseTest {
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

    public final static String[] GRANT_TYPES = { "implicit" };

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
        sessionId = null;
    }

    @Test
    public void implicitGrantWithTokenAsFragment()
            throws Exception {

        ClientAppBasic client = getClient();

        logger.debug("implicit grant (as fragment) with client " + client.getClientId() + " and user session "
                + sessionId);

        // check client
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        // request token
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", sessionId));

        long now = new Date().getTime() / 1000;
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
                        + "&response_type=token"
                        + "&response_mode=fragment"
                        + "&state=" + state,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to pre-authorize
        Assert.assertTrue((response.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // expect something like
        // http://localhost:41379/aac/eauth/pre-authorize?client_id=e9f50bd4-d6ba-4896-b96c-a9bd45cc2973&redirect_uri=http%253A%252F%252Flocalhost%253A41379&scope=openid&response_type=token&response_mode=fragment&nonce=123as
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
        Assert.assertEquals(parameters.getFirst("response_type"), "token");
        Assert.assertEquals(parameters.getFirst("response_mode"), "fragment");
        Assert.assertEquals(parameters.getFirst("state"), state);

        // call pre-auth to fetch tokens as fragments
        ResponseEntity<String> response2 = restTemplate.exchange(
                locationURL,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to location with fragments
        Assert.assertTrue((response2.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response2.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // extract fragments
        locationURL = response2.getHeaders().getFirst(HttpHeaders.LOCATION);
        logger.trace(locationURL);

        UriComponents uri = UriComponentsBuilder.fromUriString(locationURL)
                .build();

        String fragment = uri.getFragment();
        // parse as local url to extract as query params
        parameters = UriComponentsBuilder.fromUriString(server + ":" + port + "/?" + fragment)
                .build()
                .getQueryParams();

        String accessToken = parameters.getFirst("access_token");
        String tokenType = parameters.getFirst("token_type");
        int expiresIn = Integer.parseInt(parameters.getFirst("expires_in"));

        logger.debug("accessToken " + accessToken);

        // basic validation
        Assert.assertTrue(StringUtils.isNotEmpty(accessToken));
        Assert.assertEquals("Bearer", tokenType);
        Assert.assertEquals(state, parameters.getFirst("state"));

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);

        // ensure NO refresh token is issued
        Assert.assertTrue(parameters.getFirst("refresh_token") == null);

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
