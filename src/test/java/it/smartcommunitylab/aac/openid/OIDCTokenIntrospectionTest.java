package it.smartcommunitylab.aac.openid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2ConfigUtils;
import it.smartcommunitylab.aac.oauth.OAuth2TestConfig.UserRegistration;
import it.smartcommunitylab.aac.oauth.OAuth2TestUtils;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * OAuth 2.0 Token Introspection
 * as per 
 * https://www.rfc-editor.org/rfc/rfc7662
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OIDCTokenIntrospectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

    @Value("${application.url}")
    private String applicationURL;

    private String userId;
    private String username;
    private String password;

    private String clientId;
    private String clientSecret;
    private String clientJwks;
    private String client2Id;
    private String client2Secret;

    @BeforeEach
    public void setUp() {
        if (clientId == null || clientSecret == null || clientJwks == null || client2Id == null
                || client2Secret == null) {
            List<ClientRegistration> clients = OAuth2ConfigUtils.with(config).clients();
            assertThat(clients.size()).isGreaterThanOrEqualTo(2);

            ClientRegistration client1 = clients.get(0);
            clientId = client1.getClientId();
            clientSecret = client1.getClientSecret();
            clientJwks = client1.getJwks();

            ClientRegistration client2 = clients.get(1);
            client2Id = client2.getClientId();
            client2Secret = client2.getClientSecret();
        }

        if (clientId == null || clientSecret == null || clientJwks == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (userId == null || username == null || password == null) {
            UserRegistration user = OAuth2ConfigUtils.with(config).user();
            assertThat(user).isNotNull();

            userId = user.getUserId();
            username = user.getUsername();
            password = user.getPassword();
        }

        if (username == null || password == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    public void introspectMetadataIsAvailable() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // check that required keys are available
        REQUIRED_METADATA.forEach(k -> {
            assertThat(k).isIn(metadata.keySet());
        });
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userIdTokenNoHintWithBasicAuthIsNotSupportedTest() throws Exception {
        // fetch a valid user id token
        String idToken = OAuth2TestUtils.getUserIdTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, idToken);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        // token should be active
        // TODO support id token introspection, for now AAC should return false
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userIdTokenExactHintWithBasicAuthIsNotSupportedTest() throws Exception {
        // fetch a valid user id token
        String idToken = OAuth2TestUtils.getUserIdTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, idToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OidcParameterNames.ID_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        // token should be active
        // TODO support id token introspection, for now AAC should return false
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userIdTokenWrongHintWithBasicAuthIsNotSupportedTest() throws Exception {
        // fetch a valid user id token
        String idToken = OAuth2TestUtils.getUserIdTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, idToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.ACCESS_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        // token should be active
        // TODO support id token introspection, for now AAC should return false
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void invalidIdTokenNoHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-id-token");

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        // token should be inactive
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(false);

        // no other info is provided
        assertThat(response.keySet()).containsOnly(OAuth2TokenIntrospectionClaimNames.ACTIVE);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void invalidIdTokenExactHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-id-token");
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OidcParameterNames.ID_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        // token should be inactive
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(false);

        // no other info is provided
        assertThat(response.keySet()).containsOnly(OAuth2TokenIntrospectionClaimNames.ACTIVE);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void invalidIdTokenWrongHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-id-token");
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.ACCESS_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        // token should be inactive
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(false);

        // no other info is provided
        assertThat(response.keySet()).containsOnly(OAuth2TokenIntrospectionClaimNames.ACTIVE);
    }

    /*
     * endpoints
     */
    public final static String METADATA_URL = "/.well-known/oauth-authorization-server";
    private final static String INTROSPECTION_URL = TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL;

    /*
     * claims
     */
    public final static Set<String> REQUIRED_METADATA;
    public final static String OAUTH2_METADATA_ISSUER = "issuer";
    public final static String OAUTH2_METADATA_INTROSPECTION_ENDPOINT = "introspection_endpoint";
    public final static String OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_METHODS = "introspection_endpoint_auth_methods_supported";
    public final static String OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_SIGNIN_ALG = "introspection_endpoint_auth_signing_alg_values_supported";

    static {
        REQUIRED_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(
                List.of(OAUTH2_METADATA_INTROSPECTION_ENDPOINT,
                        OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_METHODS,
                        OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_SIGNIN_ALG)));

    }
    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
}
