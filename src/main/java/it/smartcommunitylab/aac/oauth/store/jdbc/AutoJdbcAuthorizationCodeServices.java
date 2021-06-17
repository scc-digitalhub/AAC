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

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.provider.PeekableAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.store.HumanStringKeyGenerator;

/**
 * Authorization code services with DB table creation on startup. Also supports
 * lifetime limit as per RFC6749
 * https://tools.ietf.org/html/rfc6749#section-4.1.1
 * 
 * @see {@link JdbcAuthorizationCodeServices}
 * @author raman
 *
 */
public class AutoJdbcAuthorizationCodeServices
        implements AuthorizationCodeServices, PeekableAuthorizationCodeServices {

    private static final StringKeyGenerator TOKEN_GENERATOR = new HumanStringKeyGenerator(6);
    private static final int DEFAULT_CODE_VALIDITY_SECONDS = 10 * 60;

    private static final String DEFAULT_CREATE_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS oauth_code (code VARCHAR(256), client_id VARCHAR(256), expiresAt TIMESTAMP, authentication BLOB);";
    private static final String DEFAULT_SELECT_STATEMENT = "select code, client_id, expiresAt, authentication from oauth_code where code = ?";
    private static final String DEFAULT_INSERT_STATEMENT = "insert into oauth_code (code, client_id, expiresAt, authentication) values (?, ?, ?, ?)";
    private static final String DEFAULT_DELETE_STATEMENT = "delete from oauth_code where code = ?";

    private String createAuthenticationSql = DEFAULT_CREATE_TABLE_STATEMENT;
    private String selectAuthenticationSql = DEFAULT_SELECT_STATEMENT;
    private String insertAuthenticationSql = DEFAULT_INSERT_STATEMENT;
    private String deleteAuthenticationSql = DEFAULT_DELETE_STATEMENT;

    private JdbcTemplate jdbcTemplate;
    private StringKeyGenerator tokenGenerator;
    private int codeValidityMillis = DEFAULT_CODE_VALIDITY_SECONDS * 1000;

    /**
     * @param dataSource
     */
    public AutoJdbcAuthorizationCodeServices(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource required");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initSchema();
    }

    /**
     * @param dataSource
     * @param code       validity
     */
    public AutoJdbcAuthorizationCodeServices(DataSource dataSource, int codeValidity) {
        this(dataSource);
        this.codeValidityMillis = codeValidity * 1000;
        this.tokenGenerator = TOKEN_GENERATOR;
    }

    public String createAuthorizationCode(OAuth2Authentication authentication) {
        // build a secure random code and store
        String code = tokenGenerator.generateKey();
        store(code, authentication);

        return code;
    }

    public OAuth2Authentication consumeAuthorizationCode(String code)
            throws InvalidGrantException {
        OAuth2Authentication auth = remove(code);
        if (auth == null) {
            throw new InvalidGrantException("Invalid authorization code: " + code);
        }

        return auth;
    }

    public OAuth2Authentication peekAuthorizationCode(String code)
            throws InvalidGrantException {
        OAuth2Authentication auth = this.load(code);
        // we can return null if missing
        return auth;
    }

    protected void store(String code, OAuth2Authentication authentication) {
        // extract clientId
        String clientId = authentication.getOAuth2Request().getClientId();

        jdbcTemplate.update(insertAuthenticationSql,
                new Object[] {
                        code, clientId,
                        new java.sql.Timestamp(System.currentTimeMillis() + codeValidityMillis),
                        new SqlLobValue(SerializationUtils.serialize(authentication))
                }, new int[] { Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.BLOB });
    }

    public OAuth2Authentication load(String code) {
        Pair<OAuth2Authentication, Long> authentication;
        try {
            authentication = jdbcTemplate.queryForObject(selectAuthenticationSql,
                    new RowMapper<Pair<OAuth2Authentication, Long>>() {
                        public Pair<OAuth2Authentication, Long> mapRow(ResultSet rs, int rowNum) throws SQLException {
                            OAuth2Authentication a = SerializationUtils.deserialize(rs.getBytes("authentication"));
                            Long e = rs.getTimestamp("expiresAt").getTime();
                            return Pair.of(a, e);
                        }
                    }, code);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        if (authentication != null) {
            long expiresAt = authentication.getSecond().longValue();
            OAuth2Authentication oauth = authentication.getFirst();

            // validate expire
            if (System.currentTimeMillis() < expiresAt) {
                return oauth;
            }
        }

        return null;
    }

    public OAuth2Authentication remove(String code) {
        Pair<OAuth2Authentication, Long> authentication;
        try {
            authentication = jdbcTemplate.queryForObject(selectAuthenticationSql,
                    new RowMapper<Pair<OAuth2Authentication, Long>>() {
                        public Pair<OAuth2Authentication, Long> mapRow(ResultSet rs, int rowNum) throws SQLException {
                            OAuth2Authentication a = SerializationUtils.deserialize(rs.getBytes("authentication"));
                            Long e = rs.getTimestamp("expiresAt").getTime();
                            return Pair.of(a, e);
                        }
                    }, code);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        if (authentication != null) {
            // remove
            jdbcTemplate.update(deleteAuthenticationSql, code);

            long expiresAt = authentication.getSecond().longValue();
            OAuth2Authentication oauth = authentication.getFirst();

            // validate expire
            if (System.currentTimeMillis() < expiresAt) {
                return oauth;
            }
        }

        return null;
    }

    public void setCreateAuthenticationSql(String createAuthenticationSql) {
        this.createAuthenticationSql = createAuthenticationSql;
    }

    public void setSelectAuthenticationSql(String selectAuthenticationSql) {
        this.selectAuthenticationSql = selectAuthenticationSql;
    }

    public void setInsertAuthenticationSql(String insertAuthenticationSql) {
        this.insertAuthenticationSql = insertAuthenticationSql;
    }

    public void setDeleteAuthenticationSql(String deleteAuthenticationSql) {
        this.deleteAuthenticationSql = deleteAuthenticationSql;
    }

    public void setTokenGenerator(StringKeyGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    protected void initSchema() {
        jdbcTemplate.execute(createAuthenticationSql);
    }
}
