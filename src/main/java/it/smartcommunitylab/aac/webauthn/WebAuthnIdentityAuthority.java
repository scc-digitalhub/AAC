package it.smartcommunitylab.aac.webauthn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityFilterProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityConfigurationProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnLoginRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;

@Service
public class WebAuthnIdentityAuthority extends
        AbstractIdentityAuthority<WebAuthnIdentityProvider, InternalUserIdentity, WebAuthnIdentityProviderConfigMap, WebAuthnIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/webauthn/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // key repository
    private final WebAuthnUserCredentialsService credentialsService;

    // filter provider
    private final WebAuthnIdentityFilterProvider filterProvider;

    public WebAuthnIdentityAuthority(
            UserAccountService<InternalUserAccount> userAccountService,
            WebAuthnUserCredentialsService credentialsService,
            WebAuthnLoginRpService rpService, WebAuthnAssertionRequestStore requestStore,
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");
        Assert.notNull(requestStore, "webauthn request store is mandatory");

        this.accountService = userAccountService;
        this.credentialsService = credentialsService;

        // build filter provider
        this.filterProvider = new WebAuthnIdentityFilterProvider(rpService, registrationRepository, requestStore);
    }

    @Autowired
    public void setConfigProvider(WebAuthnIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public WebAuthnIdentityProvider buildProvider(WebAuthnIdentityProviderConfig config) {
        WebAuthnIdentityProvider idp = new WebAuthnIdentityProvider(
                config.getProvider(),
                accountService, credentialsService,
                config, config.getRealm());

        return idp;
    }

    @Override
    public WebAuthnIdentityFilterProvider getFilterProvider() {
        return filterProvider;
    }

}
