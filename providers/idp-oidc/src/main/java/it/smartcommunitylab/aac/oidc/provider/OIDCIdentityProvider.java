/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oidc.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.oidc.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Transactional
public class OIDCIdentityProvider
    extends AbstractIdentityProvider<
        OIDCUserIdentity,
        OIDCUserAccount,
        OIDCUserAuthenticatedPrincipal,
        OIDCIdentityProviderConfigMap,
        OIDCIdentityProviderConfig
    > {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // providers
    private final OIDCAccountService accountService;
    private final OIDCAccountPrincipalConverter principalConverter;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCUserResolver userResolver;

    public OIDCIdentityProvider(
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<OIDCUserAccount> userAccountService,
        OIDCIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, userEntityService, userAccountService, config, realm);
    }

    public OIDCIdentityProvider(
        String authority,
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<OIDCUserAccount> userAccountService,
        OIDCIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, config, realm);
        logger.debug(
            "create oidc provider for authority {} with id {}",
            String.valueOf(authority),
            String.valueOf(providerId)
        );

        // build resource providers, we use our providerId to ensure consistency
        OIDCAccountServiceConfigConverter configConverter = new OIDCAccountServiceConfigConverter();
        this.accountService = new OIDCAccountService(
            authority,
            providerId,
            userAccountService,
            configConverter.convert(config),
            realm
        );

        this.principalConverter = new OIDCAccountPrincipalConverter(authority, providerId, userAccountService, realm);
        this.principalConverter.setTrustEmailAddress(config.trustEmailAddress());
        this.principalConverter.setAlwaysTrustEmailAddress(config.alwaysTrustEmailAddress());

        this.attributeProvider = new OIDCAttributeProvider(authority, providerId, realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(
            authority,
            providerId,
            userAccountService,
            config,
            realm
        );

        this.userResolver = new OIDCUserResolver(
            authority,
            providerId,
            userEntityService,
            userAccountService,
            config,
            realm
        );

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

    public void setExecutionService(ScriptExecutionService executionService) {
        this.authenticationProvider.setExecutionService(executionService);
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.accountService.setResourceService(resourceService);
    }

    @Override
    public OIDCAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public AccountProvider<OIDCUserAccount> getAccountProvider() {
        return accountService;
    }

    @Override
    protected OIDCAccountService getAccountService() {
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
    public OIDCUserResolver getUserResolver() {
        return userResolver;
    }

    @Override
    protected OIDCUserIdentity buildIdentity(
        OIDCUserAccount account,
        OIDCUserAuthenticatedPrincipal principal,
        Collection<UserAttributes> attributes
    ) {
        // build identity
        OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account, principal);
        identity.setAttributes(attributes);

        return identity;
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        // TODO move url build to helper class
        return "/auth/" + getAuthority() + "/authorize/" + getProvider();
    }

    @Override
    public OIDCLoginProvider getLoginProvider(ClientDetails clientDetails, AuthorizationRequest authRequest) {
        OIDCLoginProvider lp = new OIDCLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        lp.setTitleMap(getTitleMap());
        lp.setDescriptionMap(getDescriptionMap());

        lp.setLoginUrl(getAuthenticationUrl());
        lp.setPosition(getConfig().getPosition());

        //template override
        //TODO validate against supported
        if (StringUtils.hasText(config.getSettingsMap().getTemplate())) {
            lp.setTemplate(config.getSettingsMap().getTemplate());
        }

        return lp;
    }
}
