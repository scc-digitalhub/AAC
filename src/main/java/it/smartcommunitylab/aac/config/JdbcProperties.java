/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.config;

import java.util.Map;
import java.util.Objects;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

public class JdbcProperties {

    //use platform placeholder by default, let database initializer resolve actual schema
    private static final String DEFAULT_SCHEMA_LOCATION = "classpath:db/sql/schema-@@platform@@.sql";

    private String platform;
    private String schema = DEFAULT_SCHEMA_LOCATION;

    private String driver;
    private String dialect;
    private String url;
    private String user;
    private String password;

    private boolean showSql;

    private int maxPoolSize;
    private int minPoolSize;
    private int idleTimeout;
    private int keepAliveTimeout;
    private int connectionTimeout;

    private Map<String, Object> dataSourceProperties;

    private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isShowSql() {
        return showSql;
    }

    public void setShowSql(boolean showSql) {
        this.showSql = showSql;
    }

    public DatabaseInitializationMode getInitializeSchema() {
        return initializeSchema;
    }

    public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
        this.initializeSchema = initializeSchema;
    }

    public static String getDefaultSchemaLocation() {
        return DEFAULT_SCHEMA_LOCATION;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Map<String, Object> getDataSourceProperties() {
        return dataSourceProperties;
    }

    public void setDataSourceProperties(Map<String, Object> dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JdbcProperties that = (JdbcProperties) obj;
        return (
            showSql == that.showSql &&
            maxPoolSize == that.maxPoolSize &&
            minPoolSize == that.minPoolSize &&
            idleTimeout == that.idleTimeout &&
            keepAliveTimeout == that.keepAliveTimeout &&
            connectionTimeout == that.connectionTimeout &&
            Objects.equals(driver, that.driver) &&
            Objects.equals(dialect, that.dialect) &&
            Objects.equals(url, that.url) &&
            Objects.equals(user, that.user) &&
            Objects.equals(password, that.password)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            driver,
            dialect,
            url,
            user,
            password,
            showSql,
            maxPoolSize,
            minPoolSize,
            idleTimeout,
            keepAliveTimeout,
            connectionTimeout
        );
    }
}
