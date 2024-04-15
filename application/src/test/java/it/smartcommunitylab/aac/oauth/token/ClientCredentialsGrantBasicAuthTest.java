/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2ConfigUtils;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import java.io.Serializable;
import java.util.Map;
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

/*
 * OAuth 2.0 Client Credentials
 * as per RFC6749
 *
 * https://www.rfc-editor.org/rfc/rfc6749#section-4.4
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientCredentialsGrantBasicAuthTest {

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
        if (clientId == null || clientSecret == null) {
            ClientRegistration client = OAuth2ConfigUtils.with(config).client();
            assertThat(client).isNotNull();

            clientId = client.getClientId();
            clientSecret = client.getClientSecret();
        }

        if (clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    public void basicAuthWithSecretTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

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
    public void basicAuthWithSecretAndScopesTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_CLIENT_ROLE);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

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
    public void basicAuthTestWithSecretAndClientIdTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        // also set same clientId in form
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

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
    }

    @Test
    public void basicAuthTestWithSecretAndWrongClientIdTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        // set a different clientId in form
        params.add(OAuth2ParameterNames.CLIENT_ID, "clientId");
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isBadRequest()).andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("invalid_request");
    }

    @Test
    public void basicAuthTestWithoutSecretTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
            .with(httpBasic(clientId, ""))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isUnauthorized()).andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void basicAuthTestWrongSecretTest() throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
            .with(httpBasic(clientId, "secret"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isUnauthorized()).andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    private static final String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private final TypeReference<Map<String, Serializable>> typeRef = new TypeReference<Map<String, Serializable>>() {};
}
