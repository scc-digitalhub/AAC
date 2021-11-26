package it.smartcommunitylab.aac.openid;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.config.ProvidersProperties;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

@Service
public class OIDCIdentityAuthority implements IdentityAuthority, InitializingBean {

    // TODO make consistent with global config
    public static final String AUTHORITY_URL = "/auth/oidc/";

    // private account repository
    private final OIDCUserAccountRepository accountRepository;

    // system attributes store
    private final AutoJdbcAttributeStore jdbcAttributeStore;

//    // identity providers by id
//    // TODO move to a registry with cache/db etc
//    // this class should fetch only configuration from registry, parsed, and handle
//    // a loading cache to instantiate providers as needed
//    private final Map<String, OIDCIdentityProvider> providers = new HashMap<>();

    private final ProviderRepository<OIDCIdentityProviderConfig> registrationRepository;

    // loading cache for idps
    private final LoadingCache<String, OIDCIdentityProvider> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, OIDCIdentityProvider>() {
                @Override
                public OIDCIdentityProvider load(final String id) throws Exception {
                    OIDCIdentityProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    AttributeStore attributeStore = getAttributeStore(id, config.getPersistence());

                    OIDCIdentityProvider idp = new OIDCIdentityProvider(
                            getAuthorityId(), id,
                            accountRepository, attributeStore,
                            config,
                            config.getRealm());

                    idp.setExecutionService(executionService);
                    return idp;

                }
            });

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    // configuration templates
    private ProvidersProperties providerProperties;
    private final Map<String, OIDCIdentityProviderConfig> templates = new HashMap<>();

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;

    public OIDCIdentityAuthority(
            OIDCUserAccountRepository accountRepository,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderRepository<OIDCIdentityProviderConfig> registrationRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {

        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.accountRepository = accountRepository;
        this.jdbcAttributeStore = jdbcAttributeStore;
        this.registrationRepository = registrationRepository;
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

    @Autowired
    public void setProviderProperties(ProvidersProperties providerProperties) {
        this.providerProperties = providerProperties;
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // build templates
        if (providerProperties != null && providerProperties.getTemplates() != null) {
            List<OIDCIdentityProviderConfigMap> templateConfigs = providerProperties.getTemplates().getOidc();
            for (OIDCIdentityProviderConfigMap configMap : templateConfigs) {
                try {
                    String templateId = "oidc." + configMap.getClientName().toLowerCase();
                    OIDCIdentityProviderConfig template = new OIDCIdentityProviderConfig(templateId, null);
                    template.setConfigMap(configMap);
                    template.setName(configMap.getClientName());

                    templates.put(templateId, template);
                } catch (Exception e) {
                    // skip
                }
            }
        }

    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_OIDC;
    }

    @Override
    public boolean hasIdentityProvider(String providerId) {
        OIDCIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public OIDCIdentityProvider getIdentityProvider(String providerId) {
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
        Collection<OIDCIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public String getUserProviderId(String userId) {
        // unpack id
        return extractProviderId(userId);
    }

    @Override
    public OIDCIdentityProvider getUserIdentityProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);
        return getIdentityProvider(providerId);
    }

    @Override
    public OIDCIdentityProvider registerIdentityProvider(ConfigurableIdentityProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if id clashes with another provider from a different realm
            OIDCIdentityProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null && !realm.equals(e.getRealm())) {
                // name clash
                throw new RegistrationException("a provider with the same id already exists under a different realm");
            }

            try {
                OIDCIdentityProviderConfig providerConfig = OIDCIdentityProviderConfig.fromConfigurableProvider(cp);

                // build registration, will ensure configuration is valid *before* registering
                // the provider in repositories
                ClientRegistration registration = providerConfig.getClientRegistration();

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // add client registration to registry
                clientRegistrationRepository.addRegistration(registration);

                // load and return
                return providers.get(providerId);
            } catch (Exception ex) {
                // cleanup
                clientRegistrationRepository.removeRegistration(providerId);
                registrationRepository.removeRegistration(providerId);

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        OIDCIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // remove from repository to disable filters
            clientRegistrationRepository.removeRegistration(providerId);

            // remove from cache
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
        return templates.values().stream().map(c -> OIDCIdentityProviderConfig.toConfigurableProvider(c))
                .collect(Collectors.toList());
    }

    @Override
    public ConfigurableIdentityProvider getConfigurableProviderTemplate(String templateId)
            throws NoSuchProviderException {
        if (templates.containsKey(templateId)) {
            return OIDCIdentityProviderConfig.toConfigurableProvider(templates.get(templateId));
        }

        throw new NoSuchProviderException("no templates available");
    }

    /*
     * helpers
     */

    private AttributeStore getAttributeStore(String providerId, String persistence) {
        // we generate a new store for each provider
        AttributeStore store = new NullAttributeStore();
        if (SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)) {
            store = new PersistentAttributeStore(SystemKeys.AUTHORITY_OIDC, providerId, jdbcAttributeStore);
        } else if (SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)) {
            store = new InMemoryAttributeStore(SystemKeys.AUTHORITY_OIDC, providerId);
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
