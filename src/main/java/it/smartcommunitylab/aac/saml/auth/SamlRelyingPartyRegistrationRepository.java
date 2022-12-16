package it.smartcommunitylab.aac.saml.auth;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;

public class SamlRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    // provider configs by id
    private final ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository;

    public SamlRelyingPartyRegistrationRepository(
            ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository) {
        Assert.notNull(registrationRepository, "provider registration repository can not be null");
        this.registrationRepository = registrationRepository;
    }

    /*
     * read access as per interface
     */

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id cannot be empty");

        // fetch provider registration with matching id
        SamlIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(registrationId);
        if (providerConfig == null) {
            return null;
        }

        // build
        // TODO registrationId loading cache
        return providerConfig.getRelyingPartyRegistration();
    }

}
