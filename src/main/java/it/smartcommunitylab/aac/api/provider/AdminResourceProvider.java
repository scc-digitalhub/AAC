package it.smartcommunitylab.aac.api.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.AdminApiResourceAuthority;
import it.smartcommunitylab.aac.api.scopes.AdminApiResource;
import it.smartcommunitylab.aac.api.scopes.AdminRealmsScope;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.AuthorityInternalScopeProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

public class AdminResourceProvider extends
        AbstractInternalResourceProvider<AdminApiResource, AdminRealmsScope, AbstractClaimDefinition, it.smartcommunitylab.aac.api.provider.AdminResourceProvider.AdminResourceProviderConfig> {

    public AdminResourceProvider(AdminResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected AuthorityInternalScopeProvider<AdminRealmsScope> buildScopeProvider(AdminRealmsScope scope) {
        return new AuthorityInternalScopeProvider<>(scope, false, true);
    }

    public static class AdminResourceProviderConfig
            extends AbstractApiResourceProviderConfig<AdminApiResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + AdminApiResourceAuthority.AUTHORITY;

        public AdminResourceProviderConfig(AdminApiResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }
}