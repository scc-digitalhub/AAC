package it.smartcommunitylab.aac.webauthn;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnFilterProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityConfigurationProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;

@Service
public class WebAuthnIdentityAuthority extends
        AbstractIdentityAuthority<InternalUserIdentity, WebAuthnIdentityProvider, WebAuthnIdentityProviderConfig, WebAuthnIdentityProviderConfigMap>
        implements InitializingBean {

    public static final String AUTHORITY_URL = "/auth/webauthn/";

    // internal account service
    private final InternalUserAccountService accountService;

    // key repository
    private final WebAuthnCredentialsRepository credentialsRepository;

    // filter provider
    private final WebAuthnFilterProvider filterProvider;

    // services
    protected MailService mailService;
    protected RealmAwareUriBuilder uriBuilder;

    public WebAuthnIdentityAuthority(
            UserEntityService userEntityService, SubjectService subjectService,
            InternalUserAccountService userAccountService, WebAuthnCredentialsRepository credentialsRepository,
            WebAuthnRpService rpService, WebAuthnAssertionRequestStore requestStore,
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, userEntityService, subjectService, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");
        Assert.notNull(requestStore, "webauthn request store is mandatory");

        this.accountService = userAccountService;
        this.credentialsRepository = credentialsRepository;

        // build filter provider
        this.filterProvider = new WebAuthnFilterProvider(rpService, registrationRepository, requestStore);
    }

    @Autowired
    public void setConfigProvider(WebAuthnIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Autowired
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public WebAuthnIdentityProvider buildProvider(WebAuthnIdentityProviderConfig config) {
        WebAuthnIdentityProvider idp = new WebAuthnIdentityProvider(
                config.getProvider(),
                userEntityService, accountService, subjectService,
                credentialsRepository,
                config, config.getRealm());

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        return idp;
    }

    @Override
    public WebAuthnFilterProvider getFilterProvider() {
        return filterProvider;
    }

}
