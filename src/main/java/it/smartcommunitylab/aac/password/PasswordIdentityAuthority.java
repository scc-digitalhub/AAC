package it.smartcommunitylab.aac.password;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.provider.PasswordFilterProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityConfigurationProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class PasswordIdentityAuthority
    extends AbstractIdentityAuthority<PasswordIdentityProvider, InternalUserIdentity, PasswordIdentityProviderConfigMap, PasswordIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/password/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // password service
    private final InternalPasswordUserCredentialsService passwordService;

    // filter provider
    private final PasswordFilterProvider filterProvider;

    // services
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private ResourceEntityService resourceService;

    public PasswordIdentityAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordUserCredentialsService passwordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");

        this.accountService = userAccountService;
        this.passwordService = passwordService;

        // build filter provider
        this.filterProvider = new PasswordFilterProvider(userAccountService, passwordService, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(PasswordIdentityConfigurationProvider configProvider) {
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

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public PasswordIdentityProvider buildProvider(PasswordIdentityProviderConfig config) {
        PasswordIdentityProvider idp = new PasswordIdentityProvider(
            config.getProvider(),
            accountService,
            passwordService,
            config,
            config.getRealm()
        );

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        idp.setResourceService(resourceService);

        return idp;
    }

    @Override
    public PasswordFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
