package it.smartcommunitylab.aac.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.provider.InternalPasswordIdentityService;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class InternalIdentityAuthority implements IdentityAuthority, InitializingBean {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // user service
    private final UserEntityService userEntityService;

    // resources registry
    private final SubjectService subjectService;

    // internal account persistence service
    private final InternalUserAccountService userAccountService;
    private final InternalUserPasswordRepository passwordRepository;

    // configuration provider
    private InternalIdentityConfigurationProvider configProvider;
    private AuthoritiesProperties authoritiesProperties;

    // services
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    // identity provider configs by id
    private final ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository;

    // loading cache for idps
    // TODO replace with external loadableProviderRepository for
    // ProviderRepository<InternalIdentityProvider>
    private final LoadingCache<String, InternalIdentityService<?>> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, InternalIdentityService<?>>() {
                @Override
                public InternalIdentityService<?> load(final String id) throws Exception {
                    InternalIdentityProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    if (config.getCredentialsType() == CredentialsType.PASSWORD) {

                        InternalPasswordIdentityService idp = new InternalPasswordIdentityService(
                                id,
                                userAccountService, userEntityService, subjectService,
                                passwordRepository,
                                config, config.getRealm());

                        // set services
                        idp.setMailService(mailService);
                        idp.setUriBuilder(uriBuilder);
                        return idp;
                    }

                    throw new IllegalArgumentException("no configuration matching the given provider id");

                }
            });

    public InternalIdentityAuthority(
            InternalUserAccountService userAccountService, InternalUserPasswordRepository passwordRepository,
            UserEntityService userEntityService, SubjectService subjectService,
            ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(passwordRepository, "password repository is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.passwordRepository = passwordRepository;
        this.userEntityService = userEntityService;
        this.subjectService = subjectService;
        this.registrationRepository = registrationRepository;
    }

    @Autowired
    public void setConfigProvider(InternalIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Autowired
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Autowired
    public void setAuthoritiesProperties(AuthoritiesProperties authoritiesProperties) {
        this.authoritiesProperties = authoritiesProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(configProvider, "config provider is mandatory");
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
    public InternalIdentityService<?> getIdentityProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            return providers.get(providerId);
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public List<InternalIdentityService<?>> getIdentityProviders(
            String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<InternalIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

//    @Override
//    public String getUserProviderId(String userId) {
//        // unpack id
//        return extractProviderId(userId);
//    }
//
//    @Override
//    public InternalIdentityService getUserIdentityProvider(String userId) {
//        // unpack id
//        String providerId = extractProviderId(userId);
//        return getIdentityProvider(providerId);
//    }

    @Override
    public InternalIdentityService<?> registerIdentityProvider(ConfigurableIdentityProvider cp) {
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
            }

            try {
                // build config
                InternalIdentityProviderConfig providerConfig = configProvider.getConfig(cp);

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // load and return
                return providers.get(providerId);
            } catch (Exception ex) {
                // cleanup
                registrationRepository.removeRegistration(providerId);

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
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
    public InternalIdentityService<?> getIdentityService(String providerId) {
        return getIdentityProvider(providerId);
    }

    @Override
    public List<InternalIdentityService<?>> getIdentityServices(
            String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<InternalIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityService(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigurableIdentityProvider> getConfigurableProviderTemplates() {
        return Collections.emptyList();
    }

    @Override
    public ConfigurableIdentityProvider getConfigurableProviderTemplate(String templateId)
            throws NoSuchProviderException {
        throw new NoSuchProviderException("no templates available");
    }

}
