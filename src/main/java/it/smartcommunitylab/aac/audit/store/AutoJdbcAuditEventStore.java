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

package it.smartcommunitylab.aac.audit.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.audit.model.ApplicationAuditEvent;
import it.smartcommunitylab.aac.audit.model.ExtendedAuditEvent;
import it.smartcommunitylab.aac.audit.model.RealmAuditEvent;
import it.smartcommunitylab.aac.audit.model.TxAuditEvent;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class AutoJdbcAuditEventStore implements AuditEventStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

    private final JdbcTemplate jdbcTemplate;
    private RowMapper<AuditEvent> rowMapper;
    private ObjectMapper mapper;

    private static final String DEFAULT_INSERT_STATEMENT =
        "INSERT INTO audit_events (event_time, principal, realm, tx, event_type, event_class, event_data ) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String DEFAULT_SELECT_PRINCIPAL_STATEMENT =
        "SELECT event_time, principal, event_type, event_data FROM audit_events WHERE principal = ?";
    private static final String DEFAULT_SELECT_REALM_STATEMENT =
        "SELECT event_time, principal, event_type, event_data FROM audit_events WHERE realm = ?";
    private static final String DEFAULT_SELECT_TX_STATEMENT =
        "SELECT event_time, principal, event_type, event_data FROM audit_events WHERE tx = ?";

    private static final String DEFAULT_COUNT_PRINCIPAL_STATEMENT =
        "SELECT COUNT(*) FROM audit_events WHERE principal = ?";
    private static final String DEFAULT_COUNT_REALM_STATEMENT = "SELECT COUNT(*) FROM audit_events WHERE realm = ?";
    private static final String DEFAULT_COUNT_TX_STATEMENT = "SELECT COUNT(*) FROM audit_events WHERE tx = ?";

    private static final String TIME_AFTER_CONDITION = "event_time >= ?";
    private static final String TIME_BETWEEN_CONDITION = "event_time BETWEEN ? AND ? ";
    private static final String TYPE_CONDITION = "event_type = ?";

    private static final String OFFSET_LIMIT_CONDITION = "OFFSET ? LIMIT ?";

    private static final String DEFAULT_ORDER_BY = "ORDER BY event_time DESC";

    private String insertAuditEventSql = DEFAULT_INSERT_STATEMENT;

    private String selectByPrincipalAuditEvent = DEFAULT_SELECT_PRINCIPAL_STATEMENT;
    private String selectByRealmAuditEvent = DEFAULT_SELECT_REALM_STATEMENT;
    private String selectByTxAuditEvent = DEFAULT_SELECT_TX_STATEMENT;
    private String countByPrincipalAuditEvent = DEFAULT_COUNT_PRINCIPAL_STATEMENT;
    private String countByRealmAuditEvent = DEFAULT_COUNT_REALM_STATEMENT;
    private String countByTxAuditEvent = DEFAULT_COUNT_TX_STATEMENT;

    private String timeAfterCondition = TIME_AFTER_CONDITION;
    private String timeBetweenCondition = TIME_BETWEEN_CONDITION;
    private String typeCondition = TYPE_CONDITION;

    private String offsetLimitCondition = OFFSET_LIMIT_CONDITION;

    private String orderBy = DEFAULT_ORDER_BY;

    private Converter<Map<String, Object>, byte[]> writer;
    private Converter<byte[], Map<String, Object>> reader;

    public AutoJdbcAuditEventStore(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource required");
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        //use CBOR by default as mapper
        this.mapper =
            new CBORMapper()
                .registerModule(new JavaTimeModule())
                //include only non-null fields
                .setSerializationInclusion(Include.NON_NULL)
                //add mixin for including typeInfo in events
                .addMixIn(ApplicationEvent.class, AuditApplicationEventMixIns.class);

        this.writer =
            map -> {
                try {
                    return (mapper.writeValueAsBytes(map));
                } catch (JsonProcessingException e) {
                    logger.error("error writing data: {}", e.getMessage());
                    throw new IllegalArgumentException("error writing data", e);
                }
            };

        this.reader =
            bytes -> {
                try {
                    return mapper.readValue(bytes, typeRef);
                } catch (IOException e) {
                    logger.error("error reading data: {}", e.getMessage());
                    throw new IllegalArgumentException("error reading data", e);
                }
            };

        this.rowMapper = new AuditEventMappedRowMapper(reader);
    }

    public void setWriter(Converter<Map<String, Object>, byte[]> writer) {
        this.writer = writer;
    }

    public void setReader(Converter<byte[], Map<String, Object>> reader) {
        this.reader = reader;
        this.rowMapper = new AuditEventMappedRowMapper(reader);
    }

    @Override
    public void add(AuditEvent event) {
        // extract data and repack
        String principal = event.getPrincipal();
        long time = event.getTimestamp().toEpochMilli();
        String type = event.getType();

        ExtendedAuditEvent<ApplicationEvent> eae = ExtendedAuditEvent.from(event);

        String realm = eae.getRealm();
        String tx = eae.getTx();
        String clazz = eae.getClazz();

        //pack audit event in data
        Map<String, Object> data = mapper.convertValue(event, typeRef);

        byte[] bytes = writer != null ? writer.convert(data) : null;

        jdbcTemplate.update(
            insertAuditEventSql,
            new Object[] { new java.sql.Timestamp(time), principal, realm, tx, type, clazz, new SqlLobValue(bytes) },
            new int[] {
                Types.TIMESTAMP,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.BLOB,
            }
        );
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

        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, params.toArray(new Object[0]));
        if (count == null) {
            return 0;
        }
        return count.longValue();
    }

    @Override
    public List<AuditEvent> findByRealm(String realm, Instant after, Instant before, String type) {
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

        return jdbcTemplate.query(query.toString(), rowMapper, params.toArray(new Object[0]));
    }

    @Override
    public Page<AuditEvent> searchByRealm(String realm, Instant after, Instant before, String type, @NotNull Pageable pageable) {
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

        query.append(" ").append(offsetLimitCondition);
        params.add(pageable.getOffset());
        params.add(pageable.getPageSize());

        return PageableExecutionUtils.getPage(
            jdbcTemplate.query(query.toString(), rowMapper, params.toArray(new Object[0])),
            pageable,
            () -> countByRealm(realm, after, before, type)
        );
    }

    @Override
    public long countByTx(String tx, String type) {
        StringBuilder query = new StringBuilder();
        query.append(countByTxAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(tx);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, params.toArray(new Object[0]));
        if (count == null) {
            return 0;
        }
        return count.longValue();
    }

    @Override
    public List<AuditEvent> findByTx(String tx, String type) {
        StringBuilder query = new StringBuilder();
        query.append(selectByTxAuditEvent);

        List<Object> params = new LinkedList<>();
        params.add(tx);

        if (StringUtils.hasText(type)) {
            query.append(" AND ").append(typeCondition);
            params.add(type);
        }

        query.append(" ").append(orderBy);

        return jdbcTemplate.query(query.toString(), rowMapper, params.toArray(new Object[0]));
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

        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, params.toArray(new Object[0]));
        if (count == null) {
            return 0;
        }
        return count.longValue();
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

    private class AuditEventMappedRowMapper implements RowMapper<AuditEvent> {

        private final Converter<byte[], Map<String, Object>> reader;

        public AuditEventMappedRowMapper(Converter<byte[], Map<String, Object>> reader) {
            this.reader = reader;
        }

        @Override
        public AuditEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp time = rs.getTimestamp("event_time");
            String principal = rs.getString("principal");
            String type = rs.getString("event_type");
            byte[] bytes = rs.getBytes("event_data");
            Map<String, Object> data = Collections.emptyMap();
            Map<String, Object> raw = reader != null ? reader.convert(bytes) : Collections.emptyMap();

            //unpack event to extract data
            if (raw != null && raw.containsKey("data")) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) raw.get("data");
                    data = d;
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("invalid value for data");
                }
            }

            return new AuditEvent(time.toInstant(), principal, type, data);
        }
    }
}
