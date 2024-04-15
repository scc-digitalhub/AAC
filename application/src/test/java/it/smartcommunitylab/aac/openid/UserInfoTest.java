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

package it.smartcommunitylab.aac.openid;

import static it.smartcommunitylab.aac.auth.BearerTokenRequestPostProcessor.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.auth.WithMockBearerTokenAuthentication;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2ConfigUtils;
import it.smartcommunitylab.aac.oauth.OAuth2TestConfig.UserRegistration;
import it.smartcommunitylab.aac.oauth.OAuth2TestUtils;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.openid.endpoint.UserInfoEndpoint;
import it.smartcommunitylab.aac.openid.scope.OpenIdScope;
import it.smartcommunitylab.aac.profiles.scope.OpenIdDefaultScope;
import it.smartcommunitylab.aac.profiles.scope.OpenIdEmailScope;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/*
 * UserInfo endpoint test
 * OpenID Connect Core 1.0
 *
 * ref
 * https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserInfoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

    private UserRegistration user;
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

        if (user == null) {
            user = OAuth2ConfigUtils.with(config).user();
            assertThat(user).isNotNull();
        }

        if (user == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    public void endpointIsProtected() throws Exception {
        MvcResult res = this.mockMvc.perform(get(USERINFO_URL)).andExpect(status().isUnauthorized()).andReturn();

        // expect a 401 with no body
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void getInfoForOpenIdUserAccessTokenTest() throws Exception {
        String[] scopes = { OpenIdScope.SCOPE };

        // fetch a valid user access token with scopes
        // this is required because userinfo loads the token...
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(
            mockMvc,
            String.join(" ", scopes),
            clientId,
            clientSecret
        );

        // userinfo request
        // use bearer token context
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(USERINFO_URL)
            .with(bearer(accessToken).subject(user.getUserId()).scopes(scopes));

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // validate userinfo with core fields

        // sub is required, should match user
        assertThat(response.get(StandardClaimNames.SUB))
            .isNotNull()
            .isInstanceOf(String.class)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .isEqualTo(user.getUserId());

        // email should NOT be exposed
        assertThat(response.get(StandardClaimNames.EMAIL)).isNull();

        // username should NOT be exposed
        assertThat(response.get(StandardClaimNames.PREFERRED_USERNAME)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void postInfoForOpenIdUserAccessTokenTest() throws Exception {
        String[] scopes = { OpenIdScope.SCOPE };

        // fetch a valid user access token with scopes
        // this is required because userinfo loads the token...
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(
            mockMvc,
            String.join(" ", scopes),
            clientId,
            clientSecret
        );

        // userinfo request
        // use bearer token context
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .post(USERINFO_URL)
            .with(bearer(accessToken).subject(user.getUserId()).scopes(scopes));

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // validate userinfo with core fields

        // sub is required, should match user
        assertThat(response.get(StandardClaimNames.SUB))
            .isNotNull()
            .isInstanceOf(String.class)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .isEqualTo(user.getUserId());

        // email should NOT be exposed
        assertThat(response.get(StandardClaimNames.EMAIL)).isNull();

        // username should NOT be exposed
        assertThat(response.get(StandardClaimNames.PREFERRED_USERNAME)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void getInfoForOpenProfileIdUserAccessTokenTest() throws Exception {
        String[] scopes = { OpenIdScope.SCOPE, OpenIdDefaultScope.SCOPE };

        // fetch a valid user access token with scopes
        // this is required because userinfo loads the token...
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(
            mockMvc,
            String.join(" ", scopes),
            clientId,
            clientSecret
        );

        // userinfo request
        // use bearer token context
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(USERINFO_URL)
            .with(bearer(accessToken).subject(user.getUserId()).scopes(scopes));

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // validate userinfo with core fields

        // sub is required, should match user
        assertThat(response.get(StandardClaimNames.SUB))
            .isNotNull()
            .isInstanceOf(String.class)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .isEqualTo(user.getUserId());

        // username is required, should match user
        assertThat(response.get(StandardClaimNames.PREFERRED_USERNAME))
            .isNotNull()
            .isInstanceOf(String.class)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .isEqualTo(user.getUsername());

        // name is optional, if provided it should match request
        assertThat(response.get(StandardClaimNames.NAME))
            .satisfiesAnyOf(
                s -> assertThat(s).isNull(),
                s ->
                    assertThat(s)
                        .isNotNull()
                        .isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING)
                        .isEqualTo(user.getName())
            );

        // family name is optional, if provided it should match request
        assertThat(response.get(StandardClaimNames.FAMILY_NAME))
            .satisfiesAnyOf(
                s -> assertThat(s).isNull(),
                s ->
                    assertThat(s)
                        .isNotNull()
                        .isInstanceOf(String.class)
                        .asInstanceOf(InstanceOfAssertFactories.STRING)
                        .isEqualTo(user.getSurname())
            );

        // email should NOT be exposed
        assertThat(response.get(StandardClaimNames.EMAIL)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void getInfoForOpenEmailIdUserAccessTokenTest() throws Exception {
        String[] scopes = { OpenIdScope.SCOPE, OpenIdEmailScope.SCOPE };

        // fetch a valid user access token with scopes
        // this is required because userinfo loads the token...
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(
            mockMvc,
            String.join(" ", scopes),
            clientId,
            clientSecret
        );

        // userinfo request
        // use bearer token context
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(USERINFO_URL)
            .with(bearer(accessToken).subject(user.getUserId()).scopes(scopes));

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // validate userinfo with core fields

        // sub is required, should match user
        assertThat(response.get(StandardClaimNames.SUB))
            .isNotNull()
            .isInstanceOf(String.class)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .isEqualTo(user.getUserId());

        // email is required, should match user
        assertThat(response.get(StandardClaimNames.EMAIL))
            .isNotNull()
            .isInstanceOf(String.class)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .isEqualTo(user.getEmail());

        // email verified is optional
        assertThat(response.get(StandardClaimNames.EMAIL_VERIFIED))
            .satisfiesAnyOf(
                s -> assertThat(s).isNull(),
                s -> assertThat(s).asInstanceOf(InstanceOfAssertFactories.BOOLEAN).isNotNull()
            );

        // username should NOT be exposed
        assertThat(response.get(StandardClaimNames.PREFERRED_USERNAME)).isNull();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void getInfoForNoScopeUserAccessTokenTest() throws Exception {
        String[] scopes = {};

        // fetch a valid user access token with scopes
        // this is required because userinfo loads the token...
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(
            mockMvc,
            String.join(" ", scopes),
            clientId,
            clientSecret
        );

        // userinfo request
        // use bearer token context
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(USERINFO_URL)
            .with(bearer(accessToken).subject(user.getUserId()).scopes(scopes));

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isForbidden()).andReturn();

        // expect an error in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("insufficient_scope");
    }

    @Test
    public void getInfoForInvalidTokenUserAccessTokenTest() throws Exception {
        String[] scopes = {};

        // userinfo request
        // use bearer token context
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .get(USERINFO_URL)
            .with(bearer("token").subject(user.getUserId()).scopes(scopes));

        MvcResult res = this.mockMvc.perform(req).andExpect(status().isUnauthorized()).andReturn();

        // expect an error in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // error is in www-authenticate header
        String wwwAuth = res.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(wwwAuth).isNotBlank();
        assertThat(wwwAuth).contains("error=invalid_token");
    }

    /*
     * Config
     *
     */
    public static final String USERINFO_URL = UserInfoEndpoint.USERINFO_URL;

    private static final String USER_ID = "test-0000-12345-user";

    private final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};
}
