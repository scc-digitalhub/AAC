package it.smartcommunitylab.aac.openid.apple.provider;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountPrincipalConverter;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCAttributeProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCLoginProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCSubjectResolver;

public class AppleIdentityProvider
        extends
        AbstractIdentityProvider<OIDCUserIdentity, OIDCUserAccount, OIDCUserAuthenticatedPrincipal, AppleIdentityProviderConfigMap, AppleIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // providers
    private final AppleAccountService accountService;
    private final OIDCAccountPrincipalConverter principalConverter;
    private final OIDCAttributeProvider attributeProvider;
    private final AppleAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    public AppleIdentityProvider(
            String providerId,
            UserAccountService<OIDCUserAccount> userAccountService,
            AppleIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_APPLE, providerId, config, realm);

        logger.debug("create apple provider  with id {}", String.valueOf(providerId));

        // build resource providers, we use our providerId to ensure consistency
        AppleAccountServiceConfigConverter configConverter = new AppleAccountServiceConfigConverter();
        this.accountService = new AppleAccountService(providerId, userAccountService, configConverter.convert(config),
                realm);

        this.principalConverter = new OIDCAccountPrincipalConverter(SystemKeys.AUTHORITY_APPLE, providerId,
                userAccountService, realm);
        this.principalConverter.setTrustEmailAddress(config.trustEmailAddress());

        this.attributeProvider = new OIDCAttributeProvider(SystemKeys.AUTHORITY_APPLE, providerId, realm);
        this.subjectResolver = new OIDCSubjectResolver(SystemKeys.AUTHORITY_APPLE, providerId, userAccountService,
                config.getRepositoryId(), realm);
        this.subjectResolver.setLinkable(config.isLinkable());

        // build custom authenticator
        this.authenticationProvider = new AppleAuthenticationProvider(providerId, userAccountService, config, realm);

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
    public AppleAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public AccountProvider<OIDCUserAccount> getAccountProvider() {
        return accountService;
    }

    @Override
    public AppleAccountService getAccountService() {
        return accountService;
    }

    @Override
    public OIDCAccountPrincipalConverter getAccountPrincipalConverter() {
        return principalConverter;
    }

    @Override
    public OIDCAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public OIDCSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected OIDCUserIdentity buildIdentity(OIDCUserAccount account, OIDCUserAuthenticatedPrincipal principal,
            Collection<UserAttributes> attributes) {
        // build identity
        OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);
        identity.setAttributes(attributes);

        return identity;
    }

    @Override
    public String getAuthenticationUrl() {
        return "/auth/" + getAuthority() + "/authorize/" + getProvider();
    }

    @Override
    public OIDCLoginProvider getLoginProvider() {
        OIDCLoginProvider lp = new OIDCLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        lp.setTitleMap(getTitleMap());
        lp.setDescriptionMap(getDescriptionMap());

        lp.setLoginUrl(getAuthenticationUrl());

        // explicitly set apple logo as icon
        String icon = "logo-apple";
        String iconUrl = "svg/sprite.svg#" + icon;
        lp.setIcon(icon);
        lp.setIconUrl(iconUrl);

        // set position
        lp.setPosition(getConfig().getPosition());

        return lp;
    }

}
