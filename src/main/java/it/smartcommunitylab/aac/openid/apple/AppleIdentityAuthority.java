package it.smartcommunitylab.aac.openid.apple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.openid.apple.provider.AppleFilterProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractSingleProviderIdentityAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Service
public class AppleIdentityAuthority extends
        AbstractSingleProviderIdentityAuthority<AppleIdentityProvider, OIDCUserIdentity, AppleIdentityProviderConfigMap, AppleIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_APPLE + "/";

    // oidc account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // filter provider
    private final AppleFilterProvider filterProvider;

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    public AppleIdentityAuthority(
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
            @Qualifier("appleClientRegistrationRepository") OIDCClientRegistrationRepository clientRegistrationRepository) {
        super(SystemKeys.AUTHORITY_APPLE, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.accountService = userAccountService;
        this.clientRegistrationRepository = clientRegistrationRepository;

        // build filter provider
        this.filterProvider = new AppleFilterProvider(clientRegistrationRepository,
                registrationRepository);
    }

    @Autowired
    public void setConfigProvider(AppleIdentityConfigurationProvider configProvider) {
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
    public AppleFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public AppleIdentityProvider buildProvider(AppleIdentityProviderConfig config) {
        String id = config.getProvider();

        AppleIdentityProvider idp = new AppleIdentityProvider(
                id,
                accountService,
                config, config.getRealm());

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);

        return idp;
    }

    @Override
    public AppleIdentityProviderConfig registerProvider(ConfigurableProvider cp) throws RegistrationException {
        // register and build via super
        AppleIdentityProviderConfig config = super.registerProvider(cp);

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
        AppleIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

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
