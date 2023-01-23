package it.smartcommunitylab.aac.groups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.groups.provider.GroupsResourceProvider;
import it.smartcommunitylab.aac.groups.provider.GroupsResourceProvider.GroupsResourceProviderConfig;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class GroupsResourceAuthority extends
        AbstractConfigurableProviderAuthority<GroupsResourceProvider, GroupsResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, GroupsResourceProviderConfig>
        implements ApiResourceProviderAuthority<GroupsResourceProvider, GroupsResource> {
    public static final String AUTHORITY = GroupsResource.RESOURCE_ID;

    @Autowired
    public GroupsResourceAuthority(@Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new GroupsResourceConfigRepository(new InMemoryProviderConfigRepository<>(), baseUrl));
    }

    public GroupsResourceAuthority(
            ProviderConfigRepository<GroupsResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new GroupsResourceConfigRepository(registrationRepository, baseUrl));
    }

    @Override
    public String getAuthorityId() {
        return AUTHORITY;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_API_RESOURCE;
    }

    @Override
    protected GroupsResourceProvider buildProvider(GroupsResourceProviderConfig config) {
        return new GroupsResourceProvider(config);
    }

    private static class GroupsResourceConfigRepository extends
            InternalApiResourceConfigRepository<GroupsResource, GroupsResourceProviderConfig> {

        public GroupsResourceConfigRepository(ProviderConfigRepository<GroupsResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, GroupsResource.RESOURCE_ID,
                    (realm) -> {
                        GroupsResource res = new GroupsResource(realm, baseUrl);
                        return new GroupsResourceProviderConfig(res);
                    });
        }
    }
}
