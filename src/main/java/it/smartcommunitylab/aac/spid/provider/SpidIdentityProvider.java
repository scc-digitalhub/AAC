package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.identity.provider.IdentityAttributeProvider;
import it.smartcommunitylab.aac.identity.provider.LoginProvider;
import it.smartcommunitylab.aac.saml.provider.SamlAccountPrincipalConverter;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

// TODO: review what class we are inheriting from
public class SpidIdentityProvider extends AbstractIdentityProvider<SpidUserIdentity, SpidUserAccount, SpidUserAuthenticatedPrincipal, SpidIdentityProviderConfigMap, SpidIdentityProviderConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // internal providers
    private final SpidAccountService accountService;
    private final SamlAccountPrincipalConverter principalConverter;
    private final SpidAttributeProvider attributeProvider;
    private final SpidAuthenticationProvider authenticationProvider;
    private final SpidSubjectResolver subjectResolver;

    public SpidIdentityProvider(
        String providerId,
        UserAccountService<SpidUserAccount> userAccountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SPID, providerId, userAccountService, config, realm);
    }

    public SpidIdentityProvider(
        String authority,
        String providerId,
        UserAccountService<SpidUserAccount> userAccountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, config, realm);
        logger.debug("create spid provider with id {}", providerId);
        this.accountService = null;
        this.principalConverter = null;
        this.attributeProvider = null;
        this.authenticationProvider = null;
        this.subjectResolver = null;
        // TODO
//        SpidAccountServiceConfig accountServiceConfig = new SpidAccountServiceConfigConverter().convert(config);
//        this.accountService = new SpidAccountService(authority, providerId, userAccountService, accountServiceConfig, realm);

    }


    @Override
    public ExtendedAuthenticationProvider<SpidUserAuthenticatedPrincipal, SpidUserAccount> getAuthenticationProvider() {
        // TODO
        return null;
    }

    @Override
    protected AccountPrincipalConverter<SpidUserAccount> getAccountPrincipalConverter() {
        // TODO
        return null;
    }

    @Override
    protected AccountProvider<SpidUserAccount> getAccountProvider() {
        // TODO
        return null;
    }

    @Override
    protected AccountService<SpidUserAccount, ?, ?, ?> getAccountService() {
        // TODO
        return null;
    }

    @Override
    protected IdentityAttributeProvider<SpidUserAuthenticatedPrincipal, SpidUserAccount> getAttributeProvider() {
        // TODO
        return null;
    }

    @Override
    public SubjectResolver<SpidUserAccount> getSubjectResolver() {
        // TODO
        return null;
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO
        return null;
    }

    @Override
    public LoginProvider getLoginProvider() {
        // TODO
        return null;
    }

    @Override
    protected SpidUserIdentity buildIdentity(SpidUserAccount account, SpidUserAuthenticatedPrincipal principal, Collection<UserAttributes> attributes) {
        // TODO
        return null;
    }
}
