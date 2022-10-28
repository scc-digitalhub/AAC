package it.smartcommunitylab.aac.oauth;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.core.base.AbstractAccount;
import it.smartcommunitylab.aac.core.base.AbstractUserCredentials;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.token.ClientCredentialsGrantJwtAssertionAuthTest;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * OAuth 2.0 Token Revocation
 * as per 
 * https://www.rfc-editor.org/rfc/rfc7009
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OAuth2TokenRevocationTest {

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
        if (config == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (clientId == null || clientSecret == null || clientJwks == null) {
            RealmConfig rc = config.getRealms().iterator().next();
            if (rc == null || rc.getClientApps() == null) {
                throw new IllegalArgumentException("missing config");
            }

            Iterator<ClientApp> iter = rc.getClientApps().iterator();
            ClientApp client = iter.next();
            clientId = client.getClientId();
            clientSecret = (String) client.getConfiguration().get("clientSecret");
            clientJwks = (String) client.getConfiguration().get("jwks");

            ClientApp client2 = iter.next();
            if (client2 != null) {
                client2Id = client2.getClientId();
                client2Secret = (String) client2.getConfiguration().get("clientSecret");
            }
        }

        if (clientId == null || clientSecret == null || clientJwks == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (userId == null || username == null || password == null) {
            RealmConfig rc = config.getRealms().iterator().next();
            if (rc == null || rc.getUsers() == null || rc.getCredentials() == null) {
                throw new IllegalArgumentException("missing config");
            }
            AbstractUserCredentials cred = rc.getCredentials().stream().filter(c -> (c instanceof InternalUserPassword))
                    .findFirst().orElse(null);

            if (cred == null) {
                throw new IllegalArgumentException("missing config");
            }

            // pick matching user
            AbstractAccount account = rc.getUsers().stream()
                    .filter(u -> (u instanceof InternalUserAccount) && u.getAccountId().equals(cred.getAccountId()))
                    .findFirst().orElse(null);
            if (account == null) {
                throw new IllegalArgumentException("missing config");
            }

            userId = ((InternalUserAccount) account).getUserId();
            username = ((InternalUserAccount) account).getUsername();
            password = ((InternalUserPassword) cred).getPassword();
        }

        if (username == null || password == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    public void revocationMetadataIsAvailable() throws Exception {
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
    public void revocationEndpointMetadataIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // revocation endpoint is set
        assertThat(metadata.get(OAUTH2_METADATA_REVOCATION_ENDPOINT)).isNotNull().isInstanceOf(String.class);

        // issuer is set
        assertThat(metadata.get(OAUTH2_METADATA_ISSUER)).isNotNull().isInstanceOf(String.class);
        String issuer = (String) metadata.get(OAUTH2_METADATA_ISSUER);

        // should match definition (issuer + path)
        assertThat(metadata.get(OAUTH2_METADATA_REVOCATION_ENDPOINT)).isEqualTo(issuer + REVOCATION_URL);
    }

    @Test
    public void revocationAuthMethodMetadataIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // revocation endpoint auth method is set
        assertThat(metadata.get(OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_METHODS)).isNotNull()
                .isInstanceOf(List.class);

        // it contains expected but not NONE
        List<String> authMethods = Stream.of(
                AuthenticationMethod.CLIENT_SECRET_BASIC,
                AuthenticationMethod.CLIENT_SECRET_POST,
                AuthenticationMethod.CLIENT_SECRET_JWT,
                AuthenticationMethod.PRIVATE_KEY_JWT)
                .map(a -> a.getValue()).collect(Collectors.toList());

        assertThat(metadata.get(OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_METHODS))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsAll(authMethods).doesNotContain(AuthenticationMethod.NONE.getValue());
    }

    @Test
    public void revocationAuthSignMetadataIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // revocation endpoint auth sign is set
        assertThat(metadata.get(OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_SIGNIN_ALG)).isNotNull()
                .isInstanceOf(List.class);

        // since we expect secret_jwt and private_key to be supported this should be set
        // to contain at minimum base algs
        List<String> algs = Stream.of(
                JWSAlgorithm.HS256, JWSAlgorithm.HS384, JWSAlgorithm.HS512,
                JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512)
                .map(a -> a.getName()).collect(Collectors.toList());

        assertThat(metadata.get(OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_SIGNIN_ALG))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsAll(algs).doesNotContain(JWSAlgorithm.NONE.getName());
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenExactHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.ACCESS_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenWrongHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.REFRESH_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenInvalidHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, "invalid-token-type");

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenNoHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenExactHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.REFRESH_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenWrongHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.ACCESS_TOKEN);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenInvalidHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, "invalid-token-type");

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    public void clientAccessTokenNoHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getClientAccessTokenViaClientCredentialsWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenPlusAccessTokenWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // fetch an access token
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);

        // introspect result for access should be active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);

        // use basic auth for client auth
        req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result for refresh should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);

        // introspect result for access should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenAndRefreshTokenWithBasicAuthTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // follow forward to fetch response
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);
        res = mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();

        // code
        assertThat(queryParams.get(OAuth2ParameterNames.CODE)).isNotNull().isNotEmpty();
        String code = queryParams.get(OAuth2ParameterNames.CODE).get(0);
        assertThat(code).isNotBlank();

        // make a token request
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        params.add(OAuth2ParameterNames.CODE, code);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request for refresh
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);

        // use basic auth for client auth
        req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result for refresh should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);

        // introspect result for access should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenPlusRefreshTokenWithBasicAuthTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // follow forward to fetch response
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);
        res = mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();

        // code
        assertThat(queryParams.get(OAuth2ParameterNames.CODE)).isNotNull().isNotEmpty();
        String code = queryParams.get(OAuth2ParameterNames.CODE).get(0);
        assertThat(code).isNotBlank();

        // make a token request
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        params.add(OAuth2ParameterNames.CODE, code);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // fetch another access token
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken2 = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);

        // introspect result for access should be active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken2, null, clientId, clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

        // use basic auth for client auth
        req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result for access should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);

        // introspect result for refresh should be active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, refreshToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(true);

        // introspect result for access 2 should be active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken2, null, clientId, clientSecret);
        assertThat(active).isEqualTo(true);
    }

    @Test
    public void invalidUserAccessTokenNoHintWithBasicAuthTest() throws Exception {
        String accessToken = "invalid-access-token";

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

        // use basic auth for client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithFormAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithSecretJwtTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build assertion
        SignedJWT assertion = ClientCredentialsGrantJwtAssertionAuthTest.buildClientAssertion(applicationURL, clientId,
                signer, JWSAlgorithm.HS256, clientId);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE,
                ClientCredentialsGrantJwtAssertionAuthTest.JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithPrivateKeyJwtTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect result should be active
        Boolean active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId,
                clientSecret);
        assertThat(active).isEqualTo(true);

        // parse keys
        JWK jwk = ClientCredentialsGrantJwtAssertionAuthTest.loadKeys(clientJwks);

        // build signer for RSA
        RSASSASigner signer = new RSASSASigner(jwk.toRSAKey());

        // build assertion
        SignedJWT assertion = ClientCredentialsGrantJwtAssertionAuthTest.buildClientAssertion(applicationURL, clientId,
                signer, JWSAlgorithm.RS256, jwk.getKeyID());

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE,
                ClientCredentialsGrantJwtAssertionAuthTest.JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a blank response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // introspect result should be not active
        active = OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, null, clientId, clientSecret);
        assertThat(active).isEqualTo(false);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenWithNoAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

        // no client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isForbidden())
                .andReturn();

        // expect no response
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenWithWrongClientBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // revoke request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

        // use client 2 auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(REVOCATION_URL)
                .with(httpBasic(client2Id, client2Secret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect an error in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // error
        assertThat(response.get(OAuth2ParameterNames.ERROR)).isNotNull();
        assertThat(response.get(OAuth2ParameterNames.ERROR)).isEqualTo("unauthorized_client");
    }

    /*
     * endpoints
     */
    public final static String METADATA_URL = "/.well-known/oauth-authorization-server";
    private final static String REVOCATION_URL = TokenRevocationEndpoint.TOKEN_REVOCATION_URL;
    private final static String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private final static String AUTHORIZE_URL = AuthorizationEndpoint.AUTHORIZATION_URL;
    private final static String AUTHORIZED_URL = AuthorizationEndpoint.AUTHORIZED_URL;

    /*
     * claims
     */
    public final static Set<String> REQUIRED_METADATA;
    public final static String OAUTH2_METADATA_ISSUER = "issuer";
    public final static String OAUTH2_METADATA_REVOCATION_ENDPOINT = "revocation_endpoint";
    public final static String OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_METHODS = "revocation_endpoint_auth_methods_supported";
    public final static String OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_SIGNIN_ALG = "revocation_endpoint_auth_signing_alg_values_supported";

    static {
        REQUIRED_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(
                List.of(OAUTH2_METADATA_REVOCATION_ENDPOINT,
                        OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_METHODS,
                        OAUTH2_METADATA_REVOCATION_ENDPOINT_AUTH_SIGNIN_ALG)));

    }
    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
}
