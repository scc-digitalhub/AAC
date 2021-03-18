package it.smartcommunitylab.aac.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;

@Service
public class InternalIdentityAuthority implements IdentityAuthority {

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/internal/";

    private final InternalUserAccountRepository accountRepository;

    // identity providers by id
    private Map<String, InternalIdentityProvider> providers = new HashMap<>();

    public InternalIdentityAuthority(InternalUserAccountRepository accountRepository) {
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;

    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    private void registerIdp(InternalIdentityProvider idp) {
        providers.put(idp.getProvider(), idp);
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

            // check if id clashes with another provider from a different realm
            InternalIdentityProvider e = providers.get(providerId);
            if (e != null && !realm.equals(e.getRealm())) {
                // name clash
                throw new RegistrationException("a provider with the same id already exists under a different realm");
            }

            // we also enforce a single internal idp per realm
            if (!getIdentityProviders(realm).isEmpty()) {
                throw new RegistrationException("an internal provider is already registered for the given realm");
            }

            // link to internal repos
            InternalIdentityProvider internalIdp = new InternalIdentityProvider(
                    providerId,
                    accountRepository,
                    realm);

            // register
            registerIdp(internalIdp);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void unregisterIdentityProvider(String realm, String providerId) {
        if (providers.containsKey(providerId)) {
            synchronized (this) {
                InternalIdentityProvider idp = providers.get(providerId);

                // check realm match
                if (!realm.equals(idp.getRealm())) {
                    throw new IllegalArgumentException("realm does not match");
                }

                // can't unregister default provider, check
                if (SystemKeys.REALM_GLOBAL.equals(idp.getRealm())) {
                    return;
                }

                // someone else should have already destroyed sessions
                idp.shutdown();

                // remove
                providers.remove(providerId);
            }

        }

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
