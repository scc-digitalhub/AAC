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
