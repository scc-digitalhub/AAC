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

import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import it.smartcommunitylab.aac.oauth.store.ExtendedAuthenticationKeyGenerator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
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

    private static final String DEFAULT_ACCESS_TOKEN_INSERT_STATEMENT = "insert into oauth_access_token (token_id, token, authentication_id, user_name, client_id, issued_at, expires_at, authentication, refresh_token) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEFAULT_SELECT_ACCESS_TOKEN_FROM_REFRESH_TOKEN =
        "select token_id, token from oauth_access_token where refresh_token = ?";
    private static final String DEFAULT_REFRESH_TOKEN_SELECT_STATEMENT = "select token_id, token from oauth_refresh_token where token_id = ? FOR UPDATE";
	private static final String DEFAULT_REFRESH_TOKEN_INSERT_STATEMENT = "insert into oauth_refresh_token (token_id, token, authentication_id, user_name, client_id, issued_at, expires_at, authentication) values (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String DEFAULT_DELETE_EXPIRED_ACCESS_TOKENS_STATEMENT = "delete from oauth_access_token where expires_at < ?";
    private static final String DEFAULT_DELETE_EXPIRED_REFRESH_TOKENS_STATEMENT = "delete from oauth_refresh_token where expires_at < ?";

    private String insertAccessTokenSql = DEFAULT_ACCESS_TOKEN_INSERT_STATEMENT;
    private String selectAccessTokenFromRefreshTokenSql = DEFAULT_SELECT_ACCESS_TOKEN_FROM_REFRESH_TOKEN;
	private String deleteExpiredAccessTokenSql = DEFAULT_DELETE_EXPIRED_ACCESS_TOKENS_STATEMENT;	

	private String insertRefreshTokenSql = DEFAULT_REFRESH_TOKEN_INSERT_STATEMENT;
    private String selectRefreshTokenSql = DEFAULT_REFRESH_TOKEN_SELECT_STATEMENT;
    private String deleteExpiredRefreshTokenSql = DEFAULT_DELETE_EXPIRED_REFRESH_TOKENS_STATEMENT;

    private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();


    public AutoJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        // set a saner authkey generator, but we should really just drop it, we won't
        // read back anyway
        this.authenticationKeyGenerator = new ExtendedAuthenticationKeyGenerator();
        super.setAuthenticationKeyGenerator(this.authenticationKeyGenerator );
    }


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

    public OAuth2RefreshToken readRefreshTokenForUpdate(String token) {
		OAuth2RefreshToken refreshToken = null;

		try {
            refreshToken = jdbcTemplate.queryForObject(selectRefreshTokenSql, new RowMapper<OAuth2RefreshToken>() {
				public OAuth2RefreshToken mapRow(ResultSet rs, int rowNum) throws SQLException {
					return deserializeRefreshToken(rs.getBytes(2));
				}
			}, extractTokenKey(token));
		} catch (EmptyResultDataAccessException e) {			
		} catch (IllegalArgumentException e) {
			removeRefreshToken(token);
		}

		return refreshToken;
	}


    @Override
	public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
		String refreshToken = null;
		if (token.getRefreshToken() != null) {
			refreshToken = token.getRefreshToken().getValue();
		}

        java.sql.Timestamp expiresAt = null;
        if(token.getExpiration() != null) {
            expiresAt =  new java.sql.Timestamp(token.getExpiration().getTime());
        }

        java.sql.Timestamp issuedAt = new java.sql.Timestamp(System.currentTimeMillis());
        if(token instanceof AACOAuth2AccessToken && ((AACOAuth2AccessToken)token).getIssuedAt() != null) {
            issuedAt =  new java.sql.Timestamp(((AACOAuth2AccessToken)token).getIssuedAt().getTime());            
        }

		//DISABLED: store should store *new* tokens, not update!
		// if (readAccessToken(token.getValue())!=null) {
		// 	removeAccessToken(token.getValue());
		// }

		jdbcTemplate.update(insertAccessTokenSql, new Object[] { extractTokenKey(token.getValue()),
				new SqlLobValue(serializeAccessToken(token)), authenticationKeyGenerator.extractKey(authentication),
				authentication.isClientOnly() ? null : authentication.getName(),
				authentication.getOAuth2Request().getClientId(),
                issuedAt, expiresAt,
				new SqlLobValue(serializeAuthentication(authentication)), extractTokenKey(refreshToken) }, new int[] {
				Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP, Types.BLOB, Types.VARCHAR });
	}

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        java.sql.Timestamp expiresAt = null;
        if(refreshToken instanceof ExpiringOAuth2RefreshToken && ((ExpiringOAuth2RefreshToken)refreshToken).getExpiration() != null) {
            expiresAt =  new java.sql.Timestamp(((ExpiringOAuth2RefreshToken)refreshToken).getExpiration().getTime());            
        }

		jdbcTemplate.update(insertRefreshTokenSql, new Object[] { extractTokenKey(refreshToken.getValue()),
				new SqlLobValue(serializeRefreshToken(refreshToken)),authenticationKeyGenerator.extractKey(authentication),
				authentication.isClientOnly() ? null : authentication.getName(),
				authentication.getOAuth2Request().getClientId(),
                new java.sql.Timestamp(System.currentTimeMillis()), expiresAt,
				new SqlLobValue(serializeAuthentication(authentication)) }, new int[] { 
					Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP, Types.BLOB });
	}


    @Override
    public void deleteExpiredAccessTokens(long interval) {
        //interval in seconds
        long timestamp = System.currentTimeMillis()- (interval*1000);

        jdbcTemplate.update(deleteExpiredAccessTokenSql, new Object[] {new java.sql.Timestamp(timestamp)}, new int[] {Types.TIMESTAMP});
    }


    @Override
    public void deleteExpiredRefreshTokens(long interval) {
          //interval in seconds
          long timestamp = System.currentTimeMillis()- (interval*1000);

          jdbcTemplate.update(deleteExpiredRefreshTokenSql, new Object[] {new java.sql.Timestamp(timestamp)}, new int[] {Types.TIMESTAMP});
      }
   
}
