package it.smartcommunitylab.aac.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResource;
import it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResourceProvider;
import it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResourceProvider.OAuth2DCRResourceProviderConfig;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceConfigRepository;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class OAuth2DCRResourceAuthority extends
        AbstractConfigurableProviderAuthority<OAuth2DCRResourceProvider, OAuth2DCRResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, OAuth2DCRResourceProviderConfig>
        implements ApiResourceProviderAuthority<OAuth2DCRResourceProvider, OAuth2DCRResource> {
    public static final String AUTHORITY = OAuth2DCRResource.RESOURCE_ID;

    public OAuth2DCRResourceAuthority(@Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new OAuth2DCRResourceConfigRepository(new InMemoryProviderConfigRepository<>(), baseUrl));
    }

    public OAuth2DCRResourceAuthority(
            ProviderConfigRepository<OAuth2DCRResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new OAuth2DCRResourceConfigRepository(registrationRepository, baseUrl));
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
    protected OAuth2DCRResourceProvider buildProvider(OAuth2DCRResourceProviderConfig config) {
        return new OAuth2DCRResourceProvider(config);
    }

    private static class OAuth2DCRResourceConfigRepository extends
            InternalApiResourceConfigRepository<OAuth2DCRResource, OAuth2DCRResourceProviderConfig> {

        public OAuth2DCRResourceConfigRepository(
                ProviderConfigRepository<OAuth2DCRResourceProviderConfig> baseRepository,
                String baseUrl) {
            super(baseRepository, OAuth2DCRResource.RESOURCE_ID,
                    (realm) -> {
                        OAuth2DCRResource res = new OAuth2DCRResource(realm, baseUrl);
                        return new OAuth2DCRResourceProviderConfig(res);
                    });
        }
    }
}
