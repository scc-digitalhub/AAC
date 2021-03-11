package it.smartcommunitylab.aac.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
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
        // TODO
        return null;
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
            InternalIdentityProvider internalIdp = new InternalIdentityProvider(
                    providerId,
                    accountRepository,
                    realm);

            // register
            registerIdp(internalIdp);
        }
    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        if (providers.containsKey(providerId)) {
            synchronized (this) {
                InternalIdentityProvider idp = providers.get(providerId);

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

}
