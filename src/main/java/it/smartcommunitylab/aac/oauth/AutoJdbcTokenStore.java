/**
 *    Copyright 2012-2013 Trento RISE
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

package it.smartcommunitylab.aac.oauth;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

/**
 * Token store with DB tables creation on startup.
 * @see {@link JdbcTokenStore}
 * @author raman
 *
 */
public class AutoJdbcTokenStore extends JdbcTokenStore implements ExtTokenStore {

	private JdbcTemplate jdbcTemplate;
	
	private Log logger = LogFactory.getLog(getClass());
	
	private static final String DEFAULT_CREATE_RT_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS oauth_refresh_token ( token_id VARCHAR(64) NOT NULL PRIMARY KEY, token BLOB NOT NULL, authentication BLOB NOT NULL);";
	private static final String DEFAULT_CREATE_AT_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS oauth_access_token (token_id VARCHAR(256),  token BLOB, authentication_id VARCHAR(256), user_name VARCHAR(256), client_id VARCHAR(256), authentication BLOB, refresh_token VARCHAR(256));";

	private static final String DEFAULT_SELECT_ACCESS_TOKEN_FROM_REFRESH_TOKEN = "select token_id, token from oauth_access_token where refresh_token = ?";
	
	private String createRefreshTokenStatement = DEFAULT_CREATE_RT_TABLE_STATEMENT;
	private String createAccessTokenStatement = DEFAULT_CREATE_AT_TABLE_STATEMENT;

	private String selectAccessTokenFromRefreshTokenSql = DEFAULT_SELECT_ACCESS_TOKEN_FROM_REFRESH_TOKEN;
	
	/**
	 * @param dataSource
	 */
	public AutoJdbcTokenStore(DataSource dataSource) {
		super(dataSource);
		initSchema(dataSource);
	}

	protected void initSchema(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute(createAccessTokenStatement);
		jdbcTemplate.execute(createRefreshTokenStatement);
	}

	/**
	 * @param dataSource
	 * @param createRefreshTokenStatement
	 * @param createAccessTokenStatement
	 */
	public AutoJdbcTokenStore(DataSource dataSource, String createRefreshTokenStatement, String createAccessTokenStatement) {
		super(dataSource);
		this.createRefreshTokenStatement = createRefreshTokenStatement;
		this.createAccessTokenStatement = createAccessTokenStatement;
		initSchema(dataSource);
	}

	public OAuth2AccessToken readAccessTokenForRefreshToken(String tokenValue) {
		OAuth2AccessToken accessToken = null;
		
		String key = extractTokenKey(tokenValue);
		
		try {
			accessToken = jdbcTemplate.queryForObject(selectAccessTokenFromRefreshTokenSql ,
					new RowMapper<OAuth2AccessToken>() {
						public OAuth2AccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
							return deserializeAccessToken(rs.getBytes(2));
						}
					}, key);
		}
		catch (EmptyResultDataAccessException e) {
			if (logger.isInfoEnabled()) {
				logger.debug("Failed to find access token for refresh " + tokenValue);
			}
		}
		catch (IllegalArgumentException e) {
			logger.error("Could not extract access token for refresh " + tokenValue);
		}
		
		return accessToken;
	}
}
