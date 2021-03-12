package it.smartcommunitylab.aac.saml.auth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.Assert;

public class SamlRelyingPartyRegistrationRepository
        implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {

    private final Map<String, RelyingPartyRegistration> registrations;

    public SamlRelyingPartyRegistrationRepository() {
        this.registrations = new HashMap<>();
    }

    /*
     * read access as per interface
     */

    @Override
    public RelyingPartyRegistration findByRegistrationId(String id) {
        Assert.hasText(id, "id cannot be empty");
        return this.registrations.get(id);
    }

    @Override
    public Iterator<RelyingPartyRegistration> iterator() {
        return this.registrations.values().iterator();
    }

    /*
     * write access to dynamically manage clients
     */
    public void addRegistration(RelyingPartyRegistration registration) {
        // we override old registration if present
        // TODO require removal before add to ensure we have no hung requests
        registrations.put(registration.getRegistrationId(), registration);
    }

    public void removeRegistration(RelyingPartyRegistration registration) {
        registrations.remove(registration.getRegistrationId());
    }

    public void removeRegistration(String registrationId) {
        registrations.remove(registrationId);
    }

}
