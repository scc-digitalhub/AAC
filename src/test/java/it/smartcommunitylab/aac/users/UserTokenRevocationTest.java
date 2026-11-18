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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2ConfigUtils;
import it.smartcommunitylab.aac.oauth.OAuth2TestUtils;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import java.io.Serializable;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/*
 * A deleted user must not obtain tokens anymore: its access and refresh
 * tokens are removed on delete, a refresh and the exchange of a code issued
 * before the delete are refused at the token endpoint.
 * Tokens are stored by username, so removal must match the subject as well.
 * Each test uses its own reserved user of realm "test" (del-token-* in
 * bootstrap-test.yaml), so the users shared with other tests are untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserTokenRevocationTest {

    private static final String REALM = "test";

    private static final String REFRESH_USER = "del-token-refresh";
    private static final String REFRESH_ID = "d0000005-0000-4000-8000-000000000005";
    private static final String CODE_USER = "del-token-code";
    private static final String CODE_ID = "d0000006-0000-4000-8000-000000000006";
    private static final String SELF_DELETED_USER = "del-token-self";
    private static final String SELF_DELETED_ID = "d0000007-0000-4000-8000-000000000007";
    private static final String KEPT_USER = "del-token-keep";
    private static final String KEPT_ID = "d0000008-0000-4000-8000-000000000008";
    private static final String INTROSPECT_USER = "del-token-introspect";
    private static final String INTROSPECT_ID = "d0000009-0000-4000-8000-000000000009";

    // username of the token rows written by hand, bound to no user
    private static final String RAW_USER = "del-token-raw";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserManager userManager;

    @Autowired
    private BootstrapConfig config;

    @Autowired
    private ExtTokenStore tokenStore;

    @Autowired
    private DataSource dataSource;

    private String clientId;
    private String clientSecret;

    @BeforeEach
    public void setUp() {
        ClientRegistration client = OAuth2ConfigUtils.with(config).client();
        clientId = client.getClientId();
        clientSecret = client.getClientSecret();
    }

    @Test
    @WithMockUserAuthentication(username = REFRESH_USER, realm = REALM)
    public void refreshAfterUserRemoval_errInvalidGrant() throws Exception {
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);
        assertThat(UserTokenTestUtils.userTokens(tokenStore, REFRESH_USER, REFRESH_ID)).isNotEmpty();

        UserSessionTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, REFRESH_ID));

        // tokens are removed on delete, so the refresh is refused
        assertThat(UserTokenTestUtils.userTokens(tokenStore, REFRESH_USER, REFRESH_ID)).isEmpty();
        assertThat(tokenStore.readRefreshToken(refreshToken)).isNull();
        UserTokenTestUtils.assertInvalidGrant(UserTokenTestUtils.refresh(mockMvc, clientId, clientSecret, refreshToken, 400));
    }

    @Test
    @WithMockUserAuthentication(username = CODE_USER, realm = REALM)
    public void codeIssuedBeforeUserRemoval_errInvalidGrant() throws Exception {
        String code = UserTokenTestUtils.getAuthorizationCode(mockMvc, clientId, Config.SCOPE_OPENID);

        UserSessionTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, CODE_ID));

        UserTokenTestUtils.assertInvalidGrant(UserTokenTestUtils.exchangeCode(mockMvc, clientId, clientSecret, code, 400));
    }

    @Test
    @WithMockUserAuthentication(username = SELF_DELETED_USER, realm = REALM)
    public void refreshAfterSelfDelete_errInvalidGrant() throws Exception {
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // the user deletes its own account from the console
        mockMvc
            .perform(MockMvcRequestBuilders.delete(UserSessionTestUtils.CONSOLE_DETAILS_URL + SELF_DELETED_ID))
            .andExpect(status().isOk());

        assertThat(UserTokenTestUtils.userTokens(tokenStore, SELF_DELETED_USER, SELF_DELETED_ID)).isEmpty();
        assertThat(tokenStore.readRefreshToken(refreshToken)).isNull();
        UserTokenTestUtils.assertInvalidGrant(UserTokenTestUtils.refresh(mockMvc, clientId, clientSecret, refreshToken, 400));
    }

    @Test
    @WithMockUserAuthentication(username = INTROSPECT_USER, realm = REALM)
    public void accessTokenAfterUserRemoval_inactive() throws Exception {
        String accessToken = OAuth2TestUtils.getUserAccessTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);
        assertThat(OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, "", clientId, clientSecret)).isTrue();

        UserSessionTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, INTROSPECT_ID));

        assertThat(OAuth2TestUtils.introspectTokenWithBasicAuth(mockMvc, accessToken, "", clientId, clientSecret)).isFalse();
    }

    @Test
    @WithMockUserAuthentication(username = KEPT_USER, realm = REALM)
    public void removeTokensKeepsSameUsernameOfOtherSubject() throws Exception {
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // same username, different subject: as a homonym user of another realm
        tokenStore.removeTokensByUserName(KEPT_USER, "another-subject-id");

        assertThat(UserTokenTestUtils.userTokens(tokenStore, KEPT_USER, KEPT_ID)).isNotEmpty();
        assertThat(tokenStore.readRefreshToken(refreshToken)).isNotNull();
        Map<String, Serializable> response = UserTokenTestUtils.refresh(mockMvc, clientId, clientSecret, refreshToken, 200);
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull();
    }

    @Test
    @WithMockUserAuthentication(username = KEPT_USER, realm = REALM)
    public void removeTokensWithNullArguments_noError() throws Exception {
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // nothing to match without a username or a subject: no error, no token removed
        assertThatCode(() -> tokenStore.removeTokensByUserName(null, KEPT_ID)).doesNotThrowAnyException();
        assertThatCode(() -> tokenStore.removeTokensByUserName(KEPT_USER, null)).doesNotThrowAnyException();

        assertThat(tokenStore.readRefreshToken(refreshToken)).isNotNull();
    }

    @Test
    @WithMockUserAuthentication(username = KEPT_USER, realm = REALM)
    public void removeTokensWithoutAccessToken_refreshRemoved() throws Exception {
        String refreshToken = OAuth2TestUtils.getUserRefreshTokenViaAuthCodeWithBasicAuth(mockMvc, clientId, clientSecret);

        // the access token is gone (expired and cleaned up), the refresh token is still stored
        tokenStore.removeAccessTokenUsingRefreshToken(tokenStore.readRefreshToken(refreshToken));
        assertThat(UserTokenTestUtils.userTokens(tokenStore, KEPT_USER, KEPT_ID)).isEmpty();

        tokenStore.removeTokensByUserName(KEPT_USER, KEPT_ID);

        assertThat(tokenStore.readRefreshToken(refreshToken)).isNull();
    }

    @Test
    public void removeTokensWithUnreadableTokens_noError() {
        // rows of a damaged store: tokens can not be deserialized
        UserTokenTestUtils.insertRawToken(dataSource, UserTokenTestUtils.ACCESS_TOKEN_TABLE, "raw-access", RAW_USER);
        UserTokenTestUtils.insertRawToken(dataSource, UserTokenTestUtils.REFRESH_TOKEN_TABLE, "raw-refresh", RAW_USER);

        try {
            assertThatCode(() -> tokenStore.removeTokensByUserName(RAW_USER, "any-subject-id")).doesNotThrowAnyException();

            // the store drops unreadable tokens when it finds them
            assertThat(UserTokenTestUtils.countTokens(dataSource, UserTokenTestUtils.ACCESS_TOKEN_TABLE, RAW_USER)).isZero();
            assertThat(UserTokenTestUtils.countTokens(dataSource, UserTokenTestUtils.REFRESH_TOKEN_TABLE, RAW_USER)).isZero();
        } finally {
            UserTokenTestUtils.deleteTokens(dataSource, UserTokenTestUtils.ACCESS_TOKEN_TABLE, RAW_USER);
            UserTokenTestUtils.deleteTokens(dataSource, UserTokenTestUtils.REFRESH_TOKEN_TABLE, RAW_USER);
        }
    }
}
