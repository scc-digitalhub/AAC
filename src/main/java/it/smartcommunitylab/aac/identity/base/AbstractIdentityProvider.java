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

package it.smartcommunitylab.aac.identity.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.identity.provider.IdentityAttributeProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.users.auth.ExtendedAuthenticationProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public abstract class AbstractIdentityProvider<
    I extends AbstractUserIdentity,
    U extends AbstractUserAccount,
    P extends AbstractUserAuthenticatedPrincipal,
    M extends AbstractConfigMap,
    C extends AbstractIdentityProviderConfig<M>
>
    extends AbstractConfigurableResourceProvider<I, C, IdentityProviderSettingsMap, M>
    implements IdentityProvider<I, U, P, M, C>, ApplicationEventPublisherAware, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ApplicationEventPublisher eventPublisher;

    protected AbstractIdentityProvider(String authority, String providerId, C config, String realm) {
        super(authority, providerId, realm, config);
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(authority.equals(config.getAuthority()), "configuration does not match this provider");
        Assert.isTrue(providerId.equals(config.getProvider()), "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        logger.debug(
            "create {} idp for realm {} with id {}",
            String.valueOf(authority),
            String.valueOf(realm),
            String.valueOf(providerId)
        );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getAuthenticationProvider(), "authentication provider is mandatory");
        Assert.notNull(getAccountProvider(), "account provider is mandatory");
        Assert.notNull(getAccountService(), "account service is mandatory");
        Assert.notNull(getAttributeProvider(), "attribute provider is mandatory");
        Assert.notNull(getSubjectResolver(), "subject provider is mandatory");
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public boolean isAuthoritative() {
        // by default every provider is authoritative
        return true;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /*
     * Provider-specific
     */
    @Override
    public abstract ExtendedAuthenticationProvider<P, U> getAuthenticationProvider();

    protected abstract AccountPrincipalConverter<U> getAccountPrincipalConverter();

    /*
     * Account provider acts as the source for user accounts, when the details are
     * persisted in the provider or available for requests. Do note that idps are
     * not required to persist accounts.
     */
    protected abstract AccountProvider<U> getAccountProvider();

    protected abstract AccountService<U, ?, ?, ?> getAccountService();

    /*
     * Attribute providers retrieve and format user properties available to the
     * provider as UserAttributes bounded to the UserIdentity exposed to the outside
     * world.
     */

    protected abstract IdentityAttributeProvider<P, U> getAttributeProvider();

    @Override
    public abstract SubjectResolver<U> getSubjectResolver();

    protected abstract I buildIdentity(U account, P principal, Collection<UserAttributes> attributes);

    protected I buildIdentity(U account, Collection<UserAttributes> attributes) {
        return buildIdentity(account, null, attributes);
    }

    /*
     * Idp
     */

    public I convertIdentity(UserAuthenticatedPrincipal authPrincipal, String userId)
        throws NoSuchUserException, RegistrationException {
        logger.debug("convert principal to identity for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("principal {}", String.valueOf(authPrincipal));
        }

        // cast principal and handle errors
        P principal = null;
        try {
            @SuppressWarnings("unchecked")
            P p = (P) authPrincipal;
            principal = p;
        } catch (ClassCastException e) {
            logger.error("Wrong principal class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported principal");
        }

        // extract local id from principal
        // we expect principalId to be == accountId == identityId
        String id = principal.getPrincipalId();

        if (id == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // TODO evaluate creation of userEntity when empty
        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // base attributes from provider
        String username = principal.getUsername();
        String emailAddress = principal.getEmailAddress();

        logger.debug(
            "principal for {} is {} email {}",
            String.valueOf(userId),
            String.valueOf(username),
            String.valueOf(emailAddress)
        );

        // convert to account
        U reg = getAccountPrincipalConverter().convertAccount(principal, userId);

        if (logger.isTraceEnabled()) {
            logger.trace("converted account: {}", String.valueOf(reg));
        }

        // check matching with principal attributes
        if (username != null && !username.equals(reg.getUsername())) {
            logger.error("username mismatch between principal and account");
            throw new IllegalArgumentException();
        }

        if (emailAddress != null && !emailAddress.equals(reg.getEmailAddress())) {
            logger.error("emailAddress mismatch between principal and account");
            throw new IllegalArgumentException();
        }

        // look in service for existing accounts
        U account = getAccountProvider().findAccount(id);
        //TODO refactor to properly support missing account service and fallback to reg
        if (account == null) {
            // create account if supported
            if (getAccountService() == null) {
                logger.error("no account service available, required for account creation");
                throw new IllegalArgumentException();
            }

            account = getAccountService().createAccount(userId, id, reg);
        } else {
            // check if userId matches
            if (!userId.equals(account.getUserId())) {
                //              // force link
                //              // TODO re-evaluate
                //              account.setSubject(subjectId);
                //              account = accountRepository.save(account);
                throw new IllegalArgumentException("user mismatch");
            }

            // update if supported
            if (getAccountService() != null) {
                logger.debug("update existing account with id {}", String.valueOf(id));
                account = getAccountService().updateAccount(userId, id, reg);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("persisted account: {}", String.valueOf(account));
        }

        // uuid is available for persisted accounts
        String uuid = account.getUuid();
        // // set uuid on principal when possible - DISABLED, not needed
        // if (principal instanceof AbstractUserAuthenticatedPrincipal) {
        //     ((AbstractUserAuthenticatedPrincipal) principal).setUuid(uuid);
        // }

        // convert attribute sets via provider, will update store
        logger.debug("convert principal and account to attributes via provider for {}", String.valueOf(id));
        Collection<UserAttributes> attributes = getAttributeProvider().convertPrincipalAttributes(principal, account);
        if (logger.isTraceEnabled()) {
            logger.trace("identity attributes: {}", String.valueOf(attributes));
        }

        // build identity
        logger.debug("build identity for user {} from account {}", String.valueOf(userId), String.valueOf(id));
        I identity = buildIdentity(account, principal, attributes);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    //    @Override
    //    @Transactional(readOnly = true)
    //    public I findIdentityByUuid(String userId, String uuid) {
    //        logger.debug("find identity for uuid {}", String.valueOf(uuid));
    //
    //        // lookup a matching account
    //        U account = getAccountProvider().findAccountByUuid(uuid);
    //        if (account == null) {
    //            return null;
    //        }
    //
    //        // check userId matches
    //        if (!account.getUserId().equals(userId)) {
    //            return null;
    //        }
    //
    //        // build identity without attributes or principal
    //        I identity = buildIdentity(account, null);
    //        if (logger.isTraceEnabled()) {
    //            logger.trace("identity: {}", String.valueOf(identity));
    //        }
    //
    //        return identity;
    //    }

    @Override
    @Transactional(readOnly = true)
    public I findIdentity(String userId, String accountId) {
        logger.debug("find identity for id {}", String.valueOf(accountId));

        // lookup a matching account
        U account = getAccountProvider().findAccount(accountId);
        if (account == null) {
            return null;
        }

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            return null;
        }

        // build identity without attributes or principal
        I identity = buildIdentity(account, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public I getIdentity(String userId, String accountId) throws NoSuchUserException {
        return getIdentity(userId, accountId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public I getIdentity(String userId, String accountId, boolean fetchAttributes) throws NoSuchUserException {
        logger.debug(
            "get identity for id {} user {} with attributes {}",
            String.valueOf(accountId),
            String.valueOf(userId),
            String.valueOf(fetchAttributes)
        );

        // lookup a matching account
        U account = getAccountProvider().getAccount(accountId);

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user mismatch");
        }

        Collection<UserAttributes> attributes = null;
        if (fetchAttributes) {
            // convert attribute sets and fetch from repo
            attributes = getAttributeProvider().getAccountAttributes(account);
        }

        // use builder to properly map attributes
        I identity = buildIdentity(account, attributes);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<I> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<I> listIdentities(String userId, boolean fetchAttributes) {
        logger.debug(
            "list identities for user {} attributes {}",
            String.valueOf(userId),
            String.valueOf(fetchAttributes)
        );

        // lookup for matching accounts
        Collection<U> accounts = getAccountProvider().listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<I> identities = new ArrayList<>();
        for (U account : accounts) {
            Collection<UserAttributes> attributes = null;
            if (fetchAttributes) {
                // convert attribute sets and fetch from repo
                attributes = getAttributeProvider().getAccountAttributes(account);
            }

            I identity = buildIdentity(account, attributes);
            if (logger.isTraceEnabled()) {
                logger.trace("identity: {}", String.valueOf(identity));
            }

            identities.add(identity);
        }

        return identities;
    }

    @Override
    @Transactional(readOnly = false)
    public I linkIdentity(String userId, String accountId) throws NoSuchUserException, RegistrationException {
        logger.debug("link identity with id {} to user {}", String.valueOf(accountId), String.valueOf(userId));

        // get the internal account entity
        U account = getAccountProvider().getAccount(accountId);

        if (isAuthoritative()) {
            // re-link to new userId
            account = getAccountProvider().linkAccount(accountId, userId);
        }

        // use builder, skip attributes
        I identity = buildIdentity(account, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String userId, String accountId) throws NoSuchUserException {
        logger.debug("delete identity with id {} for user {}", String.valueOf(accountId), String.valueOf(userId));

        // delete account
        // authoritative deletes the registration with shared accounts
        U account = getAccountProvider().findAccount(accountId);
        if (account != null && isAuthoritative()) {
            // check userId matches
            if (!account.getUserId().equals(userId)) {
                throw new IllegalArgumentException("user mismatch");
            }

            // remove account
            getAccountProvider().deleteAccount(accountId);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
        logger.debug("delete identities for user {}", String.valueOf(userId));

        Collection<U> accounts = getAccountProvider().listAccounts(userId);
        for (U account : accounts) {
            try {
                deleteIdentity(userId, account.getAccountId());
            } catch (NoSuchUserException e) {}
        }
    }
}
