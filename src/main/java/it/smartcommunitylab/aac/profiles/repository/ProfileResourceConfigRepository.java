package it.smartcommunitylab.aac.profiles.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.profiles.provider.ProfileResourceProvider.ProfileResourceProviderConfig;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public class ProfileResourceConfigRepository
        implements ProviderConfigRepository<ProfileResourceProviderConfig> {

    private final String baseUrl;
    private final ProviderConfigRepository<ProfileResourceProviderConfig> repository;

    // TODO replace with a translator on attributeConfigs
    private final AttributeService attributeService;

    public ProfileResourceConfigRepository(AttributeService attributeService,
            ProviderConfigRepository<ProfileResourceProviderConfig> baseRepository,
            String baseUrl) {
        Assert.notNull(attributeService, "attribute service is required");
        Assert.notNull(baseRepository, "base repository can not be null");
        Assert.hasText(baseUrl, "baseUrl can not be null or empty");

        this.baseUrl = baseUrl;
        this.repository = baseRepository;
        this.attributeService = attributeService;
    }

    private ProfileResourceProviderConfig createConfig(String realm) {
        Collection<AttributeSet> sets = attributeService.listAttributeSets(realm);
        Set<String> identifiers = sets.stream().map(s -> s.getIdentifier()).collect(Collectors.toSet());
        ProfileResourceProviderConfig config = buildConfig(realm, identifiers);
        repository.addRegistration(config);
        return config;

    }

    private ProfileResourceProviderConfig buildConfig(String realm, Set<String> identifiers) {
        ProfileResource res = new ProfileResource(realm, baseUrl, identifiers);
        return new ProfileResourceProviderConfig(res);
    }

    private ProfileResourceProviderConfig refreshConfig(ProfileResourceProviderConfig config) {
        String realm = config.getRealm();
        Collection<AttributeSet> sets = attributeService.listAttributeSets(realm);
        Set<String> identifiers = sets.stream().map(s -> s.getIdentifier()).collect(Collectors.toSet());

        if (!identifiers.equals(config.getResource().getIdentifiers())) {
            // update list and version
            int version = config.getVersion() + 1;
            config = buildConfig(realm, identifiers);
            config.setVersion(version);
            repository.addRegistration(config);

        }

        return config;

    }

    @Override
    public ProfileResourceProviderConfig findByProviderId(String providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException();
        }

        ProfileResourceProviderConfig c = repository.findByProviderId(providerId);
        if (c == null) {
            // id should match schema on resource
            // resourceId + SystemKeys.URN_SEPARATOR + realm
            if (!providerId.startsWith(ProfileResource.RESOURCE_ID) || !providerId.contains(SystemKeys.URN_SEPARATOR)) {
                return null;
            }

            String[] s = providerId.split(SystemKeys.URN_SEPARATOR);
            if (s.length != 2) {
                return null;
            }

            // use builder and store if successful
            c = createConfig(s[1]);
        } else {
            // compare config with attribute sets and update when required
            c = refreshConfig(c);
        }

        return c;
    }

    @Override
    public Collection<ProfileResourceProviderConfig> findAll() {
        return repository.findAll().stream().map(c -> refreshConfig(c)).collect(Collectors.toList());
    }

    @Override
    public Collection<ProfileResourceProviderConfig> findByRealm(String realm) {
        // we have one config per realm, so either pick or create
        ProfileResourceProviderConfig c;
        Collection<ProfileResourceProviderConfig> list = repository.findByRealm(realm);
        if (list.isEmpty()) {
            c = createConfig(realm);
        } else {
            c = refreshConfig(list.iterator().next());
        }

        return Collections.singletonList(c);
    }

    @Override
    public void addRegistration(ProfileResourceProviderConfig registration) {
        // nothing to do
    }

    @Override
    public void removeRegistration(String providerId) {
        // nothing to do
    }

    @Override
    public void removeRegistration(ProfileResourceProviderConfig registration) {
        // nothing to do
    }
}
