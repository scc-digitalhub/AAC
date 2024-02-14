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
}
