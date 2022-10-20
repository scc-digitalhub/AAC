package it.smartcommunitylab.aac.oauth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
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
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;

/*
 * OAuth 2.0 Client Credentials
 * as per RFC6749
 * 
 * https://www.rfc-editor.org/rfc/rfc6749#section-4.4
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientCredentialsGrantFormPostTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

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
    }

    @Test
    public void formPostWithSecretTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
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
    }

    @Test
    public void formPostWithSecretAndScopesTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_CLIENT_ROLE);

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

        // scopes are set and match request
        assertThat(response.get(OAuth2ParameterNames.SCOPE)).isNotNull().isInstanceOf(String.class);
        String scope = (String) response.get(OAuth2ParameterNames.SCOPE);
        assertThat(scope).isEqualTo(Config.SCOPE_CLIENT_ROLE);
    }

    @Test
    public void formPostRefreshTokenTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
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

        // expect NO refresh token as per
        // https://www.rfc-editor.org/rfc/rfc6749#section-4.4.3
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNull();

        // scopes are either empty or string or set of strings
        assertThat(response.get(OAuth2ParameterNames.SCOPE))
                .satisfiesAnyOf(
                        scope -> assertThat(scope).isNull(),
                        scope -> assertThat(scope).isNotNull().isInstanceOfAny(String.class, List.class));

        // scope offline access is not included
        if (response.get(OAuth2ParameterNames.SCOPE) != null) {
            if (response.get(OAuth2ParameterNames.SCOPE) instanceof String) {
                String scope = (String) response.get(OAuth2ParameterNames.SCOPE);
                assertThat(scope).isNotEqualTo(Config.SCOPE_OFFLINE_ACCESS);
            }
            if (response.get(OAuth2ParameterNames.SCOPE) instanceof List) {
                assertThat((List<?>) response.get(OAuth2ParameterNames.SCOPE))
                        .allSatisfy(o -> assertThat(o).isInstanceOf(String.class));

                @SuppressWarnings("unchecked")
                List<String> scope = (List<String>) response.get(OAuth2ParameterNames.SCOPE);
                assertThat(scope).doesNotContain(Config.SCOPE_OFFLINE_ACCESS);

            }
        }
    }

    @Test
    public void formPostOfflineAccessTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
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
        assertThat(response.get("error")).isEqualTo(OAuth2Exception.INVALID_SCOPE);
    }

    @Test
    public void formPostNoSecretTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
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
    public void formPostWrongSecretTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.CLIENT_SECRET, "secret");

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
    public void formPostNoClientIdTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

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

    private final static String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private final TypeReference<Map<String, Serializable>> typeRef = new TypeReference<Map<String, Serializable>>() {
    };
}
