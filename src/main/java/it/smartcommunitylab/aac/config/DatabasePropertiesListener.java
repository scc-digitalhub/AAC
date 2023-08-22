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
package it.smartcommunitylab.aac.config;

import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

public class DatabasePropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Properties props = new Properties();
        if (StringUtils.isNotEmpty(environment.getProperty("JDBC_PLATFORM"))) {
            props.put("sql.init.platform", environment.getProperty("JDBC_PLATFORM"));
            props.put(
                "spring.sql.init.schema-locations",
                "classpath:db/sql/schema-" + environment.getProperty("JDBC_PLATFORM") + ".sql"
            );
            environment.getPropertySources().addFirst(new PropertiesPropertySource("myProps", props));
        }
    }
}
