package it.smartcommunitylab.aac.otp;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.otp.provider.OtpFilterProvider;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityConfigurationProvider;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProvider;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProviderConfig;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProviderConfigMap;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class OtpIdentityAuthority
    extends AbstractIdentityProviderAuthority<
        OtpIdentityProvider,
        InternalUserIdentity,
        OtpIdentityProviderConfig,
        OtpIdentityProviderConfigMap
    > {

    public static final String AUTHORITY_URL = "/auth/otp/";

    private final UserAccountService<InternalUserAccount> accountService;

    private final OtpFilterProvider filterProvider;

    private RealmService realmService;
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public OtpIdentityAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        ProviderConfigRepository<OtpIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_OTP, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
        this.filterProvider = new OtpFilterProvider(userAccountService, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(OtpIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
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
    public OtpIdentityProvider buildProvider(OtpIdentityProviderConfig config) {
        OtpIdentityProvider idp = new OtpIdentityProvider(
            config.getProvider(),
            accountService,
            config,
            config.getRealm()
        );

        idp.setRealmService(realmService);
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);

        return idp;
    }

    @Override
    public OtpFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
