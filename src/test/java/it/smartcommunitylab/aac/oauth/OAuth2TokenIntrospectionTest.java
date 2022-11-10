package it.smartcommunitylab.aac.oauth;

import org.assertj.core.api.InstanceOfAssertFactories;
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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2TestConfig.UserRegistration;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.token.ClientCredentialsGrantJwtAssertionAuthTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * OAuth 2.0 Token Introspection
 * as per 
 * https://www.rfc-editor.org/rfc/rfc7662
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OAuth2TokenIntrospectionTest {

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
    public void introspectEndpointMetadataIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // introspect endpoint is set
        assertThat(metadata.get(OAUTH2_METADATA_INTROSPECTION_ENDPOINT)).isNotNull().isInstanceOf(String.class);

        // issuer is set
        assertThat(metadata.get(OAUTH2_METADATA_ISSUER)).isNotNull().isInstanceOf(String.class);
        String issuer = (String) metadata.get(OAUTH2_METADATA_ISSUER);

        // should match definition (issuer + path)
        assertThat(metadata.get(OAUTH2_METADATA_INTROSPECTION_ENDPOINT)).isEqualTo(issuer + INTROSPECTION_URL);
    }

    @Test
    public void introspectAuthMethodMetadataIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // introspect endpoint auth method is set
        assertThat(metadata.get(OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_METHODS)).isNotNull()
                .isInstanceOf(List.class);

        // it contains expected but not NONE
        List<String> authMethods = Stream.of(
                AuthenticationMethod.CLIENT_SECRET_BASIC,
                AuthenticationMethod.CLIENT_SECRET_POST,
                AuthenticationMethod.CLIENT_SECRET_JWT,
                AuthenticationMethod.PRIVATE_KEY_JWT)
                .map(a -> a.getValue()).collect(Collectors.toList());

        assertThat(metadata.get(OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_METHODS))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsAll(authMethods).doesNotContain(AuthenticationMethod.NONE.getValue());
    }

    @Test
    public void introspectAuthSignMetadataIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // introspect endpoint auth sign is set
        assertThat(metadata.get(OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_SIGNIN_ALG)).isNotNull()
                .isInstanceOf(List.class);

        // since we expect secret_jwt and private_key to be supported this should be set
        // to contain at minimum base algs
        List<String> algs = Stream.of(
                JWSAlgorithm.HS256, JWSAlgorithm.HS384, JWSAlgorithm.HS512,
                JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512)
                .map(a -> a.getName()).collect(Collectors.toList());

        assertThat(metadata.get(OAUTH2_METADATA_INTROSPECTION_ENDPOINT_AUTH_SIGNIN_ALG))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsAll(algs).doesNotContain(JWSAlgorithm.NONE.getName());
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);

        // scope is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isBlank());

        // clientId is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.CLIENT_ID)).satisfiesAnyOf(
                id -> assertThat(id).isNull(),
                id -> assertThat(id).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId));

        // username is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.USERNAME)).satisfiesAnyOf(
                u -> assertThat(u).isNull(),
                u -> assertThat(u).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(username));

        // tokenType is optional, if provided it should match 'bearer'
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.TOKEN_TYPE)).satisfiesAnyOf(
                t -> assertThat(t).isNull(),
                t -> assertThat(t).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo("bearer"));

        // exp is optional, if provided validate
        long now = Instant.now().getEpochSecond();
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.EXP)).satisfiesAnyOf(
                e -> assertThat(e).isNull(),
                e -> assertThat(e).isNotNull().asInstanceOf(InstanceOfAssertFactories.INTEGER)
                        .isGreaterThan((int) now),
                e -> assertThat(e).isNotNull().asInstanceOf(InstanceOfAssertFactories.LONG)
                        .isGreaterThan(now));

        // iat is optional, if provided validate
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.IAT)).satisfiesAnyOf(
                i -> assertThat(i).isNull(),
                i -> assertThat(i).isNotNull().asInstanceOf(InstanceOfAssertFactories.INTEGER)
                        .isLessThanOrEqualTo((int) now),
                i -> assertThat(i).isNotNull().asInstanceOf(InstanceOfAssertFactories.LONG)
                        .isLessThanOrEqualTo(now));

        // nbf is optional, if provided validate
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.NBF)).satisfiesAnyOf(
                n -> assertThat(n).isNull(),
                n -> assertThat(n).isNotNull().isInstanceOf(Number.class));

        // sub is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.SUB)).satisfiesAnyOf(
                s -> assertThat(s).isNull(),
                s -> assertThat(s).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(userId));

        // aud is optional, if provided client should be in it
        // NOTE: this is not accurate, but we expect AAC to include clientId in audience
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.AUD)).satisfiesAnyOf(
                a -> assertThat(a).isNull(),
                a -> assertThat(a).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId),
                a -> assertThat(a).isNotNull().isInstanceOf(List.class)
                        .asInstanceOf(InstanceOfAssertFactories.LIST).contains(clientId)

        );

        // iss is optional
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ISS)).satisfiesAnyOf(
                i -> assertThat(i).isNull(),
                i -> assertThat(i).isNotNull().isInstanceOf(String.class));

        // jti is optional
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.JTI)).satisfiesAnyOf(
                j -> assertThat(j).isNull(),
                j -> assertThat(j).isNotNull().isInstanceOf(String.class));
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenExactHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenWrongHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.REFRESH_TOKEN);

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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenInvalidHintWithBasicAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, "invalid-token-type");

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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenNoHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);

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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);

        // scope is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(Config.SCOPE_OFFLINE_ACCESS));

        // clientId is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.CLIENT_ID)).satisfiesAnyOf(
                id -> assertThat(id).isNull(),
                id -> assertThat(id).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId));

        // username is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.USERNAME)).satisfiesAnyOf(
                u -> assertThat(u).isNull(),
                u -> assertThat(u).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(username));

        // tokenType is optional, if provided it should match 'bearer'
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.TOKEN_TYPE)).satisfiesAnyOf(
                t -> assertThat(t).isNull(),
                t -> assertThat(t).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo("bearer"));

        // exp is optional, if provided validate
        long now = Instant.now().getEpochSecond();
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.EXP)).satisfiesAnyOf(
                e -> assertThat(e).isNull(),
                e -> assertThat(e).isNotNull().asInstanceOf(InstanceOfAssertFactories.INTEGER)
                        .isGreaterThan((int) now),
                e -> assertThat(e).isNotNull().asInstanceOf(InstanceOfAssertFactories.LONG)
                        .isGreaterThan(now));

        // iat is optional, if provided validate
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.IAT)).satisfiesAnyOf(
                i -> assertThat(i).isNull(),
                i -> assertThat(i).isNotNull().asInstanceOf(InstanceOfAssertFactories.INTEGER)
                        .isLessThanOrEqualTo((int) now),
                i -> assertThat(i).isNotNull().asInstanceOf(InstanceOfAssertFactories.LONG)
                        .isLessThanOrEqualTo(now));

        // nbf is optional, if provided validate
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.NBF)).satisfiesAnyOf(
                n -> assertThat(n).isNull(),
                n -> assertThat(n).isNotNull().isInstanceOf(Number.class));

        // sub is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.SUB)).satisfiesAnyOf(
                s -> assertThat(s).isNull(),
                s -> assertThat(s).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(userId));

        // aud is optional, if provided client should be in it
        // NOTE: this is not accurate, but we expect AAC to include clientId in audience
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.AUD)).satisfiesAnyOf(
                a -> assertThat(a).isNull(),
                a -> assertThat(a).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId),
                a -> assertThat(a).isNotNull().isInstanceOf(List.class)
                        .asInstanceOf(InstanceOfAssertFactories.LIST).contains(clientId)

        );

        // iss is optional
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ISS)).satisfiesAnyOf(
                i -> assertThat(i).isNull(),
                i -> assertThat(i).isNotNull().isInstanceOf(String.class));

        // jti is optional
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.JTI)).satisfiesAnyOf(
                j -> assertThat(j).isNull(),
                j -> assertThat(j).isNotNull().isInstanceOf(String.class));
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenExactHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.REFRESH_TOKEN);

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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userRefreshTokenWrongHintWithBasicAuthTest() throws Exception {
        // fetch a valid user refresh token
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, refreshToken);
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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
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
    public void userAccessTokenNoHintWithFormAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithSecretJwtTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build assertion
        SignedJWT assertion = ClientCredentialsGrantJwtAssertionAuthTest.buildClientAssertion(applicationURL, clientId,
                signer, JWSAlgorithm.HS256, clientId);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE,
                ClientCredentialsGrantJwtAssertionAuthTest.JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenNoHintWithPrivateKeyJwtTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // parse keys
        JWK jwk = ClientCredentialsGrantJwtAssertionAuthTest.loadKeys(clientJwks);

        // build signer for RSA
        RSASSASigner signer = new RSASSASigner(jwk.toRSAKey());

        // build assertion
        SignedJWT assertion = ClientCredentialsGrantJwtAssertionAuthTest.buildClientAssertion(applicationURL, clientId,
                signer, JWSAlgorithm.RS256, jwk.getKeyID());

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE,
                ClientCredentialsGrantJwtAssertionAuthTest.JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void clientAccessTokenNoHintWithBasicAuthTest() throws Exception {
        // fetch a valid client access token
        String accessToken = OAuth2TestUtils.getClientAccessTokenViaClientCredentialsWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);

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
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isEqualTo(true);

        // scope is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isBlank());

        // clientId is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.CLIENT_ID)).satisfiesAnyOf(
                id -> assertThat(id).isNull(),
                id -> assertThat(id).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId));

        // username is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.USERNAME)).satisfiesAnyOf(
                u -> assertThat(u).isNull(),
                u -> assertThat(u).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId));

        // tokenType is optional, if provided it should match 'bearer'
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.TOKEN_TYPE)).satisfiesAnyOf(
                t -> assertThat(t).isNull(),
                t -> assertThat(t).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo("bearer"));

        // exp is optional, if provided validate
        long now = Instant.now().getEpochSecond();
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.EXP)).satisfiesAnyOf(
                e -> assertThat(e).isNull(),
                e -> assertThat(e).isNotNull().asInstanceOf(InstanceOfAssertFactories.INTEGER)
                        .isGreaterThan((int) now),
                e -> assertThat(e).isNotNull().asInstanceOf(InstanceOfAssertFactories.LONG)
                        .isGreaterThan(now));

        // iat is optional, if provided validate
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.IAT)).satisfiesAnyOf(
                i -> assertThat(i).isNull(),
                i -> assertThat(i).isNotNull().asInstanceOf(InstanceOfAssertFactories.INTEGER)
                        .isLessThanOrEqualTo((int) now),
                i -> assertThat(i).isNotNull().asInstanceOf(InstanceOfAssertFactories.LONG)
                        .isLessThanOrEqualTo(now));

        // nbf is optional, if provided validate
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.NBF)).satisfiesAnyOf(
                n -> assertThat(n).isNull(),
                n -> assertThat(n).isNotNull().isInstanceOf(Number.class));

        // sub is optional, if provided it should match request
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.SUB)).satisfiesAnyOf(
                s -> assertThat(s).isNull(),
                s -> assertThat(s).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId));

        // aud is optional, if provided client should be in it
        // NOTE: this is not accurate, but we expect AAC to include clientId in audience
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.AUD)).satisfiesAnyOf(
                a -> assertThat(a).isNull(),
                a -> assertThat(a).isNotNull().isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(clientId),
                a -> assertThat(a).isNotNull().isInstanceOf(List.class)
                        .asInstanceOf(InstanceOfAssertFactories.LIST).contains(clientId));

        // iss is optional
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ISS)).satisfiesAnyOf(
                i -> assertThat(i).isNull(),
                i -> assertThat(i).isNotNull().isInstanceOf(String.class));

        // jti is optional
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.JTI)).satisfiesAnyOf(
                j -> assertThat(j).isNull(),
                j -> assertThat(j).isNotNull().isInstanceOf(String.class));
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void invalidAccessTokenNoHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-access-token");

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
    public void invalidAccessTokenExactHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-access-token");
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

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void invalidAccessTokenWrongHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-access-token");
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.REFRESH_TOKEN);

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
    public void invalidRefreshTokenNoHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-refresh-token");

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
    public void invalidRefreshTokenExactHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-refresh-token");
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.REFRESH_TOKEN);

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
    public void invalidRefreshTokenWrongHintWithBasicAuthTest() throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, "invalid-access-token");
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

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAccessTokenWithNoAuthTest() throws Exception {
        // fetch a valid user access token
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId,
                clientSecret);

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.ACCESS_TOKEN);

        // no client auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
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

        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, accessToken);
        params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, OAuth2ParameterNames.ACCESS_TOKEN);

        // use client 2 auth
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(INTROSPECTION_URL)
                .with(httpBasic(client2Id, client2Secret))
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
        // token should be not active because this client is not an audience
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
