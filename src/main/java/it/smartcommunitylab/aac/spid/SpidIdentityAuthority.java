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
import it.smartcommunitylab.aac.attributes.AttributeManager;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.config.SpidProperties;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
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

    private final ProviderRepository<SpidIdentityProviderConfig> registrationRepository;

    // loading cache for idps
    private final LoadingCache<String, SpidIdentityProvider> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, SpidIdentityProvider>() {
                @Override
                public SpidIdentityProvider load(final String id) throws Exception {
                    SpidIdentityProviderConfig config = registrationRepository.findByProviderId(id);
                    config.setIdps(spidRegistry.getIdentityProviders());

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

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

    // attribute manager for custom attributes mapping
    private AttributeManager attributeManager;

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_SPID;
    }

    public SpidIdentityAuthority(
            SpidUserAccountRepository accountRepository,
            ProviderRepository<SpidIdentityProviderConfig> registrationRepository,
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

    @Autowired
    public void setAttributeManager(AttributeManager attributeManager) {
        this.attributeManager = attributeManager;
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
    public String getUserProviderId(String userId) {
        // unpack id
        return extractProviderId(userId);
    }

    @Override
    public SpidIdentityProvider getUserIdentityProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);
        return getIdentityProvider(providerId);
    }

    @Override
    public SpidIdentityProvider registerIdentityProvider(ConfigurableProvider cp) {
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
    public void unregisterIdentityProvider(String realm, String providerId) {
        SpidIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // check realm match
            if (!realm.equals(registration.getRealm())) {
                throw new IllegalArgumentException("realm does not match");
            }

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
    public Collection<ConfigurableProvider> getConfigurableProviderTemplates() {
        return Collections.emptyList();
    }

    @Override
    public ConfigurableProvider getConfigurableProviderTemplate(String templateId) throws NoSuchProviderException {
        throw new NoSuchProviderException();
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
