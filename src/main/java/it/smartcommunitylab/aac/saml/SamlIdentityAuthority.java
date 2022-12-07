package it.smartcommunitylab.aac.saml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.provider.SamlFilterProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityConfigurationProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;

@Service
public class SamlIdentityAuthority extends
        AbstractIdentityAuthority<SamlIdentityProvider, SamlUserIdentity, SamlIdentityProviderConfigMap, SamlIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_SAML + "/";

    // saml account service
    private final UserAccountService<SamlUserAccount> accountService;

    // filter provider
    private final SamlFilterProvider filterProvider;

    // saml sp services
    private final SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    @Autowired
    public SamlIdentityAuthority(
            UserAccountService<SamlUserAccount> userAccountService,
            ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository,
            @Qualifier("samlRelyingPartyRegistrationRepository") SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository) {
        this(SystemKeys.AUTHORITY_SAML, userAccountService, registrationRepository,
                samlRelyingPartyRegistrationRepository);
    }

    public SamlIdentityAuthority(
            String authorityId,
            UserAccountService<SamlUserAccount> userAccountService,
            ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository,
            @Qualifier("samlRelyingPartyRegistrationRepository") SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository) {
        super(authorityId, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(samlRelyingPartyRegistrationRepository, "relayingParty registration repository is mandatory");

        this.accountService = userAccountService;

        this.relyingPartyRegistrationRepository = samlRelyingPartyRegistrationRepository;

        // build filter provider
        this.filterProvider = new SamlFilterProvider(authorityId, relyingPartyRegistrationRepository,
                registrationRepository);
    }

    @Autowired
    public void setConfigProvider(SamlIdentityConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public SamlFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public SamlIdentityProvider buildProvider(SamlIdentityProviderConfig config) {
        String id = config.getProvider();

        SamlIdentityProvider idp = new SamlIdentityProvider(
                authorityId, id,
                accountService,
                config, config.getRealm());

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }

    @Override
    public SamlIdentityProviderConfig registerProvider(ConfigurableProvider cp) throws RegistrationException {
        // register and build via super
        SamlIdentityProviderConfig config = super.registerProvider(cp);

        // fetch id from config
        String providerId = cp.getProvider();

        try {
            // extract clientRegistration from config
            RelyingPartyRegistration registration = config.getRelyingPartyRegistration();

            // add client registration to registry
            relyingPartyRegistrationRepository.addRegistration(registration);

            return config;
        } catch (Exception ex) {
            // cleanup
            relyingPartyRegistrationRepository.removeRegistration(providerId);

            throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
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

}
