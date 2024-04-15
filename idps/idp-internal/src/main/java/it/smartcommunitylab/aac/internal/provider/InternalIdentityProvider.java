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

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.LoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.users.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

public class InternalIdentityProvider
    extends AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, InternalUserAuthenticatedPrincipal, InternalIdentityProviderConfigMap, InternalIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final InternalIdentityConfirmService confirmService;

    // providers
    private final InternalAuthenticationProvider authenticationProvider;
    private final InternalAccountPrincipalConverter principalConverter;
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider<InternalUserAuthenticatedPrincipal> attributeProvider;
    private final InternalUserResolver userResolver;

    public InternalIdentityProvider(
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalUserConfirmKeyService confirmKeyService,
        InternalIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, config, realm);
        Assert.notNull(confirmKeyService, "account confirm service is mandatory");

        String repositoryId = config.getRepositoryId();
        logger.debug("create internal provider with id {} repository {}", String.valueOf(providerId), repositoryId);

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new InternalAccountProvider(providerId, userAccountService, repositoryId, realm);
        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        this.principalConverter =
            new InternalAccountPrincipalConverter(providerId, userAccountService, repositoryId, realm);

        // build providers
        this.confirmService =
            new InternalIdentityConfirmService(providerId, userAccountService, confirmKeyService, config, realm);
        this.authenticationProvider =
            new InternalAuthenticationProvider(providerId, userAccountService, confirmService, config, realm);

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.userResolver = new InternalUserResolver(providerId, userEntityService, userAccountService, config, realm);
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.accountProvider.setResourceService(resourceService);
    }

    @Override
    public boolean isAuthoritative() {
        return true;
    }

    @Override
    protected InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    protected InternalAccountService getAccountService() {
        return null;
    }

    @Override
    protected InternalAccountPrincipalConverter getAccountPrincipalConverter() {
        return principalConverter;
    }

    @Override
    public InternalAttributeProvider<InternalUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public InternalUserResolver getUserResolver() {
        return userResolver;
    }

    @Override
    public InternalAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    protected InternalUserIdentity buildIdentity(
        InternalUserAccount account,
        InternalUserAuthenticatedPrincipal principal,
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

    // TODO remove and set accountProvider read-only (and let create fail in super)
    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity convertIdentity(UserAuthenticatedPrincipal authPrincipal, String userId)
        throws NoSuchUserException {
        Assert.isInstanceOf(InternalUserAuthenticatedPrincipal.class, authPrincipal, "Wrong principal class");
        logger.debug("convert principal to identity for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("principal {}", String.valueOf(authPrincipal));
        }

        InternalUserAuthenticatedPrincipal principal = (InternalUserAuthenticatedPrincipal) authPrincipal;

        // username binds all identity pieces together
        String username = principal.getUsername();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // get the internal account entity
        InternalUserAccount account = accountProvider.findAccount(username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // uuid is available for persisted accounts
        String uuid = account.getUuid();
        // principal.setUuid(uuid);

        // userId is always present, is derived from the same account table
        String curUserId = account.getUserId();

        if (!curUserId.equals(userId)) {
            //            // force link
            //            // TODO re-evaluate
            //            account.setSubject(subjectId);
            //            account = accountRepository.save(account);
            throw new IllegalArgumentException("user mismatch");
        }

        // store and update attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(
            getAuthority(),
            getProvider(),
            getRealm(),
            account,
            principal
        );

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(
            principal,
            account
        );
        identity.setAttributes(identityAttributes);

        return identity;
    }

    // @Override
    // public void deleteIdentity(String userId, String username) throws NoSuchUserException {
    //     // call super to remove account
    //     super.deleteIdentity(userId, username);
    // }

    @Override
    public String getAuthenticationUrl() {
        // not available
        return null;
    }

    @Override
    public LoginProvider getLoginProvider(ClientDetails clientDetails, AuthorizationRequest authRequest) {
        // no direct login available
        return null;
    }
}
