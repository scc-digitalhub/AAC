package it.smartcommunitylab.aac.test.openid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import com.nimbusds.jose.JWSObject;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import net.minidev.json.JSONObject;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
public class ImplicitFlowTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @LocalServerPort
    private int port;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    private String sessionId;

    private ClientDetails client;

    @Before
    public void init() {
        if (client == null) {
            try {

                User admin = userRepository.findByUsername(adminUsername);
                client = createClient(UUID.randomUUID().toString(), admin.getId(), "implicit", "openid");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                client = null;
            }
        }

        if (StringUtils.isEmpty(sessionId)) {
            // login and validate session
            sessionId = login(adminUsername, adminPassword);
        }

    }

    @After
    public void cleanup() {
    }

    @Test
    public void implicitFlow() throws RestClientException, UnsupportedEncodingException, ParseException {
        // check client
        Assert.assertNotNull(client);
        Assert.assertTrue(StringUtils.isNotEmpty(sessionId));

        logger.debug("implicit flow with client " + client.getClientId() + " and user session " + sessionId);

        // request token
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", sessionId));

        String clientId = client.getClientId();
        String nonce = RandomStringUtils.random(5, true, true);
        String state = RandomStringUtils.random(5, true, true);
        // redirect does not need urlEncoding because there are no parameters
        String redirectURL = "http://localhost" + port;

//        // mock
//        redirectURL = "http://localhost:8080";
//        clientId = "test_client";

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/aac/eauth/authorize?"
                        + "client_id=" + clientId
                        + "&redirect_uri=" + redirectURL
                        + "&scope=openid"
                        + "&response_type=token"
                        + "&response_mode=fragment"
                        + "&state=" + state
                        + "&nonce=" + nonce,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to pre-authorize
        Assert.assertTrue((response.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        logger.debug(response.getBody());

        // expect something like
        // http://localhost:41379/aac/eauth/pre-authorize?client_id=e9f50bd4-d6ba-4896-b96c-a9bd45cc2973&redirect_uri=http%253A%252F%252Flocalhost%253A41379&scope=openid&response_type=token&response_mode=fragment&nonce=123as
        String locationURL = response.getHeaders().getFirst(HttpHeaders.LOCATION);

        logger.debug(locationURL);

        // extract parameters from redirect to validate
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(locationURL)
                .build()
                .getQueryParams();

        Assert.assertEquals(parameters.getFirst("client_id"), clientId);
        Assert.assertEquals(parameters.getFirst("redirect_uri"), redirectURL);
        Assert.assertTrue(parameters.getFirst("scope").contains("openid"));
        Assert.assertEquals(parameters.getFirst("response_type"), "token");
        Assert.assertEquals(parameters.getFirst("response_mode"), "fragment");
        Assert.assertEquals(parameters.getFirst("nonce"), nonce);

        // call pre-auth to fetch tokens as fragments
        ResponseEntity<String> response2 = restTemplate.exchange(
                "http://localhost:" + port + "/aac/eauth/pre-authorize?"
                        + "client_id=" + clientId
                        + "&redirect_uri=" + redirectURL
                        + "&scope=openid"
                        + "&response_type=token"
                        + "&response_mode=fragment"
                        + "&state=" + state
                        + "&nonce=" + nonce,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // expect redirect to location with fragments
        Assert.assertTrue((response2.getStatusCode().is3xxRedirection()));
        Assert.assertEquals(response2.getHeaders().get(HttpHeaders.LOCATION).size(), 1);

        // extract fragments
        locationURL = response2.getHeaders().getFirst(HttpHeaders.LOCATION);
        logger.debug(locationURL);

        UriComponents uri = UriComponentsBuilder.fromUriString(locationURL)
                .build();

        String fragment = uri.getFragment();
        // parse as local url to extract as query params
        parameters = UriComponentsBuilder.fromUriString("http://localhost:" + port + "/?" + fragment)
                .build()
                .getQueryParams();

        String accessToken = parameters.getFirst("access_token");
        String tokenType = parameters.getFirst("token_type");
        String idToken = parameters.getFirst("id_token");
        int expiresIn = Integer.parseInt(parameters.getFirst("expires_in"));

        logger.debug("idToken " + idToken);

        Assert.assertTrue(StringUtils.isNotEmpty(accessToken));
        Assert.assertTrue(StringUtils.isNotEmpty(idToken));
        Assert.assertEquals("Bearer", tokenType);
        Assert.assertEquals(state, parameters.getFirst("state"));

        // validate expires (in seconds) at least 120s
        Assert.assertTrue(expiresIn > 120);

        // parse idToken and validate
        JWSObject jws = JWSObject.parse(idToken);

        // validate signature
        // TODO

        // validate payload
        // TODO
        JSONObject claims = jws.getPayload().toJSONObject();
        Assert.assertEquals(nonce, claims.getAsString("nonce"));

    }

    /*
     * Perform 2-step login to validate session on AAC+internal auth
     */

    private String login(String username, String password) {

        logger.debug("login as " + username + " with password " + password);
        String jsid = null;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // post as form data for login
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("username", username);
        map.add("password", password);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/aac/login",
                entity,
                String.class);

        // expect redirect from login to eauth
        if (response.getStatusCode().is3xxRedirection()) {
            // fetch set-cookie for session from headers
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            for (String cookie : cookies) {
                if (cookie.contains("JSESSIONID")) {
                    jsid = cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
                    break;
                }
            }

            if (jsid == null) {
                return null;
            }

            logger.debug("jsid header: " + jsid);

            // call eauth to validate session on aac
            headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", jsid));

            // use "/" as redirect
            ResponseEntity<String> response2 = restTemplate.exchange(
                    "http://localhost:" + port + "/aac/eauth/internal?target=%2F&email=admin",
                    HttpMethod.GET, new HttpEntity<Object>(headers),
                    String.class);

            if (!response2.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
                // error, clear
                jsid = "";
            }

        }

        logger.debug("sessionId: " + String.valueOf(jsid));
        return jsid;

    }

    private ClientDetails createClient(String clientId, long developerId, String grantTypes, String scopes)
            throws Exception {
        // manually add client to repo
        ClientDetailsEntity entity = new ClientDetailsEntity();

        // use local address as redirect
        String redirectUri = "http://localhost:" + port;

        entity.setName(clientId);
        entity.setClientId(clientId);
        entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
        entity.setAuthorizedGrantTypes(grantTypes);
        entity.setDeveloperId(developerId);
        entity.setClientSecret(UUID.randomUUID().toString());
        entity.setClientSecretMobile(UUID.randomUUID().toString());
        entity.setRedirectUri(redirectUri);
        entity.setMobileAppSchema(clientId);

        entity.setScope(scopes);

        ClientAppInfo info = new ClientAppInfo();
        info.setIdentityProviders(Collections.singletonMap(Config.IDP_INTERNAL, ClientAppInfo.APPROVED));
        info.setName(clientId);
        info.setDisplayName(clientId);
        info.setResourceApprovals(Collections.<String, Boolean>emptyMap());

        entity.setAdditionalInformation(info.toJson());
        return clientDetailsRepository.save(entity);
    }

    /*
     * 
     */

//    private WebTester tester;
//
//    @Before
//    public void init() {
//        tester = new WebTester();
//        tester.setBaseUrl("http://localhost:8080/test");
//    }
//
//    @After
//    public void cleanup() {
//        tester.closeBrowser();
//    }
//    
//    @Test
//    public void test1() {
//        tester.beginAt("home.xhtml"); //Open the browser on http://localhost:8080/test/home.xhtml
//        tester.clickLink("login");
//        tester.assertTitleEquals("Login");
//        tester.setTextField("username", "test");
//        tester.setTextField("password", "test123");
//        tester.submit();
//        tester.assertTitleEquals("Welcome, test!");
//        
//        tester.
//    }

}
