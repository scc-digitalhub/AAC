package it.smartcommunitylab.aac.profiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.profiles.repository.ProfileResourceConfigRepository;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;
import it.smartcommunitylab.aac.profiles.scope.ProfileResourceProvider;
import it.smartcommunitylab.aac.profiles.scope.ProfileResourceProvider.ProfileResourceProviderConfig;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

@Service
public class ProfileResourceAuthority extends
        AbstractConfigurableProviderAuthority<ProfileResourceProvider, ProfileResource, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, ProfileResourceProviderConfig>
        implements ApiResourceProviderAuthority<ProfileResourceProvider, ProfileResource> {
    public static final String AUTHORITY = ProfileResource.RESOURCE_ID;

    @Autowired
    public ProfileResourceAuthority(AttributeService attributeService, @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new ProfileResourceConfigRepository(attributeService, new InMemoryProviderConfigRepository<>(),
                baseUrl));
    }

    public ProfileResourceAuthority(
            AttributeService attributeService,
            ProviderConfigRepository<ProfileResourceProviderConfig> registrationRepository,
            @Value("${application.url}") String baseUrl) {
        super(AUTHORITY, new ProfileResourceConfigRepository(attributeService, registrationRepository, baseUrl));
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
    protected ProfileResourceProvider buildProvider(ProfileResourceProviderConfig config) {
        return new ProfileResourceProvider(config);
    }
}
