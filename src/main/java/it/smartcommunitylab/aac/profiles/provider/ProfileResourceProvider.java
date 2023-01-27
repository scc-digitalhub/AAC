package it.smartcommunitylab.aac.profiles.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.model.SerializableClaimDefinition;
import it.smartcommunitylab.aac.profiles.scope.AbstractProfileScope;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.scope.provider.SubjectTypeInternalScopeProvider;

public class ProfileResourceProvider extends
        AbstractInternalResourceProvider<ProfileResource, AbstractProfileScope, SerializableClaimDefinition, it.smartcommunitylab.aac.profiles.provider.ProfileResourceProvider.ProfileResourceProviderConfig> {

    public ProfileResourceProvider(ProfileResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected SubjectTypeInternalScopeProvider<AbstractProfileScope> buildScopeProvider(AbstractProfileScope scope) {
        return new SubjectTypeInternalScopeProvider<>(scope);
    }

    @Override
    protected ProfileClaimsSetExtractor buildClaimsExtractor(ProfileResource resource) {
        return new ProfileClaimsSetExtractor(resource);
    }

    public static class ProfileResourceProviderConfig
            extends AbstractApiResourceProviderConfig<ProfileResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + ProfileResource.AUTHORITY;

        public ProfileResourceProviderConfig(ProfileResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }
}