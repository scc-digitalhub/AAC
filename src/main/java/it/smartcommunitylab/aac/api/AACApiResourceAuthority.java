package it.smartcommunitylab.aac.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.provider.AACApiResourceProvider;
import it.smartcommunitylab.aac.api.provider.AACApiResourceProviderConfig;
import it.smartcommunitylab.aac.api.scopes.AACApiResource;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class AACApiResourceAuthority
        extends
        AbstractConfigurableProviderAuthority<AACApiResourceProvider, AACApiResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, AACApiResourceProviderConfig>
        implements ApiResourceProviderAuthority<AACApiResourceProvider, AACApiResource> {
    public static final String AUTHORITY = "aac.api";

    public AACApiResourceAuthority(
            ProviderConfigRepository<AACApiResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new AACApiResourceConfigRepository(registrationRepository, baseUrl));
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
    protected AACApiResourceProvider buildProvider(AACApiResourceProviderConfig config) {
        return new AACApiResourceProvider(config);
    }

    private static class AACApiResourceConfigRepository extends
            InternalApiResourceConfigRepository<AACApiResource, AACApiResourceProviderConfig> {

        public AACApiResourceConfigRepository(ProviderConfigRepository<AACApiResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, AACApiResource.RESOURCE_ID,
                    (realm) -> {
                        AACApiResource res = new AACApiResource(realm, baseUrl);
                        return new AACApiResourceProviderConfig(res);
                    });
        }
    }
}
