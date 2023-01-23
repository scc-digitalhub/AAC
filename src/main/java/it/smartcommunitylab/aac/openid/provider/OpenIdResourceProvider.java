package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.openid.scope.OpenIdResource;
import it.smartcommunitylab.aac.openid.scope.OpenIdResource.AbstractOpenIdScope;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.scope.provider.SubjectTypeInternalScopeProvider;

public class OpenIdResourceProvider extends
        AbstractInternalResourceProvider<OpenIdResource, AbstractOpenIdScope, AbstractClaimDefinition, it.smartcommunitylab.aac.openid.provider.OpenIdResourceProvider.OpenIdResourceProviderConfig> {

    public OpenIdResourceProvider(OpenIdResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected SubjectTypeInternalScopeProvider<AbstractOpenIdScope> buildScopeProvider(AbstractOpenIdScope scope) {
        return new SubjectTypeInternalScopeProvider<>(scope);
    }

    public static class OpenIdResourceProviderConfig
            extends AbstractApiResourceProviderConfig<OpenIdResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + OpenIdResource.AUTHORITY;

        public OpenIdResourceProviderConfig(OpenIdResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }

}