package it.smartcommunitylab.aac.openid.auth;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;


public class OIDCClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {
    private final Map<String, ClientRegistration> registrations;

    public OIDCClientRegistrationRepository() {
        this.registrations = new ConcurrentHashMap<>();
    }

    /*
     * read access as per interface
     */

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return this.registrations.get(registrationId);
    }

    /**
     * Returns an {@code Iterator} of {@link ClientRegistration}.
     *
     * @return an {@code Iterator<ClientRegistration>}
     */
    @Override
    public Iterator<ClientRegistration> iterator() {
        return this.registrations.values().iterator();
    }

    /*
     * write access to dynamically manage clients
     */
    public void addRegistration(ClientRegistration registration) {
        // we override old registration if present
        // TODO require removal before add to ensure we have no hung requests
        registrations.put(registration.getRegistrationId(), registration);
    }

    public void removeRegistration(ClientRegistration registration) {
        registrations.remove(registration.getRegistrationId());
    }

    public void removeRegistration(String registrationId) {
        registrations.remove(registrationId);
    }

}
