package it.smartcommunitylab.aac.password;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsService;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsServiceConfig;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsServiceConfigMap;
import it.smartcommunitylab.aac.password.service.InternalUserPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class PasswordCredentialsAuthority extends
        AbstractCredentialsAuthority<PasswordCredentialsService, InternalUserPassword, PasswordCredentialsServiceConfigMap, PasswordCredentialsServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/webauthn/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // key repository
    private final InternalUserPasswordService passwordService;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public PasswordCredentialsAuthority(
            UserAccountService<InternalUserAccount> userAccountService,
            InternalUserPasswordService passwordService,
            ProviderConfigRepository<PasswordCredentialsServiceConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_PASSWORD, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");

        this.accountService = userAccountService;
        this.passwordService = passwordService;
    }

    @Autowired
    public void setConfigProvider(PasswordCredentialsConfigurationProvider configProvider) {
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
    public PasswordCredentialsService buildProvider(PasswordCredentialsServiceConfig config) {
        PasswordCredentialsService idp = new PasswordCredentialsService(
                config.getProvider(),
                accountService, passwordService,
                config, config.getRealm());

        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        
        return idp;
    }

}
