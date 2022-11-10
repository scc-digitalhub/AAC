package it.smartcommunitylab.aac.oauth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.oauth2.sdk.ResponseType;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2ConfigUtils;
import it.smartcommunitylab.aac.oauth.OAuth2TestConfig.UserRegistration;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.ErrorEndpoint;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;

/*
 * OAuth 2.0 Implicit Grant
 * as per RFC6749
 * 
 * https://www.rfc-editor.org/rfc/rfc6749#section-4.2
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ImplicitGrantTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapConfig config;

    private String username;
    private String password;
    private String clientId;
    private String clientSecret;

    @BeforeEach
    public void setUp() {
        if (clientId == null || clientSecret == null) {
            ClientRegistration client = OAuth2ConfigUtils.with(config).client();
            assertThat(client).isNotNull();

            clientId = client.getClientId();
            clientSecret = client.getClientSecret();
        }

        if (clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (username == null || password == null) {
            UserRegistration user = OAuth2ConfigUtils.with(config).user();
            assertThat(user).isNotNull();

            username = user.getUsername();
            password = user.getPassword();
        }

        if (username == null || password == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthImplicitTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // set empty scopes to avoid fall back to predefined
        params.add(OAuth2ParameterNames.SCOPE, "");

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

        // expect a redirect in response with fragment
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isNotBlank();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.query(fragment);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.query(fragment);

        MultiValueMap<String, String> response = builder.build(true).getQueryParams();
        assertThat(response).isNotNull();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isNotEmpty();
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE).get(0)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isNotEmpty();
        String accessToken = response.get(OAuth2ParameterNames.ACCESS_TOKEN).get(0);
        assertThat(accessToken).isNotBlank();

        // scopes is null or empty
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).satisfiesAnyOf(
                scope -> assertThat(scope).isNull(),
                scope -> assertThat(scope).isEmpty(),
                scope -> assertThat(scope.get(0)).isEqualTo(""));

        // there is no refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthWithScopeImplicitTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_PROFILE);

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

        // expect a redirect in response with fragment
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isNotBlank();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.query(fragment);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.query(fragment);

        MultiValueMap<String, String> response = builder.build(true).getQueryParams();
        assertThat(response).isNotNull();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isNotEmpty();
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE).get(0)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isNotEmpty();
        String accessToken = response.get(OAuth2ParameterNames.ACCESS_TOKEN).get(0);
        assertThat(accessToken).isNotBlank();

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isNotEmpty();
        String scope = response.get(OAuth2ParameterNames.SCOPE).get(0);
        assertThat(scope).isEqualTo(Config.SCOPE_PROFILE);

        // there is no refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthWithValidStateImplicitTest() throws Exception {
        String state = "stateWithValidChars!@$().+,/-_";

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "");
        params.add(OAuth2ParameterNames.STATE, state);

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

        // expect a redirect in response with fragment
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isNotBlank();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.query(fragment);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.query(fragment);

        MultiValueMap<String, String> response = builder.build(true).getQueryParams();
        assertThat(response).isNotNull();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isNotEmpty();
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE).get(0)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isNotEmpty();
        String accessToken = response.get(OAuth2ParameterNames.ACCESS_TOKEN).get(0);
        assertThat(accessToken).isNotBlank();

        // state is set and matched request
        assertThat(response.get(OAuth2ParameterNames.STATE)).isNotNull().isNotEmpty();
        assertThat(response.get(OAuth2ParameterNames.STATE).get(0)).isEqualTo(state);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthWithOfflineScopeImplicitTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        // require offline scope for refresh token
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

        // expect a redirect in response with fragment
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isNotBlank();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.query(fragment);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.query(fragment);

        MultiValueMap<String, String> response = builder.build(true).getQueryParams();
        assertThat(response).isNotNull();

        // expect error
        assertThat(response.get("error")).isNotNull().isNotEmpty();
        assertThat(response.get("error").get(0)).isEqualTo(OAuth2Exception.INVALID_SCOPE);

        // no access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNull();
    }

    @Test
    public void noAuthImplicitTest() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_PROFILE);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response to login
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // no fragment params
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isBlank();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "two")
    public void userAuthWrongRealmImplicitTest() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_PROFILE);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response to login
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // no fragment params
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isBlank();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthNoClientImplicitTest() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response to error
        assertThat(res.getResponse().getContentAsString()).isBlank();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().isEqualTo(ERROR_URL);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthInvalidClientImplicitTest() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.SCOPE, "");
        params.add(OAuth2ParameterNames.CLIENT_ID, "client");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response to error
        assertThat(res.getResponse().getContentAsString()).isBlank();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().isEqualTo(ERROR_URL);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthWrongScopeImplicitTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "invalid-scope");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with fragment
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isNotBlank();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.query(fragment);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.query(fragment);

        MultiValueMap<String, String> response = builder.build(true).getQueryParams();
        assertThat(response).isNotNull();

        // error
        assertThat(response.get(OAuth2ParameterNames.ERROR)).isNotNull().isNotEmpty();
        assertThat(response.get(OAuth2ParameterNames.ERROR).get(0)).isEqualTo("invalid_scope");

        // there is no access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthWrongRedirectImplicitTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "");
        params.add(OAuth2ParameterNames.REDIRECT_URI, "http123");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response to error
        assertThat(res.getResponse().getContentAsString()).isBlank();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().isEqualTo(ERROR_URL);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void userAuthInvalidStateImplicitTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.TOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "");
        params.add(OAuth2ParameterNames.STATE, "stateWithInvalidChars#\\");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response to error
        assertThat(res.getResponse().getContentAsString()).isBlank();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().isEqualTo(ERROR_URL);
    }

    private final static String AUTHORIZE_URL = AuthorizationEndpoint.AUTHORIZATION_URL;
    private final static String AUTHORIZED_URL = AuthorizationEndpoint.AUTHORIZED_URL;
    private final static String ERROR_URL = ErrorEndpoint.ERROR_URL;

}
