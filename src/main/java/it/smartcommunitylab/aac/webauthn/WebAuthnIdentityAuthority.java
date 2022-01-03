package it.smartcommunitylab.aac.webauthn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnRpRegistrationRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

@Service
public class WebAuthnIdentityAuthority implements IdentityAuthority, InitializingBean {
    public static final String AUTHORITY_URL = "/auth/webauthn/";
    private WebAuthnIdentityProviderConfigMap defaultProviderConfig;
    private WebAuthnIdentityProviderConfig template;

    // This notation refers to application.yml file
    @Value("${authorities.webauthn.rpid}")
    private String rpid;

    @Value("${authorities.webauthn.rpName}")
    private String rpName;

    @Value("${authorities.webauthn.enableRegistration}")
    private boolean enableRegistration;

    @Value("${authorities.webauthn.enableUpdate}")
    private boolean enableUpdate;

    @Value("${authorities.webauthn.maxSessionDuration}")
    private int maxSessionDuration;

    @Value("${authorities.webauthn.trustUnverifiedAuthenticatorResponses}")
    private boolean trustUnverifiedAuthenticatorResponses;

    private final ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;
    private final WebAuthnUserAccountService userAccountService;

    private final UserEntityService userEntityService;

    @Autowired
    @Qualifier("webAuthnRpRegistrationRepository")
    private WebAuthnRpRegistrationRepository webAuthnRpRegistrationRepository;

    public WebAuthnIdentityAuthority(
            WebAuthnUserAccountService userAccountService,
            UserEntityService userEntityService,
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.userEntityService = userEntityService;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // build default config from props
        defaultProviderConfig = new WebAuthnIdentityProviderConfigMap();
        defaultProviderConfig.setRpid(rpid);
        defaultProviderConfig.setRpName(rpName);
        defaultProviderConfig.setTrustUnverifiedAuthenticatorResponses(trustUnverifiedAuthenticatorResponses);
        defaultProviderConfig.setEnableRegistration(enableRegistration);
        defaultProviderConfig.setEnableUpdate(enableUpdate);
        defaultProviderConfig.setMaxSessionDuration(maxSessionDuration);

        template = new WebAuthnIdentityProviderConfig("webauthn.default", null);
        template.setConfigMap(defaultProviderConfig);
        template.setName("webauthn default");

    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public boolean hasIdentityProvider(String providerId) {
        WebAuthnIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    private final LoadingCache<String, WebAuthnIdentityService> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires
                                                 // 1
                                                 // hour
                                                 // after
                                                 // fetch
            .maximumSize(100).build(new CacheLoader<String, WebAuthnIdentityService>() {
                @Override
                public WebAuthnIdentityService load(final String id) throws Exception {
                    WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    WebAuthnIdentityService idp = new WebAuthnIdentityService(id, userAccountService, userEntityService,
                            webAuthnRpRegistrationRepository,
                            config, config.getRealm());

                    return idp;

                }
            });

    @Override
    public WebAuthnIdentityService getIdentityProvider(String providerId) {
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
        Collection<WebAuthnIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public String getUserProviderId(String userId) {
        // unpack id
        return extractProviderId(userId);
    }

    @Override
    public WebAuthnIdentityService getUserIdentityProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);
        return getIdentityProvider(providerId);
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

    @Override
    public WebAuthnIdentityService registerIdentityProvider(ConfigurableIdentityProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if exists or id clashes with another provider from a different realm
            WebAuthnIdentityProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null) {
                if (!realm.equals(e.getRealm())) {
                    // name clash
                    throw new RegistrationException(
                            "a provider with the same id already exists under a different realm");
                }

                // same realm, unload and reload
                unregisterIdentityProvider(providerId);
            }

            // we also enforce a single webauthn idp per realm
            if (!registrationRepository.findByRealm(realm).isEmpty()) {
                throw new RegistrationException("an webauthn provider is already registered for the given realm");
            }

            try {
                // build config
                WebAuthnIdentityProviderConfig providerConfig = getProviderConfig(providerId, realm, cp);

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

    private WebAuthnIdentityProviderConfig getProviderConfig(String provider, String realm,
            ConfigurableIdentityProvider cp) {

        // build empty config if missing
        if (cp == null) {
            cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        } else {
            Assert.isTrue(SystemKeys.AUTHORITY_WEBAUTHN.equals(cp.getAuthority()),
                    "configuration does not match this provider");
        }

        // merge config with default
        return WebAuthnIdentityProviderConfig.fromConfigurableProvider(
                cp, defaultProviderConfig);

    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        WebAuthnIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // someone else should have already destroyed sessions
            // manual shutdown, not required here
            // WebAuthnIdentityProvider idp = providers.get(providerId);
            // idp.shutdown();

            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);

        }

    }

    @Override
    public WebAuthnIdentityService getIdentityService(String providerId) {
        return getIdentityProvider(providerId);
    }

    @Override
    public List<IdentityService> getIdentityServices(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<WebAuthnIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityService(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigurableIdentityProvider> getConfigurableProviderTemplates() {
        return Collections.singleton(WebAuthnIdentityProviderConfig.toConfigurableProvider(template));
    }

    @Override
    public ConfigurableIdentityProvider getConfigurableProviderTemplate(String templateId)
            throws NoSuchProviderException {
        if ("webauthn.default".equals(templateId)) {
            return WebAuthnIdentityProviderConfig.toConfigurableProvider(template);
        }

        throw new NoSuchProviderException("no templates available");
    }
}
