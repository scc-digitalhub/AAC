package it.smartcommunitylab.aac.otp;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsAuthority;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import it.smartcommunitylab.aac.internal.service.InternalJpaUserAccountService;
import it.smartcommunitylab.aac.otp.model.InternalEditableUserOtp;
import it.smartcommunitylab.aac.otp.model.InternalUserOtp;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntityRepository;
import it.smartcommunitylab.aac.otp.provider.OtpCredentialsService;
import it.smartcommunitylab.aac.otp.provider.OtpCredentialsServiceConfig;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProviderConfig;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProviderConfigMap;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import it.smartcommunitylab.aac.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class OtpCredentialsAuthority
    extends AbstractCredentialsAuthority<
        OtpCredentialsService,
        InternalUserOtp,
        InternalEditableUserOtp,
        OtpCredentialsServiceConfig,
        OtpIdentityProviderConfigMap
    > {

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private UserEntityService userService;
    private ResourceEntityService resourceService;
    private final InternalJpaUserAccountService accountService;
    private final UserCredentialsService<InternalUserOtp> credentialsService;
    private final InternalUserOtpEntityRepository otpRepository;

    public OtpCredentialsAuthority(
        ProviderConfigRepository<OtpIdentityProviderConfig> registrationRepository,
        InternalJpaUserAccountService accountService,
        UserCredentialsService<InternalUserOtp> credentialsService,
        InternalUserOtpEntityRepository otpRepository
    ) {
        super(SystemKeys.AUTHORITY_OTP, new OtpConfigTranslatorRepository(registrationRepository));
        Assert.notNull(accountService, "accountService is mandatory");
        Assert.notNull(otpRepository, "otpRepository is mandatory");

        this.accountService = accountService;
        this.otpRepository = otpRepository;
        this.credentialsService = credentialsService;
    }

    @Autowired
    public void setUserService(UserEntityService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
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
    public OtpCredentialsService buildProvider(OtpCredentialsServiceConfig config) {
        OtpCredentialsService service = new OtpCredentialsService(
            config.getProvider(),
            credentialsService,
            accountService,
            config,
            otpRepository,
            config.getRealm()
        );

        service.setMailService(mailService);
        service.setUriBuilder(uriBuilder);
        service.setUserService(userService);
        service.setResourceService(resourceService);

        return service;
    }

    static class OtpConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<OtpIdentityProviderConfig, OtpCredentialsServiceConfig> {

        public OtpConfigTranslatorRepository(ProviderConfigRepository<OtpIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(source -> {
                OtpCredentialsServiceConfig config = new OtpCredentialsServiceConfig(
                    source.getProvider(),
                    source.getRealm()
                );
                config.setName(source.getName());
                config.setTitleMap(source.getTitleMap());
                config.setDescriptionMap(source.getDescriptionMap());

                // we share the same configMap
                config.setConfigMap(source.getConfigMap());
                config.setVersion(source.getVersion());

                // build new settingsMap
                CredentialsServiceSettingsMap settingsMap = new CredentialsServiceSettingsMap();
                settingsMap.setRepositoryId(source.getRepositoryId());
                config.setSettingsMap(settingsMap);

                return config;
            });
        }
    }
}
