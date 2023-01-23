package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.AuthorityInternalScopeProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

public class OAuth2DCRResourceProvider extends

        AbstractInternalResourceProvider<OAuth2DCRResource, OAuth2DCRScope, AbstractClaimDefinition, it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResourceProvider.OAuth2DCRResourceProviderConfig> {

    public OAuth2DCRResourceProvider(OAuth2DCRResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected AuthorityInternalScopeProvider<OAuth2DCRScope> buildScopeProvider(OAuth2DCRScope scope) {
        return new AuthorityInternalScopeProvider<>(scope);
    }

    public static class OAuth2DCRResourceProviderConfig
            extends AbstractApiResourceProviderConfig<OAuth2DCRResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + OAuth2DCRResource.AUTHORITY;

        public OAuth2DCRResourceProviderConfig(OAuth2DCRResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }
}