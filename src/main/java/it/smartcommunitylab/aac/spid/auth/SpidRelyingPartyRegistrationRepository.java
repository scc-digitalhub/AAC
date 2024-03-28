package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.util.Optional;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.Assert;

public class SpidRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository;

    public SpidRelyingPartyRegistrationRepository(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(registrationRepository, "provider registration repository can not be null");
        this.registrationRepository = registrationRepository;
    }

    /*
     * read access as per interface
     */
    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id can not be empty");
        SpidIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(registrationId);
        if (providerConfig == null) {
            return null;
        }
        // TODO: problema concettuale: un provider SPID è associato ad N registrazioni.
        //  Non ho la minima idea se questa soluzione sia idonea o meno, perché non so se
        //  la registrationId in argomento corrisponde effettivamente alla RPR di opensaml
        Optional<RelyingPartyRegistration> registration = providerConfig
            .getRelyingPartyRegistrations()
            .stream()
            .filter(reg -> reg.getRegistrationId().equals(registrationId))
            .findFirst();
        return registration.orElse(null);
    }
}
