package it.smartcommunitylab.aac.openid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AttributeStore;
import it.smartcommunitylab.aac.attributes.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.PersistentAttributeStore;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.UserCredentialsStore;
import it.smartcommunitylab.aac.core.provider.UserService;
import it.smartcommunitylab.aac.core.service.AttributeEntityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;

@Service
public class OIDCIdentityAuthority implements IdentityAuthority {

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/oidc/";

    // private account repository
    private final OIDCUserAccountRepository accountRepository;

    // system attributes repository
    private final AttributeEntityService attributeEntityService;

    // identity providers by id
    private final Map<String, OIDCIdentityProvider> providers = new HashMap<>();

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    public OIDCIdentityAuthority(
            OIDCUserAccountRepository accountRepository,
            AttributeEntityService attributeEntityService,
            OIDCClientRegistrationRepository clientRegistrationRepository) {

        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(attributeEntityService, "attributeEntity service is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.accountRepository = accountRepository;
        this.attributeEntityService = attributeEntityService;
        this.clientRegistrationRepository = clientRegistrationRepository;
//
//        // global client registration repository to be used by global filters
//        clientRegistrationRepository = new OIDCClientRegistrationRepository();
////        oauth2ClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
//        authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
//
//        // oauth2 filters
//        redirectFilter = new OAuth2AuthorizationRequestRedirectFilter(clientRegistrationRepository, BASE_URL);
//        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
//
//        // our login filter leverages extendedAuth manager to handle multi-realm
//        loginFilter = new OIDCLoginAuthenticationFilter(clientRegistrationRepository);
//        loginFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
//        loginFilter.setAuthenticationManager(authManager);
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_OIDC;
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
    public String getUserProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);

        // check if exists
        if (providers.containsKey(providerId)) {
            return providerId;
        }

        return null;
    }

    @Override
    public OIDCIdentityProvider registerIdentityProvider(ConfigurableProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if id clashes with another provider from a different realm
            OIDCIdentityProvider e = providers.get(providerId);
            if (e != null && !realm.equals(e.getRealm())) {
                // name clash
                throw new RegistrationException("a provider with the same id already exists under a different realm");
            }

            try {
                // link to internal repos
                // add attribute store where requested
                AttributeStore attributeStore = getAttributeStore(providerId, cp.getPersistence());
                OIDCIdentityProvider idp = new OIDCIdentityProvider(
                        providerId,
                        accountRepository, attributeStore,
                        cp,
                        realm);

                // build registration, will ensure configuration is valid *before* registering
                // the provider in repositories
                ClientRegistration registration = idp.getClientRegistration();

                // register
                providers.put(idp.getProvider(), idp);

                // add client registration to registry
                clientRegistrationRepository.addRegistration(registration);

                return idp;
            } catch (Exception ex) {
                // cleanup
                clientRegistrationRepository.removeRegistration(providerId);
                providers.remove(providerId);

                throw new IllegalArgumentException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void unregisterIdentityProvider(String realm, String providerId) {
        if (providers.containsKey(providerId)) {
            synchronized (this) {
                OIDCIdentityProvider idp = providers.get(providerId);

                // check realm match
                if (!realm.equals(idp.getRealm())) {
                    throw new IllegalArgumentException("realm does not match");
                }

                // remove from repository to disable filters
                clientRegistrationRepository.removeRegistration(providerId);

                // someone else should have already destroyed sessions

                // remove
                providers.remove(providerId);
            }

        }

    }

    @Override
    public UserService getUserService(String providerId) {
        // TODO add read-only userService
        return null;
    }

    @Override
    public UserCredentialsStore getUserCredentialsStore(String providerId) {
        // not supported
        return null;
    }

    /*
     * helpers
     */

    private AttributeStore getAttributeStore(String providerId, String persistence) {
        // we generate a new store for each provider
        AttributeStore store = new NullAttributeStore();
        if (SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)) {
            store = new PersistentAttributeStore(SystemKeys.AUTHORITY_OIDC, providerId, attributeEntityService);
        } else if (SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)) {
            store = new InMemoryAttributeStore();
        }

        return store;
    }

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
