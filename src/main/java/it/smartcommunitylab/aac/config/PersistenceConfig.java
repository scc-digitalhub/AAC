package it.smartcommunitylab.aac.config;

import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.InMemoryExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ResourceClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.scope.InMemoryScopeRegistry;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Configuration
@Order(4)
public class PersistenceConfig {

    @Autowired
    private DataSource dataSource;

    /*
     * Wire persistence services bound to dataSource
     */

    @Bean
    public AutoJdbcAttributeStore attributeStore() {
        return new AutoJdbcAttributeStore(dataSource);
    }

    @Bean
    public ScopeRegistry scopeRegistry(Collection<ScopeProvider> scopeProviders) {
        return new InMemoryScopeRegistry(scopeProviders);
    }

    @Bean
    public ExtractorsRegistry scopeRegistry(Collection<ScopeClaimsExtractor> scopeExtractors,
            Collection<ResourceClaimsExtractor> resourceExtractors) {
        return new InMemoryExtractorsRegistry(scopeExtractors, resourceExtractors);
    }
}
