package it.smartcommunitylab.aac.config;

import java.util.Collection;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.InMemoryExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ResourceClaimsExtractorProvider;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.scope.InMemoryScopeRegistry;
import it.smartcommunitylab.aac.scope.ScopeProvider;

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

    @Bean(name = "scopeRegistry")
    public InMemoryScopeRegistry scopeRegistry(Collection<ScopeProvider> scopeProviders) {
        return new InMemoryScopeRegistry(scopeProviders);
    }

    @Bean(name = "extractorsRegistry")
    public ExtractorsRegistry extractorsRegistry(Collection<ScopeClaimsExtractorProvider> scopeExtractorsProviders,
            Collection<ResourceClaimsExtractorProvider> resourceExtractorsProviders) {
        return new InMemoryExtractorsRegistry(scopeExtractorsProviders, resourceExtractorsProviders);
    }
}
