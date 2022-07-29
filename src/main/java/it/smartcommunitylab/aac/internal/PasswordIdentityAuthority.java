package it.smartcommunitylab.aac.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.provider.PasswordIdentityConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.PasswordIdentityService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class PasswordIdentityAuthority
        extends
        AbstractInternalIdentityAuthority<PasswordIdentityService, PasswordIdentityProviderConfig, PasswordIdentityProviderConfigMap> {

    public static final String AUTHORITY_URL = "/auth/password/";

    // internal persistence service
    private final InternalUserPasswordRepository passwordRepository;

    // services
    protected MailService mailService;
    protected RealmAwareUriBuilder uriBuilder;

    public PasswordIdentityAuthority(
            InternalUserAccountService userAccountService, InternalUserPasswordRepository passwordRepository,
            UserEntityService userEntityService, SubjectService subjectService,
            ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository) {
        super(userAccountService, userEntityService, subjectService, registrationRepository);
        Assert.notNull(passwordRepository, "password repository is mandatory");
        this.passwordRepository = passwordRepository;
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
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_PASSWORD;
    }

    @Override
    public PasswordIdentityService build(PasswordIdentityProviderConfig config) {
        PasswordIdentityService idp = new PasswordIdentityService(
                config.getProvider(),
                userAccountService, userEntityService, subjectService,
                passwordRepository,
                config, config.getRealm());

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        return idp;
    }

}
