package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collection;

// TODO: review what class we are inheriting from
public class SpidIdentityProvider extends AbstractIdentityProvider<SpidUserIdentity, SpidUserAccount, SpidUserAuthenticatedPrincipal, SpidIdentityProviderConfigMap, SpidIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // internal providers
    private final SpidAccountService accountService;
    private final SpidAccountPrincipalConverter principalConverter;
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
        SpidAccountServiceConfigConverter configConverter = new SpidAccountServiceConfigConverter();
        this.accountService = new SpidAccountService(authority, providerId, userAccountService, configConverter.convert(config), realm);
        this.principalConverter = new SpidAccountPrincipalConverter(authority, providerId, config, realm);
        this.attributeProvider = new SpidAttributeProvider(authority, providerId, config, realm);
        this.authenticationProvider = new SpidAuthenticationProvider(
            providerId,
            userAccountService, // TODO: remove? currently unused by AuthnProvider
            config,
            realm
        );
        this.subjectResolver = new SpidSubjectResolver(authority, providerId, userAccountService, config, realm);

        // function hooks from config
        if (config.getHookFunctions() != null) {
            if (StringUtils.hasText(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {
                this.authenticationProvider.setCustomMappingFunction(
                        config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION)
                );
            }
            if (StringUtils.hasText(config.getHookFunctions().get(AUTHORIZATION_FUNCTION))) {
                this.authenticationProvider.setCustomAuthFunction(
                        config.getHookFunctions().get(AUTHORIZATION_FUNCTION)
                );
            }
        }
    }


    @Override
    public SpidAuthenticationProvider getAuthenticationProvider() {
        return this.authenticationProvider;
    }

    @Override
    protected AccountPrincipalConverter<SpidUserAccount> getAccountPrincipalConverter() {
        return this.principalConverter;
    }

    @Override
    protected AccountProvider<SpidUserAccount> getAccountProvider() {
        return this.accountService;
    }

    @Override
    protected SpidAccountService getAccountService() {
        return this.accountService;
    }

    @Override
    protected SpidAttributeProvider getAttributeProvider() {
        return this.attributeProvider;
    }

    @Override
    public SubjectResolver<SpidUserAccount> getSubjectResolver() {
        return this.subjectResolver;
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return "/auth/" + getAuthority() + "/authenticate/" + getProvider();
    }

    @Override
    public SpidLoginProvider getLoginProvider() {
        return new SpidLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
    }

    @Override
    protected SpidUserIdentity buildIdentity(SpidUserAccount account, SpidUserAuthenticatedPrincipal principal, Collection<UserAttributes> attributes) {
        SpidUserIdentity identity = new SpidUserIdentity(getAuthority(), getProvider(), getRealm(), account, principal);
        identity.setAttributes(attributes);
        return identity;
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.authenticationProvider.setExecutionService(executionService);
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.accountService.setResourceService(resourceService);
    }
}
