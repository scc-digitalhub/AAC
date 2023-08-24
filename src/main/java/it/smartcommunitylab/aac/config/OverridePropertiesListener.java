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
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

public class OverridePropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Properties props = new Properties();
        String jdbcPlatform = environment.getProperty("jdbc.platform");
        if (StringUtils.hasText(jdbcPlatform)) {
            props.put("sql.init.platform", jdbcPlatform);
            props.put("spring.sql.init.schema-locations", "classpath:db/sql/schema-" + jdbcPlatform + ".sql");
            environment.getPropertySources().addFirst(new PropertiesPropertySource("overrideProps", props));
        }
    }
}
