package it.smartcommunitylab.aac.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.provider.AdminResourceProvider;
import it.smartcommunitylab.aac.api.provider.AdminResourceProvider.AdminResourceProviderConfig;
import it.smartcommunitylab.aac.api.scopes.AdminApiResource;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class AdminApiResourceAuthority
        extends
        AbstractConfigurableProviderAuthority<AdminResourceProvider, AdminApiResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, AdminResourceProviderConfig>
        implements ApiResourceProviderAuthority<AdminResourceProvider, AdminApiResource> {
    public static final String AUTHORITY = AdminApiResource.RESOURCE_ID;

    public AdminApiResourceAuthority(
            ProviderConfigRepository<AdminResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new AdminResourceConfigRepository(registrationRepository, baseUrl));
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
    protected AdminResourceProvider buildProvider(AdminResourceProviderConfig config) {
        return new AdminResourceProvider(config);
    }

    private static class AdminResourceConfigRepository extends
            InternalApiResourceConfigRepository<AdminApiResource, AdminResourceProviderConfig> {

        public AdminResourceConfigRepository(ProviderConfigRepository<AdminResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, AdminApiResource.RESOURCE_ID,
                    (realm) -> {
                        AdminApiResource res = new AdminApiResource(realm, baseUrl);
                        return new AdminResourceProviderConfig(res);
                    });
        }
    }
}
