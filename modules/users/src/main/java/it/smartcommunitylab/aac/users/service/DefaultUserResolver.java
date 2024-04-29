/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.users.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.users.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.persistence.UserEntityConverter;
import it.smartcommunitylab.aac.users.provider.UserResolver;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class DefaultUserResolver extends AbstractProvider<User> implements UserResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserEntityService userEntityService;
    private Converter<UserEntity, User> userConverter = new UserEntityConverter();
    private boolean createOnMissing = true;

    public DefaultUserResolver(UserEntityService userEntityService, String realm) {
        super(SystemKeys.AUTHORITY_AAC, SystemKeys.AUTHORITY_AAC, realm);
        Assert.notNull(userEntityService, "user service is mandatory");

        this.userEntityService = userEntityService;
    }

    public void setUserConverter(Converter<UserEntity, User> userConverter) {
        Assert.notNull(userConverter, "converter can not be null");
        this.userConverter = userConverter;
    }

    public void setCreateOnMissing(boolean createOnMissing) {
        this.createOnMissing = createOnMissing;
    }

    /*
     * Default resolver for user resources.
     * Look at userId which is *required* for persisted resources.
     * Will create user if not found when flag is set.
     */
    @Override
    public User resolveByAccount(UserAccount a) {
        logger.debug("resolve by account");

        if (a == null || a.getAccountId() == null) {
            logger.debug("unable to resolve by account due to missing properties");
            return null;
        }

        String accountId = a.getAccountId();
        String userId = a.getUserId();
        if (userId == null) {
            if (createOnMissing) {
                //generate new userId
                userId = UUID.randomUUID().toString();
                logger.debug("create new userId");
            } else {
                logger.debug("unable to resolve by account due to missing properties");
                return null;
            }
        }

        logger.debug("resolve by account {}: {}", String.valueOf(accountId), String.valueOf(userId));
        if (!StringUtils.hasText(userId)) {
            logger.debug("unable to resolve by account due to missing properties");
            return null;
        }

        User u = fetchUser(userId);
        if (u == null && createOnMissing) {
            try {
                u = createUser(userId, getRealm(), a.getUsername(), a.getEmailAddress(), a.isEmailVerified());
            } catch (AlreadyRegisteredException e) {
                logger.error("unable to create user {}", e.getMessage());
            }
        }

        return u;
    }

    @Override
    public User resolveByIdentity(UserIdentity i) {
        logger.debug("resolve by identity");

        if (i == null || i.getAccount() == null) {
            logger.debug("unable to resolve by identity due to missing properties");
            return null;
        }

        //use account
        return resolveByAccount(i.getAccount());
    }

    @Override
    public User resolveByPrincipal(UserAuthenticatedPrincipal p) {
        logger.debug("resolve by principal");

        if (p == null || p.getPrincipalId() == null) {
            logger.debug("unable to resolve by principal due to missing properties");
            return null;
        }

        String userId = p.getUserId();
        if (userId == null) {
            if (createOnMissing) {
                //generate new userId
                userId = UUID.randomUUID().toString();
                logger.debug("create new userId");
            } else {
                logger.debug("unable to resolve by principal due to missing properties");
                return null;
            }
        }

        logger.debug("resolve by principal {}: {}", String.valueOf(p.getPrincipalId()), String.valueOf(userId));
        if (!StringUtils.hasText(userId)) {
            logger.debug("unable to resolve by principal due to missing properties");
            return null;
        }

        User u = fetchUser(userId);
        if (u == null && createOnMissing) {
            try {
                u = createUser(userId, getRealm(), p.getUsername(), p.getEmailAddress(), p.isEmailVerified());
            } catch (AlreadyRegisteredException e) {
                logger.error("unable to create user {}", e.getMessage());
            }
        }

        return u;
    }

    /*
     * Helpers
     */
    private User fetchUser(String userId) {
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

    private User createUser(String userId, String realm, String username, String emailAddress, boolean emailVerified)
        throws AlreadyRegisteredException {
        logger.debug("create user {}", String.valueOf(userId));

        UserEntity ue = userEntityService.addUser(userId, realm, username, emailAddress);
        if (emailVerified) {
            try {
                ue = userEntityService.verifyEmail(userId, emailAddress);
            } catch (NoSuchUserException e) {
                //should not happen
                logger.error("error verifying email for user {}", String.valueOf(userId));
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("created user entity {}: {}", String.valueOf(userId), String.valueOf(ue));
        }

        return userConverter.convert(ue);
    }
}
