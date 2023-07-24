package it.smartcommunitylab.aac.webauthn;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnEditableUserCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsService;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRegistrationRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * WebAuthn service depends on webauthn identity provider
 *
 * every idp will expose a matching service with the same configuration for credentials handling
 */
@Service
public class WebAuthnCredentialsAuthority
    extends AbstractCredentialsAuthority<WebAuthnCredentialsService, WebAuthnUserCredential, WebAuthnEditableUserCredential, WebAuthnIdentityProviderConfigMap, WebAuthnCredentialsServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/webauthn/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private ResourceEntityService resourceService;

    // key repository
    private final WebAuthnUserCredentialsService credentialsService;

    // shared service
    private final WebAuthnRegistrationRpService rpService;

    public WebAuthnCredentialsAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnUserCredentialsService credentialsService,
        WebAuthnRegistrationRpService rpService,
        ProviderConfigRepository<WebAuthnCredentialsServiceConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");

        this.accountService = userAccountService;
        this.credentialsService = credentialsService;
        this.rpService = rpService;
    }

    @Autowired
    public void setConfigProvider(WebAuthnCredentialsConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public WebAuthnCredentialsService buildProvider(WebAuthnCredentialsServiceConfig config) {
        WebAuthnCredentialsService service = new WebAuthnCredentialsService(
            config.getProvider(),
            accountService,
            credentialsService,
            rpService,
            config,
            config.getRealm()
        );

        service.setResourceService(resourceService);

        return service;
    }

    @Override
    public WebAuthnCredentialsServiceConfig registerProvider(ConfigurableProvider cp) {
        throw new IllegalArgumentException("direct registration not supported");
    }
}
