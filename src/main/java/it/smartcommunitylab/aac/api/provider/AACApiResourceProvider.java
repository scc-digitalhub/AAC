package it.smartcommunitylab.aac.api.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.AACApiResourceAuthority;
import it.smartcommunitylab.aac.api.scopes.AACApiResource;
import it.smartcommunitylab.aac.api.scopes.AACApiResource.AbstractAACApiScope;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.AuthorityInternalScopeProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

public class AACApiResourceProvider
        extends
        AbstractInternalResourceProvider<AACApiResource, AbstractAACApiScope, AbstractClaimDefinition, it.smartcommunitylab.aac.api.provider.AACApiResourceProvider.AACApiResourceProviderConfig> {

    public AACApiResourceProvider(AACApiResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected AuthorityInternalScopeProvider<AbstractAACApiScope> buildScopeProvider(AbstractAACApiScope scope) {
        return new AuthorityInternalScopeProvider<>(scope);
    }

    public static class AACApiResourceProviderConfig
            extends AbstractApiResourceProviderConfig<AACApiResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + AACApiResourceAuthority.AUTHORITY;

        public AACApiResourceProviderConfig(AACApiResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }

}