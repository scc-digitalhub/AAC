package it.smartcommunitylab.aac.saml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProvider;

@Service
public class SamlIdentityAuthority implements IdentityAuthority {

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/saml/";

    private final SamlUserAccountRepository accountRepository;

    // identity providers by id
    private final Map<String, SamlIdentityProvider> providers = new HashMap<>();

    // saml sp services
    private final SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_SAML;
    }

    public SamlIdentityAuthority(SamlUserAccountRepository accountRepository,
            SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(relyingPartyRegistrationRepository, "relayingParty registration repository is mandatory");

        this.accountRepository = accountRepository;
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
    }

    @Override
    public IdentityProvider getIdentityProvider(String providerId) {
        return providers.get(providerId);
    }

    @Override
    public List<IdentityProvider> getIdentityProviders(String realm) {
        return providers.values().stream().filter(idp -> idp.getRealm().equals(realm)).collect(Collectors.toList());
    }

    @Override
    public IdentityProvider getUserIdentityProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);
        // get
        return getIdentityProvider(providerId);
    }

    @Override
    public void registerIdentityProvider(ConfigurableProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            try {
                // link to internal repos
                // TODO add attribute store as persistentStore
                SamlIdentityProvider idp = new SamlIdentityProvider(
                        providerId,
                        accountRepository, null,
                        cp,
                        realm);

                // build registration, will ensure configuration is valid *before* registering
                // the provider in repositories
                RelyingPartyRegistration registration = idp.getRelyingPartyRegistration();

                // register
                providers.put(providerId, idp);

                // add rp registration to registry
                relyingPartyRegistrationRepository.addRegistration(registration);

            } catch (Exception ex) {
                // cleanup
                relyingPartyRegistrationRepository.removeRegistration(providerId);
                providers.remove(providerId);

                throw new IllegalArgumentException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        if (providers.containsKey(providerId)) {
            synchronized (this) {
                // remove from repository to disable filters
                relyingPartyRegistrationRepository.removeRegistration(providerId);

                SamlIdentityProvider idp = providers.get(providerId);
                // someone else should have already destroyed sessions

                // remove
                providers.remove(providerId);
            }

        }

    }

    /*
     * helpers
     */
    private String extractProviderId(String userId) throws IllegalArgumentException {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("empty or null id");
        }

        String[] s = userId.split(Pattern.quote("|"));

        if (s.length != 3) {
            throw new IllegalArgumentException("invalid resource id");
        }

        // check match
        if (!getAuthorityId().equals(s[0])) {
            throw new IllegalArgumentException("authority mismatch");
        }

        if (!StringUtils.hasText(s[1])) {
            throw new IllegalArgumentException("empty provider id");
        }

        return s[1];

    }

}
