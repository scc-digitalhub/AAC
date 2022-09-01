package it.smartcommunitylab.aac.password;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.provider.InternalPasswordFilterProvider;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityConfigurationProvider;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProvider;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class InternalPasswordIdentityAuthority extends
        AbstractIdentityAuthority<InternalUserIdentity, InternalPasswordIdentityProvider, InternalPasswordIdentityProviderConfig, InternalPasswordIdentityProviderConfigMap>
        implements InitializingBean {

    public static final String AUTHORITY_URL = "/auth/password/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // TODO remove
    private final InternalUserConfirmKeyService confirmKeyService;

    // password repository
    private final InternalUserPasswordRepository passwordRepository;

    // filter provider
    private final InternalPasswordFilterProvider filterProvider;

    // services
    protected MailService mailService;
    protected RealmAwareUriBuilder uriBuilder;

    public InternalPasswordIdentityAuthority(
            UserEntityService userEntityService, SubjectService subjectService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            InternalUserPasswordRepository passwordRepository,
            ProviderConfigRepository<InternalPasswordIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_PASSWORD, userEntityService, subjectService, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");
        Assert.notNull(passwordRepository, "password repository is mandatory");

        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
        this.passwordRepository = passwordRepository;

        // build filter provider
        this.filterProvider = new InternalPasswordFilterProvider(userAccountService, confirmKeyService,
                passwordRepository,
                registrationRepository);
    }

    @Autowired
    public void setConfigProvider(InternalPasswordIdentityConfigurationProvider configProvider) {
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
    public InternalPasswordIdentityProvider buildProvider(InternalPasswordIdentityProviderConfig config) {
        InternalPasswordIdentityProvider idp = new InternalPasswordIdentityProvider(
                config.getProvider(),
                userEntityService,
                accountService, confirmKeyService, subjectService,
                passwordRepository,
                config, config.getRealm());

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        return idp;
    }

    @Override
    public InternalPasswordFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
