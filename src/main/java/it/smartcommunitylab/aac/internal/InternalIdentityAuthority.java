package it.smartcommunitylab.aac.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class InternalIdentityAuthority
        extends
        AbstractIdentityAuthority<InternalUserIdentity, InternalIdentityService, InternalIdentityProviderConfig, InternalIdentityProviderConfigMap>
        implements IdentityServiceAuthority<InternalUserIdentity, InternalUserAccount, InternalIdentityService>,
        InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal account service
    private final InternalUserAccountService accountService;
    // services
    protected MailService mailService;
    protected RealmAwareUriBuilder uriBuilder;

    public InternalIdentityAuthority(
            UserEntityService userEntityService, SubjectService subjectService,
            InternalUserAccountService userAccountService, InternalUserPasswordRepository passwordRepository,
            ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository) {
        super(userEntityService, subjectService, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(passwordRepository, "password repository is mandatory");

        this.accountService = userAccountService;
    }

    @Autowired
    public void setConfigProvider(InternalIdentityConfigurationProvider configProvider) {
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
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public InternalIdentityService buildProvider(InternalIdentityProviderConfig config) {
        InternalIdentityService idp = new InternalIdentityService(
                config.getProvider(),
                userEntityService, accountService, subjectService,
                config, config.getRealm());

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        return idp;
    }

}
