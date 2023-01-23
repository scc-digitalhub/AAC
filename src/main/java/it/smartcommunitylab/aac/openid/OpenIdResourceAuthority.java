package it.smartcommunitylab.aac.openid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.openid.provider.OpenIdResourceProvider;
import it.smartcommunitylab.aac.openid.provider.OpenIdResourceProvider.OpenIdResourceProviderConfig;
import it.smartcommunitylab.aac.openid.scope.OpenIdResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class OpenIdResourceAuthority extends
        AbstractConfigurableProviderAuthority<OpenIdResourceProvider, OpenIdResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, OpenIdResourceProviderConfig>
        implements ApiResourceProviderAuthority<OpenIdResourceProvider, OpenIdResource> {
    public static final String AUTHORITY = OpenIdResource.RESOURCE_ID;

    public OpenIdResourceAuthority(@Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new OpenIdResourceConfigRepository(new InMemoryProviderConfigRepository<>(), baseUrl));
    }

    public OpenIdResourceAuthority(
            ProviderConfigRepository<OpenIdResourceProviderConfig> registrationRepository,
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
    protected OpenIdResourceProvider buildProvider(OpenIdResourceProviderConfig config) {
        return new OpenIdResourceProvider(config);
    }

    private static class OpenIdResourceConfigRepository extends
            InternalApiResourceConfigRepository<OpenIdResource, OpenIdResourceProviderConfig> {

        public OpenIdResourceConfigRepository(ProviderConfigRepository<OpenIdResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, OpenIdResource.RESOURCE_ID,
                    (realm) -> {
                        OpenIdResource res = new OpenIdResource(realm, baseUrl);
                        return new OpenIdResourceProviderConfig(res);
                    });
        }
    }
}
