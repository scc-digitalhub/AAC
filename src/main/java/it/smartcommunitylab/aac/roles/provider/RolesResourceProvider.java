package it.smartcommunitylab.aac.roles.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.roles.scopes.RolesResource.AbstractRolesScope;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.scope.provider.SubjectTypeInternalScopeProvider;

public class RolesResourceProvider extends
        AbstractInternalResourceProvider<RolesResource, AbstractRolesScope, AbstractClaimDefinition, it.smartcommunitylab.aac.roles.provider.RolesResourceProvider.RolesResourceProviderConfig> {

    public RolesResourceProvider(RolesResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected SubjectTypeInternalScopeProvider<AbstractRolesScope> buildScopeProvider(AbstractRolesScope scope) {
        return new SubjectTypeInternalScopeProvider<>(scope);
    }

    @Override
    protected RolesClaimsExtractor buildClaimsExtractor(RolesResource resource) {
        return new RolesClaimsExtractor(resource);
    }

    public static class RolesResourceProviderConfig
            extends AbstractApiResourceProviderConfig<RolesResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + RolesResource.AUTHORITY;

        public RolesResourceProviderConfig(RolesResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }

}