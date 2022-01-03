package it.smartcommunitylab.aac.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class InternalIdentityAuthority implements IdentityAuthority, InitializingBean {

    // TODO replace with proper configuration bean read from props
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

    private final InternalUserAccountService userAccountService;

    private InternalIdentityProviderConfigMap defaultProviderConfig;

    private InternalIdentityProviderConfig template;

    // services
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    // identity providers by id
//    private Map<String, InternalIdentityProvider> providers = new HashMap<>();

    private final ProviderRepository<InternalIdentityProviderConfig> registrationRepository;

    // loading cache for idps
    private final LoadingCache<String, InternalIdentityService> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, InternalIdentityService>() {
                @Override
                public InternalIdentityService load(final String id) throws Exception {
                    InternalIdentityProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    InternalIdentityService idp = new InternalIdentityService(
                            id,
                            userAccountService, userEntityService,
                            config, config.getRealm());

                    // set services
                    idp.setMailService(mailService);
                    idp.setUriBuilder(uriBuilder);

                    return idp;

                }
            });

    public InternalIdentityAuthority(
            InternalUserAccountService userAccountService,
            UserEntityService userEntityService,
            ProviderRepository<InternalIdentityProviderConfig> registrationRepository) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.userEntityService = userEntityService;
        this.registrationRepository = registrationRepository;

    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    public void setDefaultProviderConfig(InternalIdentityProviderConfigMap defaultProviderConfig) {
        this.defaultProviderConfig = defaultProviderConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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

        template = new InternalIdentityProviderConfig("internal.default", null);
        template.setConfigMap(defaultProviderConfig);
        template.setName("system default");

    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public boolean hasIdentityProvider(String providerId) {
        InternalIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public InternalIdentityService getIdentityProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            return providers.get(providerId);
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public List<IdentityProvider> getIdentityProviders(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<InternalIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public String getUserProviderId(String userId) {
        // unpack id
        return extractProviderId(userId);
    }

    @Override
    public InternalIdentityService getUserIdentityProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);
        return getIdentityProvider(providerId);
    }

    @Override
    public InternalIdentityService registerIdentityProvider(ConfigurableIdentityProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if exists or id clashes with another provider from a different realm
            InternalIdentityProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null) {
                if (!realm.equals(e.getRealm())) {
                    // name clash
                    throw new RegistrationException(
                            "a provider with the same id already exists under a different realm");
                }

                // same realm, unload and reload
                unregisterIdentityProvider(providerId);
            }

            // we also enforce a single internal idp per realm
            if (!registrationRepository.findByRealm(realm).isEmpty()) {
                throw new RegistrationException("an internal provider is already registered for the given realm");
            }

            try {
                // build config
                InternalIdentityProviderConfig providerConfig = getProviderConfig(providerId, realm, cp);

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // load and return
                return providers.get(providerId);
            } catch (Exception ee) {
                // cleanup
                registrationRepository.removeRegistration(providerId);

                throw new RegistrationException(ee.getMessage());
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        InternalIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // someone else should have already destroyed sessions
            // manual shutdown, not required here
//                InternalIdentityProvider idp = providers.get(providerId);
//                idp.shutdown();

            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);

        }

    }

    @Override
    public InternalIdentityService getIdentityService(String providerId) {
        return getIdentityProvider(providerId);
    }

    @Override
    public List<IdentityService> getIdentityServices(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<InternalIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityService(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigurableIdentityProvider> getConfigurableProviderTemplates() {
        return Collections.singleton(InternalIdentityProviderConfig.toConfigurableProvider(template));
    }

    @Override
    public ConfigurableIdentityProvider getConfigurableProviderTemplate(String templateId)
            throws NoSuchProviderException {
        if ("internal.default".equals(templateId)) {
            return InternalIdentityProviderConfig.toConfigurableProvider(template);
        }

        throw new NoSuchProviderException("no templates available");
    }

    /*
     * Helpers
     */

    private InternalIdentityProviderConfig getProviderConfig(String provider, String realm,
            ConfigurableIdentityProvider cp) {

        // build empty config if missing
        if (cp == null) {
            cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        } else {
            Assert.isTrue(SystemKeys.AUTHORITY_INTERNAL.equals(cp.getAuthority()),
                    "configuration does not match this provider");
        }

        // merge config with default
        return InternalIdentityProviderConfig.fromConfigurableProvider(
                cp, defaultProviderConfig);

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
