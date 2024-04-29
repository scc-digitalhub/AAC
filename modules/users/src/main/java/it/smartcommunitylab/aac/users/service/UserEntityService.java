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

package it.smartcommunitylab.aac.users.service;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.users.model.UserStatus;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.persistence.UserEntityRepository;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * Manage persistence for user entities and authorities (roles)
 */
@Service
@Transactional
public class UserEntityService {

    private final UserEntityRepository userRepository;

    public UserEntityService(UserEntityRepository userRepository) {
        Assert.notNull(userRepository, "user repository is mandatory");

        this.userRepository = userRepository;
    }

    public UserEntity createUser(String realm) {
        String id = generateUuid();
        return new UserEntity(id, realm);
    }

    public UserEntity addUser(String uuid, String realm, String username, String emailAddress)
        throws AlreadyRegisteredException {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            throw new AlreadyRegisteredException();
        }

        // create user
        u = new UserEntity(uuid, realm);
        u.setUsername(username);
        u.setEmailAddress(emailAddress);
        // ensure user is active
        u.setStatus(UserStatus.ACTIVE.getValue());

        u = userRepository.save(u);
        return u;
    }

    @Transactional(readOnly = true)
    public UserEntity findUser(String uuid) {
        return userRepository.findByUuid(uuid);
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(String uuid) throws NoSuchUserException {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u == null) {
            throw new NoSuchUserException("no user for subject " + String.valueOf(uuid));
        }

        return u;
    }

    @Transactional(readOnly = true)
    public long countUsers(String realm) {
        return userRepository.countByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> listUsers(String realm) {
        return userRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findUsersByUsername(String realm, String username) {
        return userRepository.findByRealmAndUsername(realm, username);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findUsersByEmailAddress(String realm, String emailAddress) {
        return userRepository.findByRealmAndEmailAddress(realm, emailAddress);
    }

    @Transactional(readOnly = true)
    public Page<UserEntity> searchUsers(String realm, String q, Pageable pageRequest) {
        Page<UserEntity> page = StringUtils.hasText(q)
            ? userRepository.findByRealmAndUsernameContainingIgnoreCaseOrRealmAndUuidContainingIgnoreCaseOrRealmAndEmailAddressContainingIgnoreCase(
                realm,
                q,
                realm,
                q,
                realm,
                q,
                pageRequest
            )
            : userRepository.findByRealm(realm.toLowerCase(), pageRequest);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<UserEntity> searchUsersWithSpec(Specification<UserEntity> spec, Pageable pageRequest) {
        Page<UserEntity> page = userRepository.findAll(spec, pageRequest);
        return page;
    }

    public UserEntity updateUser(String uuid, String username, String emailAddress) throws NoSuchUserException {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u == null) {
            throw new NoSuchUserException("no user for subject " + uuid);
        }

        u.setUsername(username);
        u.setEmailAddress(emailAddress);
        u = userRepository.save(u);

        return u;
    }

    public UserEntity updateLogin(String uuid, String provider, Date loginDate, String loginIp)
        throws NoSuchUserException {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u == null) {
            throw new NoSuchUserException("no user for subject " + uuid);
        }

        u.setLoginProvider(provider);
        u.setLoginDate(loginDate);
        u.setLoginIp(loginIp);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity activateUser(String uuid) throws NoSuchUserException {
        return updateStatus(uuid, UserStatus.ACTIVE);
    }

    public UserEntity inactivateUser(String uuid) throws NoSuchUserException {
        return updateStatus(uuid, UserStatus.INACTIVE);
    }

    public UserEntity blockUser(String uuid) throws NoSuchUserException {
        return updateStatus(uuid, UserStatus.BLOCKED);
    }

    public UserEntity updateStatus(String uuid, UserStatus newStatus) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        // check if active, inactive users can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(u.getStatus());
        if (UserStatus.INACTIVE == curStatus && UserStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("user is inactive, activate first to update status");
        }

        u.setStatus(newStatus.getValue());
        u = userRepository.save(u);
        return u;
    }

    public UserEntity updateExpiration(String uuid, Date exp) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        u.setExpirationDate(exp);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity verifyEmail(String uuid, String emailAddress) throws NoSuchUserException {
        if (!StringUtils.hasText(emailAddress)) {
            throw new IllegalArgumentException("null or empty email");
        }

        UserEntity u = getUser(uuid);
        if (u.getEmailAddress() == null) {
            u.setEmailAddress(emailAddress);
        }

        if (!emailAddress.equals(u.getEmailAddress())) {
            throw new IllegalArgumentException("email address mismatch");
        }

        u.setEmailVerified(true);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity unverifyEmail(String uuid) throws NoSuchUserException {
        UserEntity u = getUser(uuid);
        u.setEmailVerified(false);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity updateTos(String uuid, Boolean tos) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        if (tos != null) {
            u.setTosAccepted(tos.booleanValue());
        } else {
            u.setTosAccepted(null);
        }

        u = userRepository.save(u);
        return u;
    }

    public void deleteUser(String uuid) {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            // remove entity
            userRepository.delete(u);
        }
    }

    private String generateUuid() {
        return UUID.randomUUID().toString();
    }
}
