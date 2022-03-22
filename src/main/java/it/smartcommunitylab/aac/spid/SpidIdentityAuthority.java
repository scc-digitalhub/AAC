package it.smartcommunitylab.aac.spid;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.config.SpidProperties;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.service.LocalSpidRegistry;
import it.smartcommunitylab.aac.spid.service.SpidRegistry;

@Service
public class SpidIdentityAuthority implements IdentityAuthority, InitializingBean {

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/spid/";

    private final SpidUserAccountRepository accountRepository;

    private final ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository;

    // loading cache for idps
    private final LoadingCache<String, SpidIdentityProvider> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, SpidIdentityProvider>() {
                @Override
                public SpidIdentityProvider load(final String id) throws Exception {
                    SpidIdentityProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }
                    config.setIdps(spidRegistry.getIdentityProviders());

                    SpidIdentityProvider idp = new SpidIdentityProvider(
                            id, config.getName(),
                            accountRepository,
                            config, config.getRealm());
                    return idp;

                }
            });

    // saml sp services
    private final SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    // configuration
    private SpidProperties spidProperties;
    private SpidRegistry spidRegistry;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;

    public SpidIdentityAuthority(
            SpidUserAccountRepository accountRepository,
            ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
            @Qualifier("spidRelyingPartyRegistrationRepository") SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository) {
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");
        Assert.notNull(samlRelyingPartyRegistrationRepository, "relayingParty registration repository is mandatory");

        this.accountRepository = accountRepository;
        this.registrationRepository = registrationRepository;
        this.relyingPartyRegistrationRepository = samlRelyingPartyRegistrationRepository;
    }

    @Autowired
    public void setSpidProperties(SpidProperties spidProperties) {
        this.spidProperties = spidProperties;
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // initialize registry
        if (spidProperties != null) {
            // we support only local registry for now
            spidRegistry = new LocalSpidRegistry(spidProperties);
        }
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_SPID;
    }

    @Override
    public boolean hasIdentityProvider(String providerId) {
        SpidIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public SpidIdentityProvider getIdentityProvider(String providerId) {
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
        Collection<SpidIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public SpidIdentityProvider registerIdentityProvider(ConfigurableIdentityProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if id clashes with another provider from a different realm
            SpidIdentityProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null && !realm.equals(e.getRealm())) {
                // name clash
                throw new RegistrationException("a provider with the same id already exists under a different realm");
            }

            try {
                SpidIdentityProviderConfig providerConfig = SpidIdentityProviderConfig.fromConfigurableProvider(cp);
                providerConfig.setIdps(spidRegistry.getIdentityProviders());

                // build registration, will ensure configuration is valid *before* registering
                // the provider in repositories
                Set<RelyingPartyRegistration> registrations = providerConfig.getRelyingPartyRegistrations();

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // add client registration to registry for each idp
                for (RelyingPartyRegistration registration : registrations) {
                    relyingPartyRegistrationRepository.addRegistration(registration);
                }

                // load and return
                return providers.get(providerId);
            } catch (Exception ex) {
                // cleanup
                relyingPartyRegistrationRepository.removeRegistration(providerId);
                registrationRepository.removeRegistration(providerId);

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        SpidIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // remove all registrations from repository to disable filters
            Set<String> registrationIds = registration.getRelyingPartyRegistrationIds();
            for (String registrationId : registrationIds) {
                relyingPartyRegistrationRepository.removeRegistration(registrationId);
            }

            // remove config from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);

            // someone else should have already destroyed sessions

        }

    }

    @Override
    public IdentityService getIdentityService(String providerId) {
        return null;
    }

    @Override
    public List<IdentityService> getIdentityServices(String realm) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ConfigurableIdentityProvider> getConfigurableProviderTemplates() {
        return Collections.emptyList();
    }

    @Override
    public ConfigurableIdentityProvider getConfigurableProviderTemplate(String templateId)
            throws NoSuchProviderException {
        throw new NoSuchProviderException();
    }

}
