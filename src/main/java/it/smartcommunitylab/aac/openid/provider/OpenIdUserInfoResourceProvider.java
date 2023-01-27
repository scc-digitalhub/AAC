package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource.AbstractOpenIdUserInfoScope;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.scope.provider.SubjectTypeInternalScopeProvider;

public class OpenIdUserInfoResourceProvider extends
        AbstractInternalResourceProvider<OpenIdUserInfoResource, AbstractOpenIdUserInfoScope, AbstractClaimDefinition, it.smartcommunitylab.aac.openid.provider.OpenIdUserInfoResourceProvider.OpenIdUserInfoResourceProviderConfig> {

    public OpenIdUserInfoResourceProvider(OpenIdUserInfoResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected SubjectTypeInternalScopeProvider<AbstractOpenIdUserInfoScope> buildScopeProvider(
            AbstractOpenIdUserInfoScope scope) {
        return new SubjectTypeInternalScopeProvider<>(scope);
    }

    @Override
    protected OpenIdUserInfoClaimsSetExtractor buildClaimsExtractor(OpenIdUserInfoResource resource) {
        return new OpenIdUserInfoClaimsSetExtractor(resource);
    }

    public static class OpenIdUserInfoResourceProviderConfig
            extends AbstractApiResourceProviderConfig<OpenIdUserInfoResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + OpenIdUserInfoResource.AUTHORITY;

        public OpenIdUserInfoResourceProviderConfig(OpenIdUserInfoResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }

}