package it.smartcommunitylab.aac.test.openid;

import org.json.JSONException;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
public class MetadataTest {
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

    @Value("${jwt.issuer}")
    private String issuer;

    public final static String ENDPOINT = "/.well-known/openid-configuration";

    /*
     * Claims definition as per
     * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
     */

    private static final String[] requiredClaims = {
            "issuer",
            "authorization_endpoint",
            "token_endpoint",
            "jwks_uri",
            "response_types_supported",
            "subject_types_supported",
            "id_token_signing_alg_values_supported"
    };
    private static final String[] recommendedClaims = {
            "userinfo_endpoint",
            // "registration_endpoint", //disabled, not supported in AAC
            "scopes_supported",
            "claims_supported"

    };

    private static final String[] optionalClaims = {
            "response_modes_supported",
            "grant_types_supported",
            "acr_values_supported",
            "id_token_encryption_alg_values_supported",
            "id_token_encryption_enc_values_supported",
            "userinfo_signing_alg_values_supported",
            "userinfo_encryption_alg_values_supported",
            "userinfo_encryption_enc_values_supported",
            "request_object_signing_alg_values_supported",
            "request_object_encryption_alg_values_supported",
            "request_object_encryption_enc_values_supported",
            "token_endpoint_auth_methods_supported",
            "token_endpoint_auth_signing_alg_values_supported",
            "display_values_supported",
            "claim_types_supported",
            "service_documentation",
            "claims_locales_supported",
            "ui_locales_supported",
            "claims_parameter_supported",
            "request_parameter_supported",
            "request_uri_parameter_supported",
            "require_request_uri_registration",
            "op_policy_uri",
            "op_tos_uri"
    };

    private static final String[] sessionClaims = {
            "check_session_iframe",
            "end_session_endpoint"
    };

    @Before
    public void init() {
    }

    @After
    public void cleanup() {
    }

    @Test
    public void validateConfiguration() {
        logger.debug("validateConfiguration");
        // load config from url
        ResponseEntity<String> response = restTemplate.getForEntity(
                server + ":" + port + contextPath + ENDPOINT,
                String.class);
        // validate response 200ish
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));
        // validate application/json
        Assert.assertTrue(response.getHeaders().getContentType().equals(MediaType.APPLICATION_JSON_UTF8));

    }

    @Test
    public void validateIssuer() throws JSONException {
        logger.debug("validateIssuer");
        // load config from url
        ResponseEntity<String> response = restTemplate.getForEntity(
                server + ":" + port + contextPath + ENDPOINT,
                String.class);

        JSONObject json = new JSONObject(response.getBody());
        String responseIssuer = json.getString("issuer");
        // ignore fetch url and validate with configuration
        Assert.assertTrue(issuer.equals(responseIssuer));

    }

    @Test
    public void validateproviderClaims() throws JSONException {
        logger.debug("validateClaims");
        // load config from url
        ResponseEntity<String> response = restTemplate.getForEntity(
                server + ":" + port + contextPath + ENDPOINT,
                String.class);

        JSONObject json = new JSONObject(response.getBody());

        // validate required
        for (String key : requiredClaims) {
            // only check if it exists
            Assert.assertTrue("missing " + key, json.has(key));
        }

        // validate recommended
        for (String key : recommendedClaims) {
            // only check if it exists
            Assert.assertTrue("missing " + key, json.has(key));
        }

        // validate if any claim not in spec?
    }

    @Test
    public void validateEndpoints() throws JSONException {
        logger.debug("validateEndpoints");
        // load config from url
        ResponseEntity<String> response = restTemplate.getForEntity(
                server + ":" + port + contextPath + ENDPOINT,
                String.class);

        JSONObject json = new JSONObject(response.getBody());

        // validate against issuer
        // see
        // https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationValidation
        String responseIssuer = json.getString("issuer");

        while(json.keys().hasNext()) {
            String key = json.keys().next().toString();

            if (key.endsWith("_endpoint")) {
                // check if it matches issuer
                String url = json.getString(key);
                Assert.assertTrue("wrong url in " + key, url.startsWith(responseIssuer));
            }
        }
    }
}
