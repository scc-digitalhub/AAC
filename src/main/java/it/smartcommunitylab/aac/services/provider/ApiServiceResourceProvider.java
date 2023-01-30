package it.smartcommunitylab.aac.services.provider;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.services.model.ApiService;
import it.smartcommunitylab.aac.services.model.ApiServiceScope;

public class ApiServiceResourceProvider
        extends
        AbstractResourceProvider<ApiService, ApiServiceScope, InternalApiResourceProviderConfigMap, it.smartcommunitylab.aac.services.provider.ApiServiceResourceProviderConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, ApiServiceScopeProvider> providers;
    private ApiServiceClaimsSetExtractor extractor;

    public ApiServiceResourceProvider(
            ScriptExecutionService executionService, SearchableApprovalStore approvalStore,
            ApiServiceResourceProviderConfig config) {
        super(SystemKeys.AUTHORITY_SERVICE, config.getProvider(), config.getRealm(), config);
        logger.debug("build resource provider for {}", config.getProvider());
        if (logger.isTraceEnabled()) {
            logger.trace("resource: {}", String.valueOf(config.getResource()));
        }

        // build providers
        // build all providers eagerly, internal resources are static
        providers = resource.getScopes().stream()
                .map(s -> new ApiServiceScopeProvider(s, executionService, approvalStore))
                .collect(Collectors.toMap(p -> p.getScope().getScope(), p -> p));

        extractor = new ApiServiceClaimsSetExtractor(resource, executionService);
    }

    @Override
    public ApiServiceScopeProvider getScopeProvider(String scope) throws NoSuchScopeException {
        ApiServiceScopeProvider sp = providers.get(scope);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

    @Override
    public ApiServiceClaimsSetExtractor getClaimsExtractor() {
        return extractor;
    }

}