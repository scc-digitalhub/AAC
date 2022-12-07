package it.smartcommunitylab.aac.openid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
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
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCFilterProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

@Service
public class OIDCIdentityAuthority extends
        AbstractIdentityAuthority<OIDCIdentityProvider, OIDCUserIdentity, OIDCIdentityProviderConfigMap, OIDCIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_OIDC + "/";

    // oidc account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // filter provider
    private final OIDCFilterProvider filterProvider;

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    @Autowired
    public OIDCIdentityAuthority(
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository,
            @Qualifier("oidcClientRegistrationRepository") OIDCClientRegistrationRepository clientRegistrationRepository) {
        this(SystemKeys.AUTHORITY_OIDC, userAccountService, registrationRepository,
                clientRegistrationRepository);
    }

    public OIDCIdentityAuthority(
            String authorityId,
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {
        super(authorityId, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.accountService = userAccountService;
        this.clientRegistrationRepository = clientRegistrationRepository;

        // build filter provider
        this.filterProvider = new OIDCFilterProvider(authorityId, clientRegistrationRepository,
                registrationRepository);
    }

    @Autowired
    public void setConfigProvider(OIDCIdentityConfigurationProvider configProvider) {
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
    public OIDCFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public OIDCIdentityProvider buildProvider(OIDCIdentityProviderConfig config) {
        String id = config.getProvider();

        OIDCIdentityProvider idp = new OIDCIdentityProvider(
                authorityId, id,
                accountService,
                config, config.getRealm());

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }

    @Override
    public OIDCIdentityProviderConfig registerProvider(ConfigurableProvider cp) throws RegistrationException {
        // register and build via super
        OIDCIdentityProviderConfig config = super.registerProvider(cp);

        // fetch id from config
        String providerId = cp.getProvider();
        try {
            // extract clientRegistration from config
            ClientRegistration registration = config.getClientRegistration();

            // add client registration to registry
            clientRegistrationRepository.addRegistration(registration);

            return config;
        } catch (Exception ex) {
            // cleanup
            clientRegistrationRepository.removeRegistration(providerId);

            throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
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

}
