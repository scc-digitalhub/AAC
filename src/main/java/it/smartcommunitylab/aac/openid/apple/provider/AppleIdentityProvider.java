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
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCAttributeProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCLoginProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCSubjectResolver;

public class AppleIdentityProvider
        extends
        AbstractIdentityProvider<OIDCUserIdentity, OIDCUserAccount, OIDCUserAuthenticatedPrincipal, AppleIdentityProviderConfigMap, AppleIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final AppleIdentityProviderConfig config;

    // providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final AppleAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    public AppleIdentityProvider(
            String providerId,
            UserAccountService<OIDCUserAccount> userAccountService,
            AttributeStore attributeStore,
            AppleIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_APPLE, providerId, userAccountService, config, realm);
        Assert.notNull(attributeStore, "attribute store is mandatory");

        logger.debug("create apple provider  with id {}", String.valueOf(providerId));
        this.config = config;

        OIDCIdentityProviderConfig oidcConfig = config.toOidcProviderConfig();

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new OIDCAccountProvider(SystemKeys.AUTHORITY_APPLE, providerId, userAccountService,
                oidcConfig, realm);
        this.attributeProvider = new OIDCAttributeProvider(SystemKeys.AUTHORITY_APPLE, providerId, attributeStore,
                oidcConfig, realm);
        this.subjectResolver = new OIDCSubjectResolver(SystemKeys.AUTHORITY_APPLE, providerId, userAccountService,
                oidcConfig, realm);

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

    @Override
    public AppleIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public AppleAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public OIDCAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public OIDCAccountProvider getAccountPrincipalConverter() {
        return accountProvider;
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
        lp.setDescription(getDescription());
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
