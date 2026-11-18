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

package it.smartcommunitylab.aac.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import java.io.Serializable;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * Helpers for token tests: run the authorization code flow up to the code
 * (user authenticated via mock security context) and post token requests
 * with client basic auth, returning the parsed JSON response.
 */
public class UserTokenTestUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    public static final String ACCESS_TOKEN_TABLE = "oauth_access_token";
    public static final String REFRESH_TOKEN_TABLE = "oauth_refresh_token";

    private UserTokenTestUtils() {}

    /*
     * Access tokens of a user: the store keeps them by username, the subject is read from the token
     */
    public static List<AACOAuth2AccessToken> userTokens(ExtTokenStore store, String username, String subjectId) {
        return store
            .findTokensByUserName(username)
            .stream()
            .filter(t -> t instanceof AACOAuth2AccessToken)
            .map(t -> (AACOAuth2AccessToken) t)
            .filter(t -> subjectId.equals(t.getSubject()))
            .collect(Collectors.toList());
    }

    /*
     * Write a token row as a damaged store could hold it: bound to the
     * username, with a content that can not be read back
     */
    public static void insertRawToken(DataSource dataSource, String table, String tokenId, String username) {
        byte[] content = new byte[] { 1, 2, 3 };
        new JdbcTemplate(dataSource)
            .update(
                "insert into " + table + " (token_id, token, user_name, authentication) values (?, ?, ?, ?)",
                new Object[] { tokenId, new SqlLobValue(content), username, new SqlLobValue(content) },
                new int[] { Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.BLOB }
            );
    }

    public static int countTokens(DataSource dataSource, String table, String username) {
        String sql = "select count(*) from " + table + " where user_name = ?";
        return new JdbcTemplate(dataSource).queryForObject(sql, Integer.class, username);
    }

    public static void deleteTokens(DataSource dataSource, String table, String username) {
        new JdbcTemplate(dataSource).update("delete from " + table + " where user_name = ?", username);
    }

    /*
     * Authorize with the current (mock) user and return the issued code
     */
    public static String getAuthorizationCode(MockMvc mockMvc, String clientId, String scope) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, "code");
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, scope);

        MvcResult res = mockMvc
            .perform(MockMvcRequestBuilders.get(AuthorizationEndpoint.AUTHORIZATION_URL).params(params))
            .andExpect(status().isOk())
            .andReturn();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(forwardedUrl).isNotNull().startsWith(AuthorizationEndpoint.AUTHORIZED_URL);
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();

        res = mockMvc
            .perform(MockMvcRequestBuilders.get(forwardedUrl).session(session))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        MultiValueMap<String, String> query = UriComponentsBuilder
            .fromUriString(res.getResponse().getRedirectedUrl())
            .build(true)
            .getQueryParams();
        String code = query.getFirst(OAuth2ParameterNames.CODE);
        assertThat(code).isNotBlank();
        return code;
    }

    public static void assertInvalidGrant(Map<String, Serializable> response) {
        assertThat(response.get(OAuth2ParameterNames.ERROR)).isEqualTo("invalid_grant");
    }

    public static Map<String, Serializable> exchangeCode(
        MockMvc mockMvc,
        String clientId,
        String clientSecret,
        String code,
        int expectedStatus
    ) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        params.add(OAuth2ParameterNames.CODE, code);
        return token(mockMvc, clientId, clientSecret, params, expectedStatus);
    }

    public static Map<String, Serializable> refresh(
        MockMvc mockMvc,
        String clientId,
        String clientSecret,
        String refreshToken,
        int expectedStatus
    ) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        params.add(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);
        return token(mockMvc, clientId, clientSecret, params, expectedStatus);
    }

    private static Map<String, Serializable> token(
        MockMvc mockMvc,
        String clientId,
        String clientSecret,
        MultiValueMap<String, String> params,
        int expectedStatus
    ) throws Exception {
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
            .post(TokenEndpoint.TOKEN_URL)
            .with(httpBasic(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(params);

        MvcResult res = mockMvc.perform(req).andExpect(status().is(expectedStatus)).andReturn();
        return mapper.readValue(res.getResponse().getContentAsString(), typeRef);
    }
}
