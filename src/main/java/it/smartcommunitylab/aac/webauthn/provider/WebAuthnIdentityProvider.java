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

package it.smartcommunitylab.aac.webauthn.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.provider.InternalAccountPrincipalConverter;
import it.smartcommunitylab.aac.internal.provider.InternalAccountProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProvider;
import it.smartcommunitylab.aac.internal.provider.InternalUserResolver;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnJpaUserCredentialsService;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.util.Assert;

public class WebAuthnIdentityProvider
    extends AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, WebAuthnUserAuthenticatedPrincipal, WebAuthnIdentityProviderConfigMap, WebAuthnIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final WebAuthnIdentityCredentialsService credentialsService;

    // providers
    private final WebAuthnIdentityAuthenticationProvider authenticationProvider;
    private final InternalAccountPrincipalConverter principalConverter;
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider<WebAuthnUserAuthenticatedPrincipal> attributeProvider;
    private final InternalUserResolver userResolver;

    public WebAuthnIdentityProvider(
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnJpaUserCredentialsService userCredentialsService,
        WebAuthnIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, config, realm);
        Assert.notNull(userCredentialsService, "credentials service is mandatory");

        String repositoryId = config.getRepositoryId();
        logger.debug("create webauthn provider with id {} repository {}", String.valueOf(providerId), repositoryId);

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        this.accountProvider =
            new InternalAccountProvider(
                SystemKeys.AUTHORITY_WEBAUTHN,
                providerId,
                userAccountService,
                repositoryId,
                realm
            );
        this.principalConverter =
            new InternalAccountPrincipalConverter(
                SystemKeys.AUTHORITY_WEBAUTHN,
                providerId,
                userAccountService,
                repositoryId,
                realm
            );

        // build providers
        this.credentialsService =
            new WebAuthnIdentityCredentialsService(
                providerId,
                userAccountService,
                userCredentialsService,
                config,
                realm
            );
        this.authenticationProvider =
            new WebAuthnIdentityAuthenticationProvider(
                providerId,
                userAccountService,
                credentialsService,
                config,
                realm
            );

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.userResolver =
            new InternalUserResolver(
                SystemKeys.AUTHORITY_WEBAUTHN,
                providerId,
                userEntityService,
                userAccountService,
                config.getRepositoryId(),
                config.isLinkable(),
                realm
            );
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.credentialsService.setResourceService(resourceService);
    }

    @Override
    public boolean isAuthoritative() {
        // webauthn handles only login
        return false;
    }

    @Override
    protected AccountService<InternalUserAccount, ?, ?, ?> getAccountService() {
        // no account service available for webauthn, accounts must already exist
        return null;
    }

    @Override
    public WebAuthnIdentityAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    protected InternalAccountPrincipalConverter getAccountPrincipalConverter() {
        return principalConverter;
    }

    @Override
    protected InternalAttributeProvider<WebAuthnUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public InternalUserResolver getUserResolver() {
        return userResolver;
    }

    @Override
    protected InternalUserIdentity buildIdentity(
        InternalUserAccount account,
        WebAuthnUserAuthenticatedPrincipal principal,
        Collection<UserAttributes> attributes
    ) {
        // build identity
        InternalUserIdentity identity = new InternalUserIdentity(
            getAuthority(),
            getProvider(),
            getRealm(),
            account,
            principal
        );
        identity.setAttributes(attributes);

        return identity;
    }

    // @Override
    // public void deleteIdentity(String userId, String username) throws NoSuchUserException {
    //     // // remove all credentials - disabled
    //     // credentialsService.deleteCredentialsByUsername(username);
    //     // do not remove account because we are NOT authoritative
    // }

    @Override
    public String getAuthenticationUrl() {
        // display url for internal form
        return getFormUrl();
    }

    public String getLoginForm() {
        return "webauthn_form";
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    @Override
    public InternalLoginProvider getLoginProvider(ClientDetails clientDetails, AuthorizationRequest authRequest) {
        InternalLoginProvider ilp = new InternalLoginProvider(getProvider(), getRealm(), getName());
        ilp.setTitleMap(getTitleMap());
        ilp.setDescriptionMap(getDescriptionMap());

        // login url is always form display
        ilp.setLoginUrl(getFormUrl());

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        // set position
        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }
}
