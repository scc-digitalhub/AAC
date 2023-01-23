package it.smartcommunitylab.aac.groups.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource.AbstractGroupsScope;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.scope.provider.SubjectTypeInternalScopeProvider;

public class GroupsResourceProvider extends
        AbstractInternalResourceProvider<GroupsResource, AbstractGroupsScope, AbstractClaimDefinition, it.smartcommunitylab.aac.groups.provider.GroupsResourceProvider.GroupsResourceProviderConfig> {

    public GroupsResourceProvider(GroupsResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected SubjectTypeInternalScopeProvider<AbstractGroupsScope> buildScopeProvider(AbstractGroupsScope scope) {
        return new SubjectTypeInternalScopeProvider<>(scope);
    }

    @Override
    protected GroupsClaimsExtractor buildClaimsExtractor(GroupsResource resource) {
        return new GroupsClaimsExtractor(resource);
    }

    public static class GroupsResourceProviderConfig
            extends AbstractApiResourceProviderConfig<GroupsResource, InternalApiResourceProviderConfigMap> {
        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
        public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
                + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + GroupsResource.AUTHORITY;

        public GroupsResourceProviderConfig(GroupsResource res) {
            super(res, new InternalApiResourceProviderConfigMap());
        }
    }

}