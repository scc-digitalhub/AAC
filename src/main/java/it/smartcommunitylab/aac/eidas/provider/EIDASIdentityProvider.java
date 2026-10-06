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

package it.smartcommunitylab.aac.eidas.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.eidas.model.EIDASUserIdentity;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.provider.SpidLoginProvider;
import it.smartcommunitylab.aac.spid.provider.SpidSubjectResolver;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.util.StringUtils;

/*
 * eIDAS identity provider.
 *
 * The italian eIDAS node is a SPID identity provider: authentication, attribute mapping, account
 * persistence and subject resolution are the SPID ones, specialized here only for the authority.
 *
 * The difference with SPID is the topology: SPID talks to the whole registry of italian idps and
 * renders a page with one button per idp, eIDAS talks to the single node declared in the
 * configuration, so the login button points straight at the authentication endpoint and no
 * intermediate form is needed.
 */
public class EIDASIdentityProvider
    extends AbstractIdentityProvider<EIDASUserIdentity, SamlUserAccount, SpidUserAuthenticatedPrincipal, SpidIdentityProviderConfigMap, EIDASIdentityProviderConfig> {

    public static final String LOGIN_TEMPLATE = "button-eidas";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // internal providers, inherited from spid
    private final EIDASAccountService accountService;
    private final EIDASAccountPrincipalConverter principalConverter;
    private final EIDASAttributeProvider attributeProvider;
    private final EIDASAuthenticationProvider authenticationProvider;
    private final SpidSubjectResolver subjectResolver;

    public EIDASIdentityProvider(
        String providerId,
        UserAccountService<SamlUserAccount> userAccountService,
        EIDASIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_EIDAS, providerId, config, realm);
        logger.debug("create eidas provider with id {}", providerId);

        this.accountService = new EIDASAccountService(providerId, userAccountService, config, realm);
        // the converter is the spid one corrected for eidas (name/surname from the Minimum Data
        // Set, email never verified); the subject resolver never exposes an authority and returns
        // a Subject, so the spid one is reused as it is - same as password/webauthn do with internal
        this.principalConverter = new EIDASAccountPrincipalConverter(providerId, config, realm);
        this.attributeProvider = new EIDASAttributeProvider(providerId, config, realm);
        this.authenticationProvider = new EIDASAuthenticationProvider(providerId, userAccountService, config, realm);
        this.subjectResolver = new SpidSubjectResolver(providerId, userAccountService, config, realm);

        // function hooks from config
        if (config.getHookFunctions() != null) {
            if (StringUtils.hasText(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {
                this.authenticationProvider.setCustomMappingFunction(
                        config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION)
                    );
            }
            if (StringUtils.hasText(config.getHookFunctions().get(AUTHORIZATION_FUNCTION))) {
                this.authenticationProvider.setCustomAuthFunction(config.getHookFunctions().get(AUTHORIZATION_FUNCTION));
            }
        }
    }

    @Override
    public EIDASAuthenticationProvider getAuthenticationProvider() {
        return this.authenticationProvider;
    }

    @Override
    protected AccountPrincipalConverter<SamlUserAccount> getAccountPrincipalConverter() {
        return this.principalConverter;
    }

    @Override
    protected AccountProvider<SamlUserAccount> getAccountProvider() {
        return this.accountService;
    }

    @Override
    protected EIDASAccountService getAccountService() {
        return this.accountService;
    }

    @Override
    protected EIDASAttributeProvider getAttributeProvider() {
        return this.attributeProvider;
    }

    @Override
    public SubjectResolver<SamlUserAccount> getSubjectResolver() {
        return this.subjectResolver;
    }

    /*
     * a single upstream node: no selection page, the url is the authentication endpoint of the node
     */
    @Override
    public String getAuthenticationUrl() {
        return buildAuthenticationUrl(nodeRegistrationId());
    }

    public String buildAuthenticationUrl(String registrationId) {
        return "/auth/" + getAuthority() + "/authenticate/" + registrationId;
    }

    private String nodeRegistrationId() {
        SpidRegistration node = getNode();
        String plainRegistrationId = node != null
            ? getConfig().evalRelyingPartyRegistrationId(node.getEntityId())
            : getProvider();

        return SpidIdentityProviderConfig.encodeRegistrationId(plainRegistrationId);
    }

    private SpidRegistration getNode() {
        return getConfig().getIdentityProviders().stream().findFirst().orElse(null);
    }

    @Override
    public SpidLoginProvider getLoginProvider(ClientDetails clientDetails, AuthorizationRequest authRequest) {
        SpidLoginProvider lp = new SpidLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        // same model as spid (a list of upstream idp buttons, a single one here), own template
        lp.setTemplate(LOGIN_TEMPLATE);
        lp.setTitleMap(getTitleMap());
        lp.setDescriptionMap(getDescriptionMap());
        lp.setPosition(getConfig().getPosition());
        lp.setLoginUrl(getAuthenticationUrl());

        List<SpidLoginProvider.SpidIdpButton> buttons = new LinkedList<>();
        for (SpidRegistration node : getConfig().getIdentityProviders()) {
            String registrationId = SpidIdentityProviderConfig.encodeRegistrationId(
                getConfig().evalRelyingPartyRegistrationId(node.getEntityId())
            );

            buttons.add(
                new SpidLoginProvider.SpidIdpButton(
                    node.getEntityId(),
                    node.getEntityName(),
                    node.getMetadataUrl(),
                    node.getEntityLabel(),
                    node.getIconUrl(),
                    buildAuthenticationUrl(registrationId)
                )
            );
        }
        lp.setIdpButtons(buttons);

        return lp;
    }

    @Override
    protected EIDASUserIdentity buildIdentity(
        SamlUserAccount account,
        SpidUserAuthenticatedPrincipal principal,
        Collection<UserAttributes> attributes
    ) {
        EIDASUserIdentity identity = new EIDASUserIdentity(
            getAuthority(),
            getProvider(),
            getRealm(),
            account,
            principal
        );
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
