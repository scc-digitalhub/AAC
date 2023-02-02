package it.smartcommunitylab.aac.openid.auth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;

public class OIDCClientRegistrationRepository implements ClientRegistrationRepository {

    // provider configs by id
    private final ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository;

    public OIDCClientRegistrationRepository(
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository) {
        Assert.notNull(registrationRepository, "provider registration repository can not be null");
        this.registrationRepository = registrationRepository;
    }

    /*
     * read access as per interface
     */

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id cannot be empty");

        // fetch provider registration with matching id
        OIDCIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(registrationId);
        if (providerConfig == null) {
            return null;
        }

        // build
        // TODO registrationId loading cache
        return providerConfig.getClientRegistration();
    }

}
