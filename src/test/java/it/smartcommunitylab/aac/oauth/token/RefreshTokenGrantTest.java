package it.smartcommunitylab.aac.oauth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.smartcommunitylab.aac.oauth.endpoint.ErrorEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;

/*
 * OAuth 2.0 Refresh Token Grant
 * as per RFC6749
 * 
 * https://www.rfc-editor.org/rfc/rfc6749#section-6
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RefreshTokenGrantTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private String client2Id;
    private String client2Secret;

    @BeforeEach
    public void setUp() {
        if (config == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (clientId == null || clientSecret == null) {
            RealmConfig rc = config.getRealms().iterator().next();
            if (rc == null || rc.getClientApps() == null) {
                throw new IllegalArgumentException("missing config");
            }

            Iterator<ClientApp> iter = rc.getClientApps().iterator();
            ClientApp client = iter.next();
            clientId = client.getClientId();
            clientSecret = (String) client.getConfiguration().get("clientSecret");

            ClientApp client2 = iter.next();
            if (client2 != null) {
                client2Id = client2.getClientId();
                client2Secret = (String) client2.getConfiguration().get("clientSecret");
            }
        }

        if (clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (username == null || password == null) {
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

            username = ((InternalUserAccount) account).getUsername();
            password = ((InternalUserPassword) cred).getPassword();
        }

        if (username == null || password == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

//    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authCodeWithUserAuthAndHttpBasicRefreshTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // set offline scope to require refresh token
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

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

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.SCOPE)).isEqualTo(Config.SCOPE_OFFLINE_ACCESS);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);
        assertThat(refreshToken).isNotBlank();

        // try to fetch a new access token
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotBlank();

        // note: access token can be the same as before

        // scopes are set and match original request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.SCOPE)).isEqualTo(Config.SCOPE_OFFLINE_ACCESS);

        // refresh token is optional
        // when present it could match the previous one
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).satisfiesAnyOf(
                token -> assertThat(token).isNull(),
                token -> assertThat(token).isNotNull().isInstanceOf(String.class));
    }

//    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authCodeWithScopeAndUserAuthAndHttpBasicRefreshTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // set offline scope to require refresh token
        // set profile to access user profile
        List<String> scopes = List.of(Config.SCOPE_OFFLINE_ACCESS, Config.SCOPE_PROFILE);
        params.add(OAuth2ParameterNames.SCOPE, String.join(" ", scopes));

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

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

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        String[] scope = StringUtils.delimitedListToStringArray((String) response.get(OAuth2ParameterNames.SCOPE), " ");
        assertThat(scope).containsAll(scopes);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);
        assertThat(refreshToken).isNotBlank();

        // try to fetch a new access token
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotBlank();

        // note: access token can be the same as before

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        scope = StringUtils.delimitedListToStringArray((String) response.get(OAuth2ParameterNames.SCOPE), " ");
        assertThat(scope).containsAll(scopes);

        // refresh token is optional
        // when present it could match the previous one
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).satisfiesAnyOf(
                token -> assertThat(token).isNull(),
                token -> assertThat(token).isNotNull().isInstanceOf(String.class));
    }

//    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authCodeWithScopeNarrowAndUserAuthAndHttpBasicRefreshTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // set offline scope to require refresh token
        // set profile to access user profile
        List<String> scopes = List.of(Config.SCOPE_OFFLINE_ACCESS, Config.SCOPE_PROFILE);
        params.add(OAuth2ParameterNames.SCOPE, String.join(" ", scopes));

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

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

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        String[] scope = StringUtils.delimitedListToStringArray((String) response.get(OAuth2ParameterNames.SCOPE), " ");
        assertThat(scope).containsAll(scopes);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);
        assertThat(refreshToken).isNotBlank();

        // try to fetch a new access token
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);
        // narrow scope from original request
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_PROFILE);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotBlank();

        // note: access token can NOT be the same as before
        assertThat((String) response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotEqualTo(accessToken);

        // scopes are set and match narrowed request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.SCOPE)).isEqualTo(Config.SCOPE_PROFILE);

        // refresh token is optional
        // when present it CAN NOT match the previous one
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).satisfiesAnyOf(
                token -> assertThat(token).isNull(),
                token -> assertThat(token).isNotNull().isInstanceOf(String.class).isNotEqualTo(refreshToken));
    }

    // TODO refresh token rotation test

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authCodeWithUserAuthAndNoClientAuthRefreshTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // set offline scope to require refresh token
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

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

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.SCOPE)).isEqualTo(Config.SCOPE_OFFLINE_ACCESS);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);
        assertThat(refreshToken).isNotBlank();

        // try to fetch a new access token
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);

        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");

        // no access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNull();
    }

//    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authCodeWithUserAuthAndWrongClientAuthRefreshTest() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // set offline scope to require refresh token
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

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

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        assertThat((String) response.get(OAuth2ParameterNames.SCOPE)).isEqualTo(Config.SCOPE_OFFLINE_ACCESS);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);
        assertThat(refreshToken).isNotBlank();

        // try to fetch a new access token
        params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);

        // use a different client as before
        req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(client2Id, client2Secret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isBadRequest())
                .andReturn();

        // expect a 400 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("invalid_grant");

        // no access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNull();
    }

    private final static String AUTHORIZE_URL = AuthorizationEndpoint.AUTHORIZATION_URL;
    private final static String AUTHORIZED_URL = AuthorizationEndpoint.AUTHORIZED_URL;
    private final static String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private final static String ERROR_URL = ErrorEndpoint.ERROR_URL;

    private final TypeReference<Map<String, Serializable>> typeRef = new TypeReference<Map<String, Serializable>>() {
    };

}
