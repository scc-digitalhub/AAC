package it.smartcommunitylab.aac.saml;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
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
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityConfigurationProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import it.smartcommunitylab.aac.saml.service.SamlUserAccountService;

@Service
public class SamlIdentityAuthority extends
        AbstractIdentityAuthority<SamlUserIdentity, SamlIdentityProvider, SamlIdentityProviderConfig, SamlIdentityProviderConfigMap>
        implements InitializingBean {

    public static final String AUTHORITY_URL = "/auth/saml/";

    // saml account service
    private final SamlUserAccountService accountService;

    // system attributes store
    private final AutoJdbcAttributeStore jdbcAttributeStore;

    // saml sp services
    private final SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;

    public SamlIdentityAuthority(
            UserEntityService userEntityService, SubjectService subjectService,
            SamlUserAccountService userAccountService, AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository,
            @Qualifier("samlRelyingPartyRegistrationRepository") SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository) {
        super(userEntityService, subjectService, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");
        Assert.notNull(samlRelyingPartyRegistrationRepository, "relayingParty registration repository is mandatory");

        this.accountService = userAccountService;
        this.jdbcAttributeStore = jdbcAttributeStore;

        this.relyingPartyRegistrationRepository = samlRelyingPartyRegistrationRepository;
    }

    @Autowired
    public void setConfigProvider(SamlIdentityConfigurationProvider configProvider) {
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
    public SamlIdentityProvider buildProvider(SamlIdentityProviderConfig config) {
        String id = config.getProvider();
        AttributeStore attributeStore = getAttributeStore(id, config.getPersistence());

        SamlIdentityProvider idp = new SamlIdentityProvider(
                id,
                userEntityService, accountService, subjectService,
                attributeStore, config, config.getRealm());

        idp.setExecutionService(executionService);
        return idp;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_SAML;
    }

    @Override
    public SamlIdentityProvider registerProvider(ConfigurableProvider cp) {
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {

            // fetch id from config
            String providerId = cp.getProvider();

            // register and build via super
            SamlIdentityProvider idp = super.registerProvider(cp);

            try {
                // extract clientRegistration from config
                RelyingPartyRegistration registration = idp.getConfig().getRelyingPartyRegistration();

                // add client registration to registry
                relyingPartyRegistrationRepository.addRegistration(registration);

                return idp;
            } catch (Exception ex) {
                // cleanup
                relyingPartyRegistrationRepository.removeRegistration(providerId);

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException();
        }

    }

    @Override
    public void unregisterProvider(String providerId) {
        SamlIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // remove from repository to disable filters
            relyingPartyRegistrationRepository.removeRegistration(providerId);

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
            store = new PersistentAttributeStore(SystemKeys.AUTHORITY_SAML, providerId, jdbcAttributeStore);
        } else if (SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)) {
            store = new InMemoryAttributeStore(SystemKeys.AUTHORITY_SAML, providerId);
        }

        return store;
    }

}
