package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.model.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.provider.*;
import liquibase.pro.packaged.P;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

@Service
public class SpidIdentityAuthority
    extends AbstractIdentityProviderAuthority<SpidIdentityProvider, SpidUserIdentity, SpidIdentityProviderConfig, SpidIdentityProviderConfigMap>
    implements ApplicationEventPublisherAware {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_SPID + "/";
    private final UserAccountService<SpidUserAccount> accountService;
    private final SpidRelyingPartyRegistrationRepository registrationRepository;
    private final SpidFilterProvider filterProvider; // TODO: a cosa serve?

    public SpidIdentityAuthority(
        String authorityId,
        UserAccountService<SpidUserAccount> accountService,
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        super(authorityId, providerConfigRepository);
        this.accountService = accountService;
        this.registrationRepository = new SpidRelyingPartyRegistrationRepository(providerConfigRepository);
        this.filterProvider = new SpidFilterProvider(authorityId, registrationRepository, providerConfigRepository);
    }

    @Autowired
    public SpidIdentityAuthority(
        UserAccountService<SpidUserAccount> accountService,
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        this(SystemKeys.AUTHORITY_SPID, accountService, providerConfigRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Autowired
    public void setConfigProvider(SpidIdentityConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }


    @Override
    protected SpidIdentityProvider buildProvider(SpidIdentityProviderConfig config) {
        String id = config.getProvider();
        SpidIdentityProvider idp = new SpidIdentityProvider(authorityId, id, accountService, config, config.getRealm());
        // TODO: more setters
        return idp;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        // TODO
    }
}
