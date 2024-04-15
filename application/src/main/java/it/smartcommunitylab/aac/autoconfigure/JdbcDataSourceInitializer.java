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

package it.smartcommunitylab.aac.autoconfigure;

import it.smartcommunitylab.aac.config.JdbcProperties;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PlatformPlaceholderDatabaseDriverResolver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * Initialize database with auto-config based on platform and sql scripts
 */
public class JdbcDataSourceInitializer extends DataSourceScriptDatabaseInitializer {

    public JdbcDataSourceInitializer(DataSource dataSource, JdbcProperties properties) {
        this(dataSource, properties, properties.getSchema());
    }

    public JdbcDataSourceInitializer(DataSource dataSource, JdbcProperties properties, String schema) {
        super(dataSource, extractSettings(dataSource, properties, schema));
    }

    static DatabaseInitializationSettings extractSettings(
        DataSource dataSource,
        JdbcProperties properties,
        String schema
    ) {
        Assert.hasText(schema, "schema can not be null or empty");
        //create settings by resolving schema
        //supports external schemas for additional platforms
        DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setSchemaLocations(resolveSchemaLocations(dataSource, properties.getPlatform(), schema));
        settings.setMode(properties.getInitializeSchema());

        //set continue to execute the script on errors
        //note: tables may already exist, but we apply the script every time
        settings.setContinueOnError(true);
        return settings;
    }

    private static List<String> resolveSchemaLocations(DataSource dataSource, String platform, String schema) {
        PlatformPlaceholderDatabaseDriverResolver platformResolver = new PlatformPlaceholderDatabaseDriverResolver();
        //if platform is set explicitly use it
        if (StringUtils.hasText(platform)) {
            return platformResolver.resolveAll(platform, schema);
        }

        //let resolver infer schema
        return platformResolver.resolveAll(dataSource, schema);
    }
}
