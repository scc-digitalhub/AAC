package it.smartcommunitylab.aac.password;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;

import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.provider.PasswordFilterProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityConfigurationProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.service.InternalUserPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class PasswordIdentityAuthority extends
        AbstractIdentityAuthority<PasswordIdentityProvider, InternalUserIdentity, PasswordIdentityProviderConfigMap, PasswordIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/password/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // password service
    private final InternalUserPasswordService passwordService;

    // filter provider
    private final PasswordFilterProvider filterProvider;

    // services
    protected MailService mailService;
    protected RealmAwareUriBuilder uriBuilder;

    public PasswordIdentityAuthority(
            UserAccountService<InternalUserAccount> userAccountService,
            InternalUserPasswordService passwordService,
            ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_PASSWORD, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");

        this.accountService = userAccountService;
        this.passwordService = passwordService;

        // build filter provider
        this.filterProvider = new PasswordFilterProvider(userAccountService, passwordService,
                registrationRepository);
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

    @Override
    public PasswordIdentityProvider buildProvider(PasswordIdentityProviderConfig config) {
        PasswordIdentityProvider idp = new PasswordIdentityProvider(
                config.getProvider(),
                accountService, passwordService,
                config, config.getRealm());

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        return idp;
    }

    @Override
    public PasswordFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
