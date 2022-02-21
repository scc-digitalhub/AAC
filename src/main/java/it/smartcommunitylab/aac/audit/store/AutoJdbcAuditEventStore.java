package it.smartcommunitylab.aac.audit.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.audit.RealmAuditEvent;

import org.springframework.jdbc.core.support.SqlLobValue;

public class AutoJdbcAuditEventStore implements AuditEventStore {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AuditEvent> rowMapper = new AuditEventRowMapper();

    private static final String DEFAULT_CREATE_TABLE_STATEMENT = " CREATE TABLE IF NOT EXISTS audit (" +
            "  time TIMESTAMP," +
            "  principal varchar(255)," +
            "  realm varchar(255) DEFAULT NULL," +
            "  type varchar(255)," +
            "  event BLOB ) ";

    private static final String DEFAULT_INSERT_STATEMENT = "insert into audit (time, principal, realm , type, event ) values (?, ?, ?, ?, ?)";

    private static final String DEFAULT_SELECT_PRINCIPAL_STATEMENT = "select time, principal, realm , type, event from audit where principal = ?";
    private static final String DEFAULT_SELECT_REALM_STATEMENT = "select time, principal, realm , type, event from audit where realm = ?";

    private static final String DEFAULT_COUNT_PRINCIPAL_STATEMENT = "select count(*) from audit where principal = ?";
    private static final String DEFAULT_COUNT_REALM_STATEMENT = "select count(*) from audit where realm = ?";

    private static final String TIME_AFTER_CONDITION = "time >= ?";
    private static final String TIME_BETWEEN_CONDITION = "time between ? and ? ";
    private static final String TYPE_CONDITION = "type = ?";

    private static final String DEFAULT_ORDER_BY = "order by time DESC";

    private String createAuditTableSql = DEFAULT_CREATE_TABLE_STATEMENT;
    private String insertAuditEventSql = DEFAULT_INSERT_STATEMENT;

    private String selectByPrincipalAuditEvent = DEFAULT_SELECT_PRINCIPAL_STATEMENT;
    private String selectByRealmAuditEvent = DEFAULT_SELECT_REALM_STATEMENT;
    private String countByPrincipalAuditEvent = DEFAULT_COUNT_PRINCIPAL_STATEMENT;
    private String countByRealmAuditEvent = DEFAULT_COUNT_REALM_STATEMENT;

    private String timeAfterCondition = TIME_AFTER_CONDITION;
    private String timeBetweenCondition = TIME_BETWEEN_CONDITION;
    private String typeCondition = TYPE_CONDITION;

    private String orderBy = DEFAULT_ORDER_BY;

    public AutoJdbcAuditEventStore(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource required");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initSchema();
    }

    protected void initSchema() {
        jdbcTemplate.execute(createAuditTableSql);
    }

    @Override
    public void add(AuditEvent event) {
        // extract data
        String principal = event.getPrincipal();
        long time = event.getTimestamp().toEpochMilli();
        String type = event.getType();
        String realm = null;
        if (event instanceof RealmAuditEvent) {
            realm = ((RealmAuditEvent) event).getRealm();
        }

        jdbcTemplate.update(insertAuditEventSql,
                new Object[] {
                        new java.sql.Timestamp(time),
                        principal, realm, type,
                        new SqlLobValue(SerializationUtils.serialize(event))
                }, new int[] { Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BLOB });

    }

    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        StringBuilder query = new StringBuilder();
        query.append(selectByPrincipalAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(principal);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        if (after != null) {
            query.append(" AND ").append(timeAfterCondition);
            params.add(new java.sql.Timestamp(after.toEpochMilli()));
        }

        query.append(" ").append(orderBy);

        return jdbcTemplate.query(query.toString(), rowMapper, params.toArray(new Object[0]));
    }

    @Override
    public long countByRealm(String realm, Instant after, Instant before, String type) {
        StringBuilder query = new StringBuilder();
        query.append(countByRealmAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(realm);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        if (after != null) {
            if (before != null) {
                query.append(" AND ").append(timeBetweenCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
                params.add(new java.sql.Timestamp(before.toEpochMilli()));
            } else {
                query.append(" AND ").append(timeAfterCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
            }
        }

        query.append(" ").append(orderBy);

        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, params.toArray(new Object[0]));
        if (count == null) {
            return 0;
        }
        return count.longValue();
    }

    @Override
    public List<RealmAuditEvent> findByRealm(String realm, Instant after, Instant before,
            String type) {
        StringBuilder query = new StringBuilder();
        query.append(selectByRealmAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(realm);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        if (after != null) {
            if (before != null) {
                query.append(" AND ").append(timeBetweenCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
                params.add(new java.sql.Timestamp(before.toEpochMilli()));
            } else {
                query.append(" AND ").append(timeAfterCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
            }
        }

        query.append(" ").append(orderBy);

        return jdbcTemplate.query(query.toString(), rowMapper, params.toArray(new Object[0])).stream()
                .filter(e -> (e instanceof RealmAuditEvent))
                .map(e -> (RealmAuditEvent) e)
                .collect(Collectors.toList());

    }

    @Override
    public List<AuditEvent> findByPrincipal(String principal, Instant after, Instant before, String type) {
        StringBuilder query = new StringBuilder();
        query.append(selectByPrincipalAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(principal);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        if (after != null) {
            if (before != null) {
                query.append(" AND ").append(timeBetweenCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
                params.add(new java.sql.Timestamp(before.toEpochMilli()));
            } else {
                query.append(" AND ").append(timeAfterCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
            }
        }

        query.append(" ").append(orderBy);

        return jdbcTemplate.query(query.toString(), rowMapper, params.toArray(new Object[0]));
    }

    @Override
    public long countByPrincipal(String principal, Instant after, Instant before, String type) {
        StringBuilder query = new StringBuilder();
        query.append(countByPrincipalAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(principal);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        if (after != null) {
            if (before != null) {
                query.append(" AND ").append(timeBetweenCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
                params.add(new java.sql.Timestamp(before.toEpochMilli()));
            } else {
                query.append(" AND ").append(timeAfterCondition);
                params.add(new java.sql.Timestamp(after.toEpochMilli()));
            }
        }

        query.append(" ").append(orderBy);

        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, params.toArray(new Object[0]));
        if (count == null) {
            return 0;
        }
        return count.longValue();
    }

    public void setCreateAuditTableSql(String createAuditTableSql) {
        this.createAuditTableSql = createAuditTableSql;
    }

    public void setInsertAuditEventSql(String insertAuditEventSql) {
        this.insertAuditEventSql = insertAuditEventSql;
    }

    public void setSelectByPrincipalAuditEvent(String selectByPrincipalAuditEvent) {
        this.selectByPrincipalAuditEvent = selectByPrincipalAuditEvent;
    }

    public void setSelectByRealmAuditEvent(String selectByRealmAuditEvent) {
        this.selectByRealmAuditEvent = selectByRealmAuditEvent;
    }

    public void setCountByPrincipalAuditEvent(String countByPrincipalAuditEvent) {
        this.countByPrincipalAuditEvent = countByPrincipalAuditEvent;
    }

    public void setCountByRealmAuditEvent(String countByRealmAuditEvent) {
        this.countByRealmAuditEvent = countByRealmAuditEvent;
    }

    public void setTimeAfterCondition(String timeAfterCondition) {
        this.timeAfterCondition = timeAfterCondition;
    }

    public void setTimeBetweenCondition(String timeBetweenCondition) {
        this.timeBetweenCondition = timeBetweenCondition;
    }

    public void setTypeCondition(String typeCondition) {
        this.typeCondition = typeCondition;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    private static class AuditEventRowMapper implements RowMapper<AuditEvent> {

        @Override
        public AuditEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
//            long time = rs.getLong("time");
//            String principal = rs.getString("principal");
//            String realm = rs.getString("realm");
//            String type = rs.getString("type");
            AuditEvent event = SerializationUtils.deserialize(rs.getBytes("event"));
            return event;
        }
    }

}
