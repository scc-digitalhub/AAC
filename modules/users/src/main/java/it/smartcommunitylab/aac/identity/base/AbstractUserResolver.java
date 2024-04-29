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

import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.persistence.UserEntityConverter;
import it.smartcommunitylab.aac.users.provider.UserResolver;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public abstract class AbstractUserResolver<
    I extends AbstractUserIdentity, U extends AbstractUserAccount, P extends AbstractUserAuthenticatedPrincipal
>
    extends AbstractProvider<User>
    implements UserResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserEntityService userEntityService;
    protected final UserAccountService<U> accountService;
    protected final String repositoryId;
    protected boolean resolve = false;

    protected Set<String> resolvableFields = Collections.emptySet();
    private Converter<UserEntity, User> userConverter = new UserEntityConverter();

    protected AbstractUserResolver(
        String authority,
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<U> userAccountService,
        String repositoryId,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is required");

        this.userEntityService = userEntityService;
        this.accountService = userAccountService;
        this.repositoryId = repositoryId;
    }

    public void setUserConverter(Converter<UserEntity, User> userConverter) {
        Assert.notNull(userConverter, "converter can not be null");
        this.userConverter = userConverter;
    }

    public void setResolvableFields(Set<String> resolvableFields) {
        Assert.notNull(resolvableFields, "resolvableFields can not be null");
        this.resolvableFields = resolvableFields;
    }

    public void setResolve(boolean value) {
        this.resolve = value;
    }

    /*
     * Core resolver for persisted accounts.
     * Look at id which is *required* for persisted accounts.
     */
    public User resolveByAccountId(String accountId) {
        logger.debug("resolve by accountId {}", String.valueOf(accountId));
        U account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            return null;
        }

        return fetchUser(account.getUserId());
    }

    public User resolveByPrincipalId(String principalId) {
        // principalId is accountId
        return resolveByAccountId(principalId);
    }

    public User resolveByIdentityId(String identityId) {
        // identityId is accountId
        return resolveByAccountId(identityId);
    }

    /*
     * Core attributes resolvers.
     * Fetch a list of matching users, only if resolve is enabled
     */

    @Transactional(readOnly = true)
    public Collection<User> resolveByUsername(String username) {
        if (!resolve) {
            return Collections.emptyList();
        }

        logger.debug("resolve by username {}", String.valueOf(username));

        //find accounts
        List<U> accounts = accountService.findAccountsByUsername(repositoryId, username);
        if (logger.isTraceEnabled()) {
            logger.trace("accounts found for username {}: {}", String.valueOf(username), accounts);
        }

        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        // fetch users
        return accounts.stream().map(a -> fetchUser(a.getUserId())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Collection<User> resolveByEmailAddress(String email) {
        if (!resolve) {
            return Collections.emptyList();
        }

        logger.debug("resolve by email {}", String.valueOf(email));

        //find accounts
        List<U> accounts = accountService.findAccountsByEmail(repositoryId, email);
        if (logger.isTraceEnabled()) {
            logger.trace("accounts found for email {}: {}", String.valueOf(email), accounts);
        }

        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        // fetch users
        return accounts.stream().map(a -> fetchUser(a.getUserId())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Collection<User> resolveByAttributes(Map<String, Serializable> map) {
        if (!resolve) {
            return Collections.emptyList();
        }

        if (map == null) {
            return null;
        }

        logger.debug("resolve by attributes {}", String.valueOf(map.keySet()));

        //find accounts by comparing attributes
        //NOTE: expensive full-scan search, could leverage some index/cache for pruning
        List<U> accounts = accountService
            .findAccounts(repositoryId)
            .stream()
            .filter(a -> anyMatch(map, a.getAttributes()))
            .collect(Collectors.toList());

        if (logger.isTraceEnabled()) {
            logger.trace("accounts found for attributes {}: {}", String.valueOf(map.keySet()), accounts);
        }

        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        // fetch users
        return accounts.stream().map(a -> fetchUser(a.getUserId())).collect(Collectors.toList());
    }

    /*
     * Helpers
     */

    protected User fetchUser(String userId) {
        logger.debug("fetch user {}", String.valueOf(userId));
        UserEntity ue = userEntityService.findUser(userId);
        if (ue == null) {
            return null;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("found user entity {}: {}", String.valueOf(userId), String.valueOf(ue));
        }

        return userConverter.convert(ue);
    }

    protected boolean anyMatch(Map<String, Serializable> map, Map<String, Serializable> attributes) {
        if (map == null || attributes == null) {
            return false;
        }

        //@formatter:off
        if (attributes.entrySet()
                //read only resolvable fields as per config
                .stream().filter(e -> resolvableFields.contains(e.getKey()))
                //any *exact* match on input is valid
                //TODO relax matching 
                //TODO evaluate doing a string-based comparison
                .anyMatch(
                    e -> map.keySet().contains(e.getKey()) && map.get(e.getKey()).equals(e.getValue())
                )) {
            //at least one matching key+value
            return true;
        }
        //@formatter:on

        return false;
    }
}
