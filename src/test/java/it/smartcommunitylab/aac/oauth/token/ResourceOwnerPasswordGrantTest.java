package it.smartcommunitylab.aac.oauth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.api.scopes.ApiUsersScope;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.core.base.AbstractAccount;
import it.smartcommunitylab.aac.core.base.AbstractUserCredentials;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;

/*
 * OAuth 2.0 Resource Owner Password Credentials Grant
 * as per RFC6749
 * 
 * https://www.rfc-editor.org/rfc/rfc6749#section-4.3
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ResourceOwnerPasswordGrantTest {

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

            ClientApp client = rc.getClientApps().iterator().next();
            clientId = client.getClientId();
            clientSecret = (String) client.getConfiguration().get("clientSecret");
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

    @Test
    public void clientAuthAndValidUserPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        // set empty scopes to avoid fall back to predefined
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
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

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // expire is set and valid
        assertThat(response.get(OAuth2ParameterNames.EXPIRES_IN)).isNotNull().isInstanceOf(Integer.class);
        Integer expiresIn = (Integer) response.get(OAuth2ParameterNames.EXPIRES_IN);
        assertTrue(expiresIn > 0);

        // scopes is null or empty
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isNotNull().isInstanceOf(String.class)
                        .isEqualTo(""),
                scope -> assertThat(scope).isNotNull().isInstanceOf(List.class)
                        .asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty());

        // there is no refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();
    }

    @Test
    public void clientAuthAndValidUserPasswordAndScopesTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_PROFILE);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
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

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        String scope = (String) response.get(OAuth2ParameterNames.SCOPE);
        assertThat(scope).isEqualTo(Config.SCOPE_PROFILE);

        // there is no refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();
    }

    @Test
    public void clientFormAndValidUserPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        // set empty scopes to avoid fall back to predefined
        params.add(OAuth2ParameterNames.SCOPE, "");
        // set client auth via form
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
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

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // expire is set and valid
        assertThat(response.get(OAuth2ParameterNames.EXPIRES_IN)).isNotNull().isInstanceOf(Integer.class);
        Integer expiresIn = (Integer) response.get(OAuth2ParameterNames.EXPIRES_IN);
        assertTrue(expiresIn > 0);

        // scopes is null or empty
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isNotNull().isInstanceOf(String.class)
                        .isEqualTo(""),
                scope -> assertThat(scope).isNotNull().isInstanceOf(List.class)
                        .asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty());

        // there is no refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();
    }

    @Test
    public void clientAuthAndInvalidUserTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, "user");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized_user");
    }

    @Test
    public void clientAuthAndInvalidUserAndPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, "user");
        params.add(OAuth2ParameterNames.PASSWORD, "password");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized_user");
    }

    @Test
    public void clientAuthAndUserWrongPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, "password");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized_user");
    }

    @Test
    public void clientAuthAndNoUserTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isBadRequest())
                .andReturn();

        // expect a 400 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("invalid_request");
    }

    @Test
    public void invalidClientAuthAndUserAndPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, "secret"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void noClientAuthAndUserAndPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void invalidClientAndUserAndPasswordTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic("client", "secret"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientAuthAndUserAndPasswordAndInvalidScopeTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        // require a client-only scope on a user grant
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_CLIENT_ROLE);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isBadRequest())
                .andReturn();

        // expect a 400 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("invalid_scope");
    }

    @Test
    public void clientAuthAndUserAndPasswordAndUnauthorizedScopeTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        // require a protected scope not available for user
        params.add(OAuth2ParameterNames.SCOPE, ApiUsersScope.SCOPE);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isBadRequest())
                .andReturn();

        // expect a 400 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("invalid_scope");
    }

    @Test
    public void clientAuthAndUserAndPasswordAndWrongScopeTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        params.add(OAuth2ParameterNames.SCOPE, "not-existing-scope");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isBadRequest())
                .andReturn();

        // expect a 400 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("invalid_scope");
    }

    @Test
    public void clientAuthAndUserAndPasswordAndOfflineScopeTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        params.add(OAuth2ParameterNames.USERNAME, username);
        params.add(OAuth2ParameterNames.PASSWORD, password);
        // require offline scope for a refresh token
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
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

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // scopes are either not set or offline is not present
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isNotNull().isInstanceOf(String.class)
                        .isNotEqualTo(Config.SCOPE_OFFLINE_ACCESS),
                scope -> assertThat(scope).isNotNull().isInstanceOf(List.class)
                        .asInstanceOf(InstanceOfAssertFactories.LIST).doesNotContain(Config.SCOPE_OFFLINE_ACCESS));

        // there is no refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();
    }

    private final static String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private final TypeReference<Map<String, Serializable>> typeRef = new TypeReference<Map<String, Serializable>>() {
    };
}
