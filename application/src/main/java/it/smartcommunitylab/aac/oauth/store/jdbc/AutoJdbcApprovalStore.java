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

package it.smartcommunitylab.aac.oauth.store.jdbc;

import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import javax.sql.DataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;

public class AutoJdbcApprovalStore extends JdbcApprovalStore implements SearchableApprovalStore {

    private JdbcTemplate jdbcTemplate;
    private final RowMapper<Approval> rowMapper = new AuthorizationRowMapper();

    private static final String DEFAULT_FIND_SINGLE_APPROVAL_SQL =
        "SELECT expiresAt,status,lastModifiedAt,userId,clientId,scope FROM oauth_approvals WHERE userId=? AND clientId=? AND scope=?";
    private static final String DEFAULT_GET_USER_APPROVAL_SQL =
        "SELECT expiresAt,status,lastModifiedAt,userId,clientId,scope FROM oauth_approvals WHERE userId=?";
    private static final String DEFAULT_GET_CLIENT_APPROVAL_SQL =
        "SELECT expiresAt,status,lastModifiedAt,userId,clientId,scope FROM oauth_approvals WHERE clientId=?";
    private static final String DEFAULT_GET_SCOPE_APPROVAL_SQL =
        "SELECT expiresAt,status,lastModifiedAt,userId,clientId,scope FROM oauth_approvals WHERE scope=?";
    private static final String DEFAULT_GET_USER_SCOPE_APPROVAL_SQL =
        "SELECT expiresAt,status,lastModifiedAt,userId,clientId,scope FROM oauth_approvals WHERE userId=? and scope=?";

    private String findSingleApprovalStatement = DEFAULT_FIND_SINGLE_APPROVAL_SQL;
    private String findUserApprovalStatement = DEFAULT_GET_USER_APPROVAL_SQL;
    private String findClientApprovalStatement = DEFAULT_GET_CLIENT_APPROVAL_SQL;
    private String findScopeApprovalStatement = DEFAULT_GET_SCOPE_APPROVAL_SQL;
    private String findUserScopeApprovalStatement = DEFAULT_GET_USER_SCOPE_APPROVAL_SQL;

    public AutoJdbcApprovalStore(DataSource dataSource) {
        super(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Collection<Approval> findUserApprovals(String userName) {
        return jdbcTemplate.query(findUserApprovalStatement, rowMapper, userName);
    }

    @Override
    public Collection<Approval> findClientApprovals(String clientId) {
        return jdbcTemplate.query(findClientApprovalStatement, rowMapper, clientId);
    }

    @Override
    public Collection<Approval> findScopeApprovals(String scope) {
        return jdbcTemplate.query(findScopeApprovalStatement, rowMapper, scope);
    }

    @Override
    public Approval findApproval(String userId, String clientId, String scope) {
        try {
            return jdbcTemplate.queryForObject(findSingleApprovalStatement, rowMapper, userId, clientId, scope);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<Approval> findUserScopeApprovals(String userName, String scope) {
        return jdbcTemplate.query(findUserScopeApprovalStatement, rowMapper, userName, scope);
    }

    private static class AuthorizationRowMapper implements RowMapper<Approval> {

        @Override
        public Approval mapRow(ResultSet rs, int rowNum) throws SQLException {
            String userName = rs.getString(4);
            String clientId = rs.getString(5);
            String scope = rs.getString(6);
            Date expiresAt = rs.getTimestamp(1);
            String status = rs.getString(2);
            Date lastUpdatedAt = rs.getTimestamp(3);

            return new Approval(userName, clientId, scope, expiresAt, ApprovalStatus.valueOf(status), lastUpdatedAt);
        }
    }
}
