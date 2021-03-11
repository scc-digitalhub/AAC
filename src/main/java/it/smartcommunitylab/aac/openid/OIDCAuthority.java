package it.smartcommunitylab.aac.openid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.openid.service.OIDCClientRegistrationRepository;

@Service
public class OIDCAuthority implements IdentityAuthority {

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/oidc/";

    private final OIDCUserAccountRepository accountRepository;

    // identity providers by id
    private final Map<String, OIDCIdentityProvider> providers = new HashMap<>();

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_OIDC;
    }

    public OIDCAuthority(
            OIDCUserAccountRepository accountRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {

        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.accountRepository = accountRepository;
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
    public void registerIdentityProvider(ConfigurableProvider idp) {
        // we support only identity provider as resource providers
        if (idp != null
                && getAuthorityId().equals(idp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(idp.getType())) {
            String providerId = idp.getProvider();
            String realm = idp.getRealm();

            // link to internal repos
            // TODO add attribute store as persistentStore
            OIDCIdentityProvider oidp = new OIDCIdentityProvider(
                    providerId,
                    accountRepository, null,
                    idp,
                    realm);

            // register
            providers.put(oidp.getProvider(), oidp);

            // add client registration to registry
            clientRegistrationRepository.addRegistration(oidp.getClientRegistration());
        }
    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        if (providers.containsKey(providerId)) {
            synchronized (this) {
                // remove from repository to disable filters
                clientRegistrationRepository.removeRegistration(providerId);

                OIDCIdentityProvider idp = providers.get(providerId);
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
