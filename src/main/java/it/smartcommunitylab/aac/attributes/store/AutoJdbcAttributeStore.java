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

package it.smartcommunitylab.aac.attributes.store;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.util.Assert;

public class AutoJdbcAttributeStore {

    private static final String DEFAULT_CREATE_TABLE_STATEMENT =
        "CREATE TABLE IF NOT EXISTS attributes (entity_id VARCHAR(256), provider_id VARCHAR(256), attr_key VARCHAR(256), attr_value BLOB);";
    private static final String DEFAULT_SELECT_STATEMENT =
        "select attr_value from attributes where  provider_id = ? and entity_id = ? and attr_key = ?";
    private static final String DEFAULT_FIND_STATEMENT =
        "select entity_id, provider_id, attr_key, attr_value from attributes where  provider_id = ? and entity_id = ?";
    private static final String DEFAULT_INSERT_STATEMENT =
        "insert into attributes (provider_id, entity_id, attr_key, attr_value) values (?, ?, ?, ?)";
    private static final String DEFAULT_UPDATE_STATEMENT =
        "update attributes set attr_value = ?  where  provider_id = ? and entity_id = ? and attr_key = ?";
    private static final String DEFAULT_DELETE_STATEMENT =
        "delete from attributes where provider_id = ? and entity_id = ? and attr_key = ?";
    private static final String DEFAULT_CLEAR_STATEMENT =
        "delete from attributes where provider_id = ? and entity_id = ?";

    private String createAttributesSql = DEFAULT_CREATE_TABLE_STATEMENT;
    private String selectAttributeSql = DEFAULT_SELECT_STATEMENT;
    private String findAttributesSql = DEFAULT_FIND_STATEMENT;
    private String insertAttributeSql = DEFAULT_INSERT_STATEMENT;
    private String updateAttributeSql = DEFAULT_UPDATE_STATEMENT;
    private String deleteAttributeSql = DEFAULT_DELETE_STATEMENT;
    private String clearAttributeSql = DEFAULT_CLEAR_STATEMENT;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Pair<String, Optional<Serializable>>> rowMapper = new AttributeRowMapper();

    public AutoJdbcAttributeStore(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource required");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initSchema();
    }

    protected void initSchema() {
        jdbcTemplate.execute(createAttributesSql);
    }

    public Serializable getAttribute(String providerId, String entityId, String key) {
        List<Pair<String, Optional<Serializable>>> list = jdbcTemplate.query(
            selectAttributeSql,
            rowMapper,
            providerId,
            entityId,
            key
        );
        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getSecond().orElse(null);
    }

    public Map<String, Serializable> findAttributes(String providerId, String entityId) {
        List<Pair<String, Optional<Serializable>>> list = jdbcTemplate.query(
            findAttributesSql,
            rowMapper,
            providerId,
            entityId
        );

        return list
            .stream()
            .filter(p -> p.getSecond().isPresent())
            .collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond().get()));
    }

    public void setAttributes(String providerId, String entityId, Set<Entry<String, Serializable>> attributesSet) {
        jdbcTemplate.update(clearAttributeSql, providerId, entityId);

        for (Entry<String, Serializable> entry : attributesSet) {
            jdbcTemplate.update(
                insertAttributeSql,
                new Object[] {
                    providerId,
                    entityId,
                    entry.getKey(),
                    new SqlLobValue(SerializationUtils.serialize(entry.getValue())),
                },
                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BLOB }
            );
        }
    }

    public void addAttribute(String providerId, String entityId, String key, Serializable value) {
        jdbcTemplate.update(
            insertAttributeSql,
            new Object[] { providerId, entityId, key, new SqlLobValue(SerializationUtils.serialize(value)) },
            new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BLOB }
        );
    }

    public void updateAttribute(String providerId, String entityId, String key, Serializable value) {
        jdbcTemplate.update(updateAttributeSql, value, providerId, entityId, key);
    }

    public void deleteAttribute(String providerId, String entityId, String key) {
        jdbcTemplate.update(deleteAttributeSql, providerId, entityId, key);
    }

    public void clearAttributes(String providerId, String entityId) {
        jdbcTemplate.update(clearAttributeSql, providerId, entityId);
    }

    public void setCreateAttributesSql(String createAttributesSql) {
        this.createAttributesSql = createAttributesSql;
    }

    public void setSelectAttributeSql(String selectAttributeSql) {
        this.selectAttributeSql = selectAttributeSql;
    }

    public void setFindAttributesSql(String findAttributesSql) {
        this.findAttributesSql = findAttributesSql;
    }

    public void setInsertAttributeSql(String insertAttributeSql) {
        this.insertAttributeSql = insertAttributeSql;
    }

    public void setUpdateAttributeSql(String updateAttributeSql) {
        this.updateAttributeSql = updateAttributeSql;
    }

    public void setDeleteAttributeSql(String deleteAttributeSql) {
        this.deleteAttributeSql = deleteAttributeSql;
    }

    public void setClearAttributeSql(String clearAttributeSql) {
        this.clearAttributeSql = clearAttributeSql;
    }

    private static class AttributeRowMapper implements RowMapper<Pair<String, Optional<Serializable>>> {

        @Override
        public Pair<String, Optional<Serializable>> mapRow(ResultSet rs, int rowNum) throws SQLException {
            String key = rs.getString("attr_key");
            Serializable value = SerializationUtils.deserialize(rs.getBytes("attr_value"));
            return Pair.of(key, Optional.ofNullable(value));
        }
    }
}
