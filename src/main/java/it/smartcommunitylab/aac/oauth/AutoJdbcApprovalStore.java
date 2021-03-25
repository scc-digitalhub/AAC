package it.smartcommunitylab.aac.oauth;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;

public class AutoJdbcApprovalStore extends JdbcApprovalStore {

    private JdbcTemplate jdbcTemplate;

    private static final String DEFAULT_CREATE_TABLE_STATEMENT = " CREATE TABLE IF NOT EXISTS `oauth_approvals` (" +
            "  `clientId` varchar(255) DEFAULT NULL," +
            "  `expiresAt` datetime DEFAULT NULL," +
            "  `lastModifiedAt` datetime DEFAULT NULL," +
            "  `scope` varchar(255) DEFAULT NULL," +
            "  `status` varchar(255) DEFAULT NULL," +
            "  `userId` varchar(255) DEFAULT NULL) ";

    private String createTableStatement = DEFAULT_CREATE_TABLE_STATEMENT;

    public AutoJdbcApprovalStore(DataSource dataSource) {
        super(dataSource);
        initSchema(dataSource);

    }

    protected void initSchema(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(createTableStatement);
    }
}
