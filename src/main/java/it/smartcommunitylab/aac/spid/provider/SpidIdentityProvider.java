/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.util.StringUtils;

// TODO: review what class we are inheriting from
public class SpidIdentityProvider
    extends AbstractIdentityProvider<SpidUserIdentity, SpidUserAccount, SpidUserAuthenticatedPrincipal, SpidIdentityProviderConfigMap, SpidIdentityProviderConfig> {

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
        this.accountService =
            new SpidAccountService(authority, providerId, userAccountService, configConverter.convert(config), realm);
        this.principalConverter = new SpidAccountPrincipalConverter(authority, providerId, config, realm);
        this.attributeProvider = new SpidAttributeProvider(authority, providerId, config, realm);
        this.authenticationProvider =
            new SpidAuthenticationProvider(
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
        SpidLoginProvider lp = new SpidLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        // bind identity provider configs to login provider
        lp.setTitleMap(getTitleMap()); // TODO: Remove? Localization and customization might not be supported for SPID buttons
        lp.setDescriptionMap(getDescriptionMap());
        lp.setPosition(getConfig().getPosition());

        lp.setLoginUrl(getAuthenticationUrl());
        List<SpidLoginProvider.SpidIdpButton> spidIdpsLogin = new LinkedList<>();
        for (RelyingPartyRegistration reg : config.getRelyingPartyRegistrations()) {
            // TODO: should be exposes by a dedicated method

            String loginUrl = getAuthenticationUrl() + "|" + reg.getRegistrationId();

            SpidRegistration spidReg = getConfig()
                .getIdentityProviders()
                .get(reg.getAssertingPartyDetails().getEntityId());

            spidIdpsLogin.add(
                new SpidLoginProvider.SpidIdpButton(
                    reg.getAssertingPartyDetails().getEntityId(),
                    spidReg.getEntityName(),
                    spidReg.getMetadataUrl(),
                    spidReg.getEntityLabel(),
                    spidReg.getIconUrl(),
                    loginUrl
                )
            );
        }
        // for (SpidRegistration reg : getConfig().getIdentityProviders().values()) {
        //     String loginUrl;
        //     try {
        //         // TODO: should be exposes by a dedicated method
        //         loginUrl = getAuthenticationUrl() + "-" + getConfig().evalIdpKeyIdentifier(reg.getMetadataUrl());
        //     } catch (URISyntaxException e) {
        //         throw new IllegalArgumentException(
        //             "invalid url when producing an upstream login url for " +
        //             reg.getEntityName() +
        //             ", " +
        //             e.getMessage()
        //         );
        //     }
        //     spidIdpsLogin.add(
        //         new SpidLoginProvider.SpidIdpButton(
        //             reg.getEntityId(),
        //             reg.getEntityName(),
        //             reg.getMetadataUrl(),
        //             reg.getEntityLabel(),
        //             reg.getIconUrl(),
        //             loginUrl
        //         )
        //     );
        // }
        lp.setIdpButtons(spidIdpsLogin);
        return lp;
    }

    @Override
    protected SpidUserIdentity buildIdentity(
        SpidUserAccount account,
        SpidUserAuthenticatedPrincipal principal,
        Collection<UserAttributes> attributes
    ) {
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
