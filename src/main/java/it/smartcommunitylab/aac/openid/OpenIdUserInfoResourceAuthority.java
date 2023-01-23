package it.smartcommunitylab.aac.openid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.openid.provider.OpenIdUserInfoResourceProvider;
import it.smartcommunitylab.aac.openid.provider.OpenIdUserInfoResourceProvider.OpenIdUserInfoResourceProviderConfig;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class OpenIdUserInfoResourceAuthority extends
        AbstractConfigurableProviderAuthority<OpenIdUserInfoResourceProvider, OpenIdUserInfoResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, OpenIdUserInfoResourceProviderConfig>
        implements ApiResourceProviderAuthority<OpenIdUserInfoResourceProvider, OpenIdUserInfoResource> {
    public static final String AUTHORITY = OpenIdUserInfoResource.RESOURCE_ID;

    public OpenIdUserInfoResourceAuthority(@Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new OpenIdResourceConfigRepository(new InMemoryProviderConfigRepository<>(), baseUrl));
    }

    public OpenIdUserInfoResourceAuthority(
            ProviderConfigRepository<OpenIdUserInfoResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new OpenIdResourceConfigRepository(registrationRepository, baseUrl));
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
    protected OpenIdUserInfoResourceProvider buildProvider(OpenIdUserInfoResourceProviderConfig config) {
        return new OpenIdUserInfoResourceProvider(config);
    }

    private static class OpenIdResourceConfigRepository extends
            InternalApiResourceConfigRepository<OpenIdUserInfoResource, OpenIdUserInfoResourceProviderConfig> {

        public OpenIdResourceConfigRepository(
                ProviderConfigRepository<OpenIdUserInfoResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, OpenIdUserInfoResource.RESOURCE_ID,
                    (realm) -> {
                        OpenIdUserInfoResource res = new OpenIdUserInfoResource(realm, baseUrl);
                        return new OpenIdUserInfoResourceProviderConfig(res);
                    });
        }
    }
}
