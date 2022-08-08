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
    public ApplicationProperties applicationPrps() {
        return new ApplicationProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "authorities")
    public AuthoritiesProperties authoritiesProps() {
        return new AuthoritiesProperties();
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

    @Bean
    @ConfigurationProperties(prefix = "spid")
    public SpidProperties spidProperties() {
        return new SpidProperties();
    }
}
