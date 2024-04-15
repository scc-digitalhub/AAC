/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.store.jdbc;

import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import it.smartcommunitylab.aac.oauth.store.ExtendedAuthenticationKeyGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

/**
 * Token store with DB tables creation on startup.
 *
 * @see {@link JdbcTokenStore}
 * @author raman
 *
 */
public class AutoJdbcTokenStore extends JdbcTokenStore implements ExtTokenStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JdbcTemplate jdbcTemplate;

    private static final String DEFAULT_SELECT_ACCESS_TOKEN_FROM_REFRESH_TOKEN =
        "select token_id, token from oauth_access_token where refresh_token = ?";

    private String selectAccessTokenFromRefreshTokenSql = DEFAULT_SELECT_ACCESS_TOKEN_FROM_REFRESH_TOKEN;

    /**
     * @param dataSource
     */
    public AutoJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        // set a saner authkey generator, but we should really just drop it, we won't
        // read back anyway
        this.setAuthenticationKeyGenerator(new ExtendedAuthenticationKeyGenerator());
    }

    //    /**
    //     * @param dataSource
    //     * @param createRefreshTokenStatement
    //     * @param createAccessTokenStatement
    //     */
    //    public AutoJdbcTokenStore(DataSource dataSource, String createRefreshTokenStatement,
    //            String createAccessTokenStatement) {
    //        super(dataSource);
    //        this.createRefreshTokenStatement = createRefreshTokenStatement;
    //        this.createAccessTokenStatement = createAccessTokenStatement;
    //        initSchema(dataSource);
    //    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        // we don't want to read a token from an authentication, it's a bad design
        return null;
    }

    public OAuth2AccessToken readAccessTokenForRefreshToken(String tokenValue) {
        OAuth2AccessToken accessToken = null;

        String key = extractTokenKey(tokenValue);

        try {
            accessToken =
                jdbcTemplate.queryForObject(
                    selectAccessTokenFromRefreshTokenSql,
                    new RowMapper<OAuth2AccessToken>() {
                        public OAuth2AccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return deserializeAccessToken(rs.getBytes(2));
                        }
                    },
                    key
                );
        } catch (EmptyResultDataAccessException e) {
            if (logger.isInfoEnabled()) {
                logger.debug("Failed to find access token for refresh " + tokenValue);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Could not extract access token for refresh " + tokenValue);
        }

        return accessToken;
    }

    @Override
    public OAuth2RefreshToken readRefreshTokenForAccessToken(String tokenValue) {
        // first we read access token and then extract refresh
        OAuth2AccessToken accessToken = readAccessToken(tokenValue);
        if (accessToken == null) {
            return null;
        }

        return accessToken.getRefreshToken();
    }
}
