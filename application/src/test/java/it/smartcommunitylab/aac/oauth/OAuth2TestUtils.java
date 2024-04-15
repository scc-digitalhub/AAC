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

package it.smartcommunitylab.aac.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.oauth2.sdk.ResponseType;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

public class OAuth2TestUtils {

    /*
     * endpoint
     */
    public static final String METADATA_URL = "/.well-known/oauth-authorization-server";
    private static final String AUTHORIZE_URL = AuthorizationEndpoint.AUTHORIZATION_URL;
    private static final String AUTHORIZED_URL = AuthorizationEndpoint.AUTHORIZED_URL;
    private static final String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private static final String REVOCATION_URL = TokenRevocationEndpoint.TOKEN_REVOCATION_URL;
    private static final String INTROSPECTION_URL = TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL;

    /*
     * Token helpers
     */
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    public static String getUserAccessTokenViaAuthCodeWithBasicAuth(
        MockMvc mockMvc,
        String clientId,
        String clientSecret
    ) throws Exception {
        return getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, "", clientId, clientSecret);
    }

    public static String getUserAccessTokenViaAuthCodeWithBasicAuth(
        MockMvc mockMvc,
        String scope,
        String clientId,
        String clientSecret
    ) throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, scope);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(AUTHORIZE_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // follow forward to fetch response
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);
        res = mockMvc.perform(req).andExpect(status().is3xxRedirection()).andReturn();

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

        req =
            MockMvcRequestBuilders
                .post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);

        return accessToken;
    }

    public static String getUserRefreshTokenViaAuthCodeWithBasicAuth(
        MockMvc mockMvc,
        String clientId,
        String clientSecret
    ) throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OFFLINE_ACCESS);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(AUTHORIZE_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // follow forward to fetch response
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);
        res = mockMvc.perform(req).andExpect(status().is3xxRedirection()).andReturn();

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

        req =
            MockMvcRequestBuilders
                .post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // refresh token
        assertThat(response.get(OAuth2ParameterNames.REFRESH_TOKEN)).isNotNull().isInstanceOf(String.class);
        String refreshToken = (String) response.get(OAuth2ParameterNames.REFRESH_TOKEN);

        return refreshToken;
    }

    public static String getUserIdTokenViaAuthCodeWithBasicAuth(MockMvc mockMvc, String clientId, String clientSecret)
        throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OPENID);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(AUTHORIZE_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // follow forward to fetch response
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);
        res = mockMvc.perform(req).andExpect(status().is3xxRedirection()).andReturn();

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

        req =
            MockMvcRequestBuilders
                .post(TOKEN_URL)
                .with(httpBasic(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // id token
        assertThat(response.get(OidcParameterNames.ID_TOKEN)).isNotNull().isInstanceOf(String.class);
        String idToken = (String) response.get(OidcParameterNames.ID_TOKEN);

        return idToken;
    }

    public static String getClientAccessTokenViaClientCredentialsWithBasicAuth(
        MockMvc mockMvc,
        String clientId,
        String clientSecret
    ) throws Exception {
        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .post(TOKEN_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);

        return accessToken;
    }

    /*
     * Introspect
     */
    public static Boolean introspectTokenWithBasicAuth(
        MockMvc mockMvc,
        String token,
        String tokenTypeHint,
        String clientId,
        String clientSecret
    ) throws Exception {
        // introspect request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.TOKEN, token);
        if (StringUtils.hasText(tokenTypeHint)) {
            params.add(OAuth2ParameterNames.TOKEN_TYPE_HINT, tokenTypeHint);
        }

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .post(INTROSPECTION_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

        // active is REQUIRED
        assertThat(response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE)).isNotNull().isInstanceOf(Boolean.class);
        Boolean active = (Boolean) response.get(OAuth2TokenIntrospectionClaimNames.ACTIVE);

        return active;
    }
}
