package it.smartcommunitylab.aac.roles;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.roles.provider.RolesResourceProvider;
import it.smartcommunitylab.aac.roles.provider.RolesResourceProvider.RolesResourceProviderConfig;
import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class RolesResourceAuthority extends
        AbstractConfigurableProviderAuthority<RolesResourceProvider, RolesResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, RolesResourceProviderConfig>
        implements ApiResourceProviderAuthority<RolesResourceProvider, RolesResource> {
    public static final String AUTHORITY = RolesResource.RESOURCE_ID;

    public RolesResourceAuthority(@Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new RolesResourceConfigRepository(new InMemoryProviderConfigRepository<>(), baseUrl));
    }

    public RolesResourceAuthority(
            ProviderConfigRepository<RolesResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new RolesResourceConfigRepository(registrationRepository, baseUrl));
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
    protected RolesResourceProvider buildProvider(RolesResourceProviderConfig config) {
        return new RolesResourceProvider(config);
    }

    private static class RolesResourceConfigRepository extends
            InternalApiResourceConfigRepository<RolesResource, RolesResourceProviderConfig> {

        public RolesResourceConfigRepository(ProviderConfigRepository<RolesResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, RolesResource.RESOURCE_ID,
                    (realm) -> {
                        RolesResource res = new RolesResource(realm, baseUrl);
                        return new RolesResourceProviderConfig(res);
                    });
        }
    }
}
