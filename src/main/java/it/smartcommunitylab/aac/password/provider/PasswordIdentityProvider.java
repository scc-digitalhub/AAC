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

package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.provider.InternalAccountPrincipalConverter;
import it.smartcommunitylab.aac.internal.provider.InternalAccountProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProvider;
import it.smartcommunitylab.aac.internal.provider.InternalSubjectResolver;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.InternalPasswordUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.password.service.InternalPasswordJpaUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class PasswordIdentityProvider
    extends AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, InternalPasswordUserAuthenticatedPrincipal, PasswordIdentityProviderConfigMap, PasswordIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final PasswordIdentityCredentialsService passwordService;

    // providers
    private final PasswordAuthenticationProvider authenticationProvider;
    private final InternalAccountPrincipalConverter principalConverter;
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider<InternalPasswordUserAuthenticatedPrincipal> attributeProvider;
    private final InternalSubjectResolver subjectResolver;

    public PasswordIdentityProvider(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService userPasswordService,
        PasswordIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, config, realm);
        Assert.notNull(userPasswordService, "password service is mandatory");

        String repositoryId = config.getRepositoryId();
        logger.debug("create password provider with id {} repository {}", String.valueOf(providerId), repositoryId);

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_PASSWORD, providerId, realm);
        this.accountProvider =
            new InternalAccountProvider(
                SystemKeys.AUTHORITY_PASSWORD,
                providerId,
                userAccountService,
                repositoryId,
                realm
            );
        this.principalConverter =
            new InternalAccountPrincipalConverter(
                SystemKeys.AUTHORITY_PASSWORD,
                providerId,
                userAccountService,
                repositoryId,
                realm
            );

        // build providers
        this.passwordService =
            new PasswordIdentityCredentialsService(providerId, userAccountService, userPasswordService, config, realm);
        this.authenticationProvider =
            new PasswordAuthenticationProvider(providerId, userAccountService, passwordService, config, realm);

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, repositoryId, false, realm);
    }

    public void setMailService(MailService mailService) {
        this.passwordService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.passwordService.setUriBuilder(uriBuilder);
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.passwordService.setResourceService(resourceService);
    }

    @Override
    public boolean isAuthoritative() {
        // password handles only login
        return false;
    }

    @Override
    protected AccountService<InternalUserAccount, ?, ?, ?> getAccountService() {
        // no account service available for password, accounts must already exist
        return null;
    }

    @Override
    public PasswordAuthenticationProvider getAuthenticationProvider() {
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
    protected InternalAttributeProvider<InternalPasswordUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public InternalSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    public PasswordIdentityCredentialsService getCredentialsService() {
        return passwordService;
    }

    @Override
    protected InternalUserIdentity buildIdentity(
        InternalUserAccount account,
        InternalPasswordUserAuthenticatedPrincipal principal,
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

        // // if attributes then load credentials
        // if (attributes != null) {
        //     try {
        //         List<InternalUserPassword> passwords = passwordService.findPassword(account.getUsername());
        //         passwords.forEach(p -> p.eraseCredentials());
        //         identity.setCredentials(passwords);
        //     } catch (NoSuchUserException e) {
        //         // this should not happen
        //         logger.error("no user for account {}", String.valueOf(account.getUsername()));
        //     }
        // }

        return identity;
    }

    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException {
        // do not remove account because we are NOT authoritative
    }

    @Override
    public String getAuthenticationUrl() {
        // display url for internal form
        return getFormUrl();
    }

    /*
     * Action urls
     */
    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return PasswordIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return PasswordIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    public String getResetUrl() {
        return PasswordIdentityAuthority.AUTHORITY_URL + "reset/" + getProvider();
    }

    public String getLoginForm() {
        return "password_form";
    }

    @Override
    public InternalLoginProvider getLoginProvider() {
        InternalLoginProvider ilp = new InternalLoginProvider(getProvider(), getRealm(), getName());
        ilp.setTitleMap(getTitleMap());
        ilp.setDescriptionMap(getDescriptionMap());

        // login url is always form display
        ilp.setLoginUrl(getFormUrl());
        //        ilp.setRegistrationUrl(getRegistrationUrl());
        ilp.setResetUrl(getResetUrl());

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        // set position
        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }
}
