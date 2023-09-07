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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(0)
public class PropertiesConfig {

    @Bean
    @ConfigurationProperties(prefix = "application")
    public ApplicationProperties applicationProps() {
        return new ApplicationProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "jdbc")
    public JdbcProperties jdbcProps() {
        return new JdbcProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "authorities.identity")
    public IdentityAuthoritiesProperties identityAuthoritiesProps() {
        return new IdentityAuthoritiesProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "authorities.account")
    public AccountAuthoritiesProperties accountAuthoritiesProps() {
        return new AccountAuthoritiesProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "authorities.credentials")
    public CredentialsAuthoritiesProperties credentialsAuthoritiesProps() {
        return new CredentialsAuthoritiesProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "providers")
    public ProvidersProperties globalProviders() {
        return new ProvidersProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "attributesets")
    public AttributeSetsProperties systemAttributeSets() {
        return new AttributeSetsProperties();
    }
}
