package it.smartcommunitylab.aac.saml.provider;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

public class SamlIdentityProvider extends
        AbstractIdentityProvider<SamlUserIdentity, SamlUserAccount, SamlUserAuthenticatedPrincipal, SamlIdentityProviderConfigMap, SamlIdentityProviderConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // internal providers
    private final SamlAccountService accountService;
    private final SamlAccountPrincipalConverter principalConverter;
    private final SamlAttributeProvider attributeProvider;
    private final SamlAuthenticationProvider authenticationProvider;
    private final SamlSubjectResolver subjectResolver;

    public SamlIdentityProvider(
            String providerId,
            UserAccountService<SamlUserAccount> userAccountService,
            SamlIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, userAccountService, config, realm);
    }

    public SamlIdentityProvider(
            String authority, String providerId,
            UserAccountService<SamlUserAccount> userAccountService,
            SamlIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, config, realm);

        logger.debug("create saml provider with id {}", String.valueOf(providerId));

        // build resource providers, we use our providerId to ensure consistency
        SamlAccountServiceConfigConverter configConverter = new SamlAccountServiceConfigConverter();
        this.accountService = new SamlAccountService(authority, providerId, userAccountService,
                configConverter.convert(config), realm);
        this.principalConverter = new SamlAccountPrincipalConverter(authority, providerId, userAccountService, config,
                realm);
        this.attributeProvider = new SamlAttributeProvider(authority, providerId, config, realm);
        this.authenticationProvider = new SamlAuthenticationProvider(authority, providerId, userAccountService, config,
                realm);
        this.subjectResolver = new SamlSubjectResolver(authority, providerId, userAccountService, config, realm);

        // function hooks from config
        if (config.getHookFunctions() != null
                && StringUtils.hasText(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {
            this.authenticationProvider
                    .setCustomMappingFunction(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION));
        }
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.authenticationProvider.setExecutionService(executionService);
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.accountService.setResourceService(resourceService);
    }

    @Override
    public SamlAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public AccountProvider<SamlUserAccount> getAccountProvider() {
        return accountService;
    }

    @Override
    public SamlAccountService getAccountService() {
        return accountService;
    }

    @Override
    public SamlAccountPrincipalConverter getAccountPrincipalConverter() {
        return principalConverter;
    }

    @Override
    public SamlAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SamlSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected SamlUserIdentity buildIdentity(SamlUserAccount account, SamlUserAuthenticatedPrincipal principal,
            Collection<UserAttributes> attributes) {
        // build identity
        SamlUserIdentity identity = new SamlUserIdentity(getAuthority(), getProvider(), getRealm(), account, principal);
        identity.setAttributes(attributes);

        return identity;
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return "/auth/" + getAuthority() + "authenticate/" + getProvider();
    }

    @Override
    public SamlLoginProvider getLoginProvider() {
        SamlLoginProvider lp = new SamlLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        lp.setTitleMap(getTitleMap());
        lp.setDescriptionMap(getDescriptionMap());

        lp.setLoginUrl(getAuthenticationUrl());
        lp.setPosition(getConfig().getPosition());

        return lp;
    }
}
