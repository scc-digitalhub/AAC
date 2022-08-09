package it.smartcommunitylab.aac.openid;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.service.OIDCUserAccountService;

@Service
public class OIDCIdentityAuthority extends
        AbstractIdentityAuthority<OIDCUserIdentity, OIDCIdentityProvider, OIDCIdentityProviderConfig, OIDCIdentityProviderConfigMap>
        implements InitializingBean {

    public static final String AUTHORITY_URL = "/auth/oidc/";

    // oidc account service
    private final OIDCUserAccountService accountService;

    // system attributes store
    private final AutoJdbcAttributeStore jdbcAttributeStore;

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;

    @Autowired
    public OIDCIdentityAuthority(
            UserEntityService userEntityService, SubjectService subjectService,
            OIDCUserAccountService userAccountService, AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository,
            @Qualifier("oidcClientRegistrationRepository") OIDCClientRegistrationRepository clientRegistrationRepository) {
        this(SystemKeys.AUTHORITY_OIDC, userEntityService, subjectService, userAccountService, jdbcAttributeStore,
                registrationRepository, clientRegistrationRepository);
    }

    public OIDCIdentityAuthority(
            String authorityId,
            UserEntityService userEntityService, SubjectService subjectService,
            OIDCUserAccountService userAccountService, AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {
        super(authorityId, userEntityService, subjectService, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.accountService = userAccountService;
        this.jdbcAttributeStore = jdbcAttributeStore;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Autowired
    public void setConfigProvider(OIDCIdentityConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public OIDCIdentityProvider buildProvider(OIDCIdentityProviderConfig config) {
        String id = config.getProvider();
        AttributeStore attributeStore = getAttributeStore(id, config.getPersistence());

        OIDCIdentityProvider idp = new OIDCIdentityProvider(
                authorityId, id,
                userEntityService, accountService, subjectService,
                attributeStore, config, config.getRealm());

        idp.setExecutionService(executionService);
        return idp;
    }

    @Override
    public OIDCIdentityProvider registerProvider(ConfigurableProvider cp) {
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {

            // fetch id from config
            String providerId = cp.getProvider();

            // register and build via super
            OIDCIdentityProvider idp = super.registerProvider(cp);

            try {
                // extract clientRegistration from config
                ClientRegistration registration = idp.getConfig().getClientRegistration();

                // add client registration to registry
                clientRegistrationRepository.addRegistration(registration);

                return idp;
            } catch (Exception ex) {
                // cleanup
                clientRegistrationRepository.removeRegistration(providerId);

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException();
        }

    }

    @Override
    public void unregisterProvider(String providerId) {
        OIDCIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // remove from repository to disable filters
            clientRegistrationRepository.removeRegistration(providerId);

            // someone else should have already destroyed sessions

            // remove from config
            super.unregisterProvider(providerId);

        }

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

}
