package it.smartcommunitylab.aac.openid.apple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.base.AbstractProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountService;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountServiceConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountServiceConfigConverter;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Service
public class AppleAccountServiceAuthority
        extends
        AbstractProviderAuthority<AppleAccountService, OIDCUserAccount, ConfigurableAccountProvider, AppleIdentityProviderConfigMap, AppleAccountServiceConfig>
        implements
        AccountServiceAuthority<AppleAccountService, OIDCUserAccount, AbstractEditableAccount, AppleIdentityProviderConfigMap, AppleAccountServiceConfig> {

    // account service
    private final UserAccountService<OIDCUserAccount> accountService;
    private ResourceEntityService resourceService;

    public AppleAccountServiceAuthority(
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_APPLE, new AppleConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected AppleAccountService buildProvider(AppleAccountServiceConfig config) {
        AppleAccountService service = new AppleAccountService(
                config.getProvider(),
                accountService,
                config, config.getRealm());
        service.setResourceService(resourceService);

        return service;
    }

    static class AppleConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<AppleIdentityProviderConfig, AppleAccountServiceConfig> {

        public AppleConfigTranslatorRepository(
                ProviderConfigRepository<AppleIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(new AppleAccountServiceConfigConverter());
        }

    }
}
