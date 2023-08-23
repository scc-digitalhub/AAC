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
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.provider.CredentialsService;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.identity.provider.IdentityService;
import it.smartcommunitylab.aac.internal.InternalAccountServiceAuthority;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class InternalIdentityService
    extends AbstractConfigurableResourceProvider<InternalUserIdentity, ConfigurableIdentityService, InternalIdentityProviderConfigMap, InternalIdentityServiceConfig>
    implements
        IdentityService<InternalUserIdentity, InternalUserAccount, InternalEditableUserAccount, InternalIdentityProviderConfigMap, InternalIdentityServiceConfig>,
        InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider authorities
    private InternalAccountServiceAuthority accountServiceAuthority;
    private Map<String, CredentialsServiceAuthority<?, ?, ?, ?, ?>> credentialsServiceAuthorities;

    // services
    private final UserEntityService userEntityService;

    public InternalIdentityService(
        String providerId,
        UserEntityService userEntityService,
        InternalIdentityServiceConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm, providerConfig);
        Assert.notNull(userEntityService, "user entity service is mandatory");

        this.userEntityService = userEntityService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(accountServiceAuthority, "account service authority is mandatory");
    }

    public void setAccountServiceAuthority(InternalAccountServiceAuthority accountServiceAuthority) {
        this.accountServiceAuthority = accountServiceAuthority;
    }

    public void setCredentialsServiceAuthorities(
        Collection<CredentialsServiceAuthority<?, ?, ?, ?, ?>> credentialsServiceAuthorities
    ) {
        if (credentialsServiceAuthorities != null) {
            this.credentialsServiceAuthorities =
                credentialsServiceAuthorities.stream().collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));
        }
    }

    @Override
    public InternalAccountService getAccountService() throws NoSuchProviderException {
        // pick from realm, we expect single
        // the service should match our repo id
        InternalAccountService service = accountServiceAuthority
            .getProvidersByRealm(getRealm())
            .stream()
            .filter(p -> p.getConfig().getRepositoryId().equals(config.getRepositoryId()))
            .findFirst()
            .orElse(null);

        if (service == null) {
            throw new NoSuchProviderException();
        }

        return service;
    }

    @Override
    public Collection<CredentialsService<?, ?, ?, ?>> getCredentialsServices() {
        if (credentialsServiceAuthorities == null) {
            return Collections.emptyList();
        }

        // pick from realm, we expect single but multiple works
        // every service should match our repo id
        return credentialsServiceAuthorities
            .values()
            .stream()
            .flatMap(a ->
                a
                    .getProvidersByRealm(getRealm())
                    .stream()
                    .filter(p -> p.getConfig().getRepositoryId().equals(config.getRepositoryId()))
            )
            .collect(Collectors.toList());
    }

    @Override
    public CredentialsService<?, ?, ?, ?> getCredentialsService(String authority)
        throws NoSuchProviderException {
        CredentialsService<?, ?, ?, ?> cs = getCredentialsServices()
            .stream()
            .filter(s -> s.getAuthority().equals(authority))
            .findFirst()
            .orElse(null);

        return cs;
    }

    protected InternalUserIdentity buildIdentity(InternalUserAccount account, Collection<UserCredentials> credentials) {
        // build identity
        InternalUserIdentity identity = new InternalUserIdentity(
            getAuthority(),
            getProvider(),
            getRealm(),
            account,
            null
        );
        identity.setCredentials(credentials);

        return identity;
    }

    @Override
    public InternalUserIdentity findIdentity(String userId, String username) {
        logger.debug("find identity for id {} user {}", String.valueOf(username), String.valueOf(userId));

        // lookup a matching account
        InternalUserAccount account = null;
        try {
            account = getAccountService().findAccount(username);
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        if (account == null) {
            return null;
        }

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            return null;
        }

        // build identity without attributes or principal
        InternalUserIdentity identity = buildIdentity(account, Collections.emptyList());
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    public InternalUserIdentity getIdentity(String userId, String username) throws NoSuchUserException {
        logger.debug("get identity for id {} user {}", String.valueOf(username), String.valueOf(userId));

        InternalUserIdentity identity = findIdentity(userId, username);
        if (identity == null) {
            throw new NoSuchUserException();
        }

        return identity;
    }

    @Override
    public InternalUserIdentity getIdentity(String userId, String username, boolean loadCredentials)
        throws NoSuchUserException {
        logger.debug("get identity for id {} user {}", String.valueOf(username), String.valueOf(userId));

        InternalUserIdentity identity = findIdentity(userId, username);
        if (identity == null) {
            throw new NoSuchUserException();
        }

        if (loadCredentials) {
            List<UserCredentials> credentials = new ArrayList<>();
            getCredentialsServices()
                .stream()
                .forEach(s -> {
                    credentials.addAll(s.listCredentialsByUser(username));
                });

            identity.setCredentials(credentials);
        }

        return identity;
    }

    @Override
    public Collection<InternalUserIdentity> listIdentities(String userId) {
        logger.debug("list identities for user {}", String.valueOf(userId));

        // lookup for matching accounts
        Collection<InternalUserAccount> accounts = null;
        try {
            accounts = getAccountService().listAccounts(userId);
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<InternalUserIdentity> identities = new ArrayList<>();
        for (InternalUserAccount account : accounts) {
            InternalUserIdentity identity = buildIdentity(account, null);
            if (logger.isTraceEnabled()) {
                logger.trace("identity: {}", String.valueOf(identity));
            }

            identities.add(identity);
        }

        return identities;
    }

    @Override
    public InternalUserIdentity registerIdentity(@Nullable String userId, UserIdentity registration)
        throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        InternalAccountService service = null;
        try {
            service = getAccountService();
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        if (service == null || registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalUserIdentity.class,
            registration,
            "registration must be an instance of internal user identity"
        );
        InternalUserIdentity reg = (InternalUserIdentity) registration;

        UserEntity user = null;
        InternalUserAccount account = null;
        List<UserCredentials> credentials = new ArrayList<>();
        boolean isNew = false;

        try {
            // check if user exists
            if (userId != null) {
                user = userEntityService.findUser(userId);
                isNew = user == null;
            }

            // register account via service
            // build editable model
            InternalEditableUserAccount ea = new InternalEditableUserAccount(
                getProvider(),
                getRealm(),
                reg.getAccount().getUserId(),
                null
            );
            ea.setUsername(reg.getAccount().getUsername());
            ea.setEmail(reg.getAccount().getEmail());
            ea.setName(reg.getAccount().getName());
            ea.setSurname(reg.getAccount().getSurname());
            ea.setLang(reg.getAccount().getLang());
            ea = service.registerAccount(userId, ea);

            account = service.getAccount(ea.getAccountId());
            userId = account.getUserId();
            String username = account.getUsername();

            // register credentials
            if (reg.getCredentials() != null) {
                for (UserCredentials uc : reg.getCredentials()) {
                    CredentialsService<?, ?, ?, ?> cs = getCredentialsServices()
                        .stream()
                        .filter(a -> a.getAuthority().equals(uc.getAuthority()))
                        .findFirst()
                        .orElse(null);
                    if (cs != null) {
                        // add as new
                        credentials.add(cs.addCredential(username, null, uc));
                    }
                }
            }

            InternalUserIdentity identity = buildIdentity(account, credentials);
            return identity;
        } catch (RegistrationException e) {
            // cleanup all new entities on error
            if (account != null) {
                service.deleteAccount(userId);

                if (isNew) {
                    // created by account service as new, delete orphan
                    userEntityService.deleteUser(userId);
                }
            }

            for (UserCredentials uc : credentials) {
                CredentialsService<?, ?, ?, ?> cs = getCredentialsServices()
                    .stream()
                    .filter(a -> a.getAuthority().equals(uc.getAuthority()))
                    .findFirst()
                    .orElse(null);
                if (cs != null) {
                    try {
                        // remove
                        cs.deleteCredential(uc.getId());
                    } catch (NoSuchCredentialException e1) {
                        // ignore
                    }
                }
            }

            throw e;
        }
    }

    @Override
    public InternalUserIdentity createIdentity(@Nullable String userId, UserIdentity registration)
        throws NoSuchUserException, RegistrationException {
        // create is always enabled
        if (registration == null) {
            throw new RegistrationException();
        }

        InternalAccountService service = null;
        try {
            service = getAccountService();
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        if (service == null || registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalUserIdentity.class,
            registration,
            "registration must be an instance of internal user identity"
        );
        InternalUserIdentity reg = (InternalUserIdentity) registration;

        // create account via service
        InternalUserAccount account = service.createAccount(userId, null, reg.getAccount());
        String username = account.getUsername();

        // register credentials
        List<UserCredentials> credentials = new ArrayList<>();
        if (reg.getCredentials() != null) {
            for (UserCredentials uc : reg.getCredentials()) {
                CredentialsService<?, ?, ?, ?> cs = getCredentialsServices()
                    .stream()
                    .filter(a -> a.getAuthority().equals(uc.getAuthority()))
                    .findFirst()
                    .orElse(null);
                if (cs != null) {
                    credentials.add(cs.addCredential(username, uc.getCredentialsId(), uc));
                }
            }
        }

        InternalUserIdentity identity = buildIdentity(account, credentials);
        return identity;
    }

    @Override
    public InternalUserIdentity updateIdentity(String userId, String username, UserIdentity registration)
        throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        InternalAccountService service = null;
        try {
            service = getAccountService();
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        if (service == null || registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalUserIdentity.class,
            registration,
            "registration must be an instance of internal user identity"
        );
        InternalUserIdentity reg = (InternalUserIdentity) registration;

        if (reg.getAccount() == null) {
            throw new MissingDataException("account");
        }

        // get the internal account entity
        InternalUserAccount account = service.getAccount(username);

        // check if userId matches account
        if (!account.getUserId().equals(userId)) {
            throw new RegistrationException("userid-mismatch");
        }

        // update account
        account = service.updateAccount(userId, username, reg.getAccount());

        // store and update attributes
        // we shouldn't have additional attributes for internal

        // we skip update of credentials, do it via service directly

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);

        // no attribute sets
        return identity;
    }

    @Override
    public void deleteIdentities(String userId) {
        logger.debug("delete identities for user {}", String.valueOf(userId));
        List<String> usernames = Collections.emptyList();
        try {
            InternalAccountService service = getAccountService();
            usernames = service.listAccounts(userId).stream().map(i -> i.getUsername()).collect(Collectors.toList());

            service.deleteAccounts(userId);
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        // remove credentials
        getCredentialsServices().forEach(c -> c.deleteCredentialsByUser(userId));
    }

    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException, RegistrationException {
        logger.debug("delete identity with id {} for user {}", String.valueOf(username), String.valueOf(userId));
        try {
            InternalAccountService service = getAccountService();
            service.deleteAccount(username);
        } catch (NoSuchProviderException e) {
            logger.error("account provider unavailable");
        }

        // remove credentials matching account

        getCredentialsServices()
            .forEach(c -> {
                List<UserCredentials> creds = c
                    .listCredentialsByUser(userId)
                    .stream()
                    .filter(s -> s.getAccountId().equals(username))
                    .collect(Collectors.toList());
                creds.forEach(s -> {
                    try {
                        c.deleteCredential(s.getCredentialsId());
                    } catch (NoSuchCredentialException e) {}
                });
            });
    }

    @Override
    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return "/auth/internal/register/" + getProvider();
    }
}
