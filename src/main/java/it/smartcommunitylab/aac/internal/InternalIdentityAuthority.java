package it.smartcommunitylab.aac.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

@Service
public class InternalIdentityAuthority implements IdentityAuthority {

    @Value("${authorities.internal.confirmation.required}")
    private boolean confirmationRequired;

    @Value("${authorities.internal.confirmation.validity}")
    private int confirmationValidity;

    @Value("${authorities.internal.password.reset.enabled}")
    private boolean passwordResetEnabled;

    @Value("${authorities.internal.password.reset.validity}")
    private int passwordResetValidity;

    @Value("${authorities.internal.password.minLength}")
    private int passwordMinLength;
    @Value("${authorities.internal.password.maxLength}")
    private int passwordMaxLength;
    @Value("${authorities.internal.password.requireAlpha}")
    private boolean passwordRequireAlpha;
    @Value("${authorities.internal.password.requireNumber}")
    private boolean passwordRequireNumber;
    @Value("${authorities.internal.password.requireSpecial}")
    private boolean passwordRequireSpecial;
    @Value("${authorities.internal.password.supportWhitespace}")
    private boolean passwordSupportWhitespace;

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/internal/";

    private final UserEntityService userEntityService;

    private final InternalUserAccountRepository accountRepository;

    private final InternalIdentityProviderConfigMap defaultProviderConfig;

    // identity providers by id
    private Map<String, InternalIdentityProvider> providers = new HashMap<>();

    public InternalIdentityAuthority(InternalUserAccountRepository accountRepository,
            UserEntityService userEntityService) {
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");

        this.accountRepository = accountRepository;
        this.userEntityService = userEntityService;

        // build default config from props
        defaultProviderConfig = new InternalIdentityProviderConfigMap();
        defaultProviderConfig.setConfirmationRequired(confirmationRequired);
        defaultProviderConfig.setConfirmationValidity(confirmationValidity);
        defaultProviderConfig.setEnablePasswordReset(passwordResetEnabled);
        defaultProviderConfig.setPasswordResetValidity(passwordResetValidity);
        defaultProviderConfig.setPasswordMaxLength(passwordMaxLength);
        defaultProviderConfig.setPasswordMinLength(passwordMinLength);
        defaultProviderConfig.setPasswordRequireAlpha(passwordRequireAlpha);
        defaultProviderConfig.setPasswordRequireNumber(passwordRequireNumber);
        defaultProviderConfig.setPasswordRequireSpecial(passwordRequireSpecial);
        defaultProviderConfig.setPasswordSupportWhitespace(passwordSupportWhitespace);
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
    public InternalIdentityProvider registerIdentityProvider(ConfigurableProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

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
            InternalIdentityProvider idp = new InternalIdentityProvider(
                    providerId,
                    accountRepository, userEntityService,
                    cp, defaultProviderConfig,
                    realm);

            // register
            registerIdp(idp);

            return idp;
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
                if (SystemKeys.REALM_SYSTEM.equals(idp.getRealm())) {
                    return;
                }

                // someone else should have already destroyed sessions
                idp.shutdown();

                // remove
                providers.remove(providerId);
            }

        }

    }

    @Override
    public InternalIdentityProvider getIdentityService(String providerId) {
        // internal idp is an identityService
        return providers.get(providerId);
    }

    @Override
    public List<IdentityService> getIdentityServices(String realm) {
        // internal idps are identityService
        return providers.values().stream().filter(idp -> idp.getRealm().equals(realm)).collect(Collectors.toList());
    }

    /*
     * Helpers
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
