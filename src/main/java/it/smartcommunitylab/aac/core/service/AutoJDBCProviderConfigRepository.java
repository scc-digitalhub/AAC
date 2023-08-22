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

package it.smartcommunitylab.aac.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.security.oauth2.common.util.DefaultSerializationStrategy;
import org.springframework.security.oauth2.common.util.SerializationStrategy;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.common.util.WhitelistedSerializationStrategy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class AutoJDBCProviderConfigRepository<U extends AbstractProviderConfig<?, ?>>
    implements ProviderConfigRepository<U> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String DEFAULT_CREATE_TABLE_STATEMENT =
        "CREATE TABLE IF NOT EXISTS provider_config (type VARCHAR(256), provider_id VARCHAR(256),  realm VARCHAR(256), config BLOB, PRIMARY KEY (provider_id, type));";
    private static final String DEFAULT_SELECT_STATEMENT =
        "select config from provider_config where provider_id = ? and type = ?";
    private static final String DEFAULT_FIND_ALL_STATEMENT = "select config from provider_config where type = ?";
    private static final String DEFAULT_FIND_BY_REALM_STATEMENT =
        "select config from provider_config where type = ? and realm = ?";
    private static final String DEFAULT_INSERT_STATEMENT =
        "insert into provider_config (type, provider_id, realm, config) values (?, ?, ?, ?)";
    private static final String DEFAULT_UPDATE_STATEMENT =
        "update provider_config set config = ?  where provider_id = ? and type = ?";
    private static final String DEFAULT_DELETE_STATEMENT =
        "delete from provider_config where provider_id = ? and type = ?";
    private static final String DEFAULT_CLEAR_STATEMENT = "delete from provider_config where type = ?";

    private String createTableSql = DEFAULT_CREATE_TABLE_STATEMENT;
    private String selectSql = DEFAULT_SELECT_STATEMENT;
    private String findAllSql = DEFAULT_FIND_ALL_STATEMENT;
    private String findByRealmSql = DEFAULT_FIND_BY_REALM_STATEMENT;
    private String insertSql = DEFAULT_INSERT_STATEMENT;
    private String updateSql = DEFAULT_UPDATE_STATEMENT;
    private String deleteSql = DEFAULT_DELETE_STATEMENT;
    private String clearSql = DEFAULT_CLEAR_STATEMENT;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<U> rowMapper;
    //    private final SerializationStrategy serializer;
    private final ObjectMapper mapper;
    private final String type;

    public AutoJDBCProviderConfigRepository(DataSource dataSource, Class<U> className) {
        Assert.notNull(className, "please provide a valid class for serializer");
        Assert.notNull(dataSource, "DataSource required");

        this.type = className.getName();
        logger.debug("create jdbc repository for provider config {}", type);

        // DISABLED
        // whitelisting requires *all* classes embedded whitelisted
        // also serializer requires the correct classloader (breaks with devtools)
        // TODO add resolver or switch to jackson converter
        //        // also whitelist superclass and base classes
        //        List<String> classes = new ArrayList<>();
        //        classes.add(type);
        //        classes.add(className.getSuperclass().getName());
        //        classes.add(AbstractProviderConfig.class.getName());
        //        logger.trace("jdbc repository for provider config {} allowed classes {}", type, classes);
        //
        //        serializer = new WhitelistedSerializationStrategy(classes);

        // DISABLED, breaks with devTools + multithreading due to classLoader issues
        //        // use default with classloader
        //        serializer = SerializationUtils.getSerializationStrategy();
        //        rowMapper = new ConfigRowMapper(serializer);

        CBORMapper cborMapper = new CBORMapper();
        this.rowMapper = new CBORConfigRowMapper(cborMapper, className);
        this.mapper = cborMapper;

        //        ObjectMapper jsonMapper = new ObjectMapper();
        //        this.rowMapper = new JsonConfigRowMapper(jsonMapper, className);
        //        this.mapper = jsonMapper;

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initSchema();
        // TODO make sure table does not contain old/stale registrations on startup
        // should support multi-instance
    }

    protected void initSchema() {
        //        jdbcTemplate.execute(createTableSql);
    }

    public void clear() {
        jdbcTemplate.update(clearSql, type);
    }

    @Override
    public U findByProviderId(String providerId) {
        logger.trace("find registration for provider {} with id {}", type, providerId);
        try {
            return jdbcTemplate.queryForObject(selectSql, rowMapper, providerId, type);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<U> findAll() {
        List<U> list = jdbcTemplate.query(findAllSql, rowMapper, type);

        return list.stream().filter(p -> p != null).collect(Collectors.toList());
    }

    @Override
    public Collection<U> findByRealm(String realm) {
        List<U> list = jdbcTemplate.query(findByRealmSql, rowMapper, type, realm);

        return list.stream().filter(p -> p != null).collect(Collectors.toList());
    }

    @Override
    public void addRegistration(U registration) {
        if (registration != null) {
            String providerId = registration.getProvider();
            if (StringUtils.hasText(providerId)) {
                logger.trace("add registration for provider {} with id {}", type, providerId);
                try {
                    // either insert or update
                    U c = findByProviderId(providerId);
                    //                    String json = mapper.writeValueAsString(registration);
                    //
                    //                    if (c == null) {
                    //                        logger.trace("insert registration for provider {} with id {}", type, providerId);
                    //
                    //                        String realm = registration.getRealm();
                    //                        jdbcTemplate.update(insertSql,
                    //                                new Object[] { type, providerId, realm, json },
                    //                                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });
                    //                    } else {
                    //                        logger.trace("update registration for provider {} with id {}", type, providerId);
                    //
                    //                        jdbcTemplate.update(updateSql,
                    //                                new Object[] { json, providerId, type },
                    //                                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });
                    //                    }

                    byte[] bytes = mapper.writeValueAsBytes(registration);

                    SqlLobValue lob = new SqlLobValue(bytes);
                    if (c == null) {
                        logger.trace("insert registration for provider {} with id {}", type, providerId);

                        String realm = registration.getRealm();
                        jdbcTemplate.update(
                            insertSql,
                            new Object[] { type, providerId, realm, lob },
                            new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BLOB }
                        );
                    } else {
                        logger.trace("update registration for provider {} with id {}", type, providerId);

                        jdbcTemplate.update(
                            updateSql,
                            new Object[] { lob, providerId, type },
                            new int[] { Types.BLOB, Types.VARCHAR, Types.VARCHAR }
                        );
                    }
                } catch (JsonProcessingException e) {
                    logger.error("error converting registration {} provider {}: {}", type, providerId, e.getMessage());
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    @Override
    public void removeRegistration(String providerId) {
        jdbcTemplate.update(deleteSql, providerId, type);
    }

    @Override
    public void removeRegistration(U registration) {
        if (registration != null) {
            String providerId = registration.getProvider();
            if (StringUtils.hasText(providerId)) {
                jdbcTemplate.update(deleteSql, providerId, type);
            }
        }
    }

    //    private class JsonConfigRowMapper implements RowMapper<U> {
    //        private final ObjectMapper mapper;
    //        private final Class<U> type;
    //
    //        public JsonConfigRowMapper(ObjectMapper mapper, Class<U> type) {
    //            Assert.notNull(mapper, "mapper required");
    //            this.mapper = mapper;
    //            this.type = type;
    //        }
    //
    //        @Override
    //        public U mapRow(ResultSet rs, int rowNum) throws SQLException {
    //            String value = rs.getString("config");
    //            if (value == null || value.length() == 0) {
    //                return null;
    //            }
    //
    //            try {
    //                U u = mapper.readValue(value, type);
    //                return u;
    //            } catch (IOException e) {
    //                throw new SQLException();
    //            }
    //        }
    //
    //    }

    private class CBORConfigRowMapper implements RowMapper<U> {

        private final CBORMapper mapper;
        private final Class<U> type;

        public CBORConfigRowMapper(CBORMapper mapper, Class<U> type) {
            Assert.notNull(mapper, "mapper required");
            this.mapper = mapper;
            this.type = type;
        }

        @Override
        public U mapRow(ResultSet rs, int rowNum) throws SQLException {
            byte[] bytes = rs.getBytes("config");
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            try {
                U u = mapper.readValue(bytes, type);
                return u;
            } catch (IOException e) {
                logger.error("error reading config for {}: {}", type, e.getMessage());
                throw new SQLException();
            }
        }
    }
    //    private class ConfigRowMapper implements RowMapper<U> {
    //        private final SerializationStrategy serializer;
    //
    //        public ConfigRowMapper(SerializationStrategy serializer) {
    //            Assert.notNull(serializer, "serializer required");
    //            this.serializer = serializer;
    //        }
    //
    //        @Override
    //        public U mapRow(ResultSet rs, int rowNum) throws SQLException {
    //            byte[] bytes = rs.getBytes("config");
    //            if (bytes == null || bytes.length == 0) {
    //                return null;
    //            }
    //
    //            Object o = serializer.deserialize(bytes);
    //            U u = (U) o;
    //
    //            return u;
    //        }
    //
    //    }
}
