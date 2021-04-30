package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntityRepository;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntityRepository;

/*
 * Manage persistence for user entities and authorities (roles) 
 */
@Service
public class UserEntityService {

    private final UserEntityRepository userRepository;

    private final UserRoleEntityRepository userRoleRepository;

    public UserEntityService(UserEntityRepository userRepository, UserRoleEntityRepository userRoleRepository) {
        Assert.notNull(userRepository, "user repository is mandatory");
        Assert.notNull(userRoleRepository, "user roles repository is mandatory");
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public UserEntity createUser(String realm) {

        // generate random
        // TODO ensure unique on multi node deploy
        // (given that UUID is derived from timestamp we consider this safe enough)
        String uuid = UUID.randomUUID().toString();

        // we prepend a fixed prefix to enable discovery of entity type from uuid
        String id = UserEntity.ID_PREFIX + uuid;

        UserEntity u = new UserEntity(id, realm);

        return u;
    }

    public UserEntity addUser(String uuid, String realm, String username) throws AlreadyRegisteredException {
        if (!StringUtils.hasText(realm)) {
            throw new RegistrationException("realm is invalid");
        }

        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            throw new AlreadyRegisteredException("user already exists");
        }

        u = new UserEntity(uuid, realm);
        u.setUsername(username);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity addUser(String uuid, String realm, String username, List<String> roles)
            throws AlreadyRegisteredException {
        if (!StringUtils.hasText(realm)) {
            throw new RegistrationException("realm is invalid");
        }

        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            throw new AlreadyRegisteredException("user already exists");
        }

        u = new UserEntity(uuid, realm);
        u.setUsername(username);
        u = userRepository.save(u);
        for (String role : roles) {
            UserRoleEntity r = new UserRoleEntity(uuid);
            r.setRealm(SystemKeys.REALM_GLOBAL);
            r.setRole(role);
            userRoleRepository.save(r);
        }
        return u;
    }

    public UserEntity findUser(String uuid) {
        return userRepository.findByUuid(uuid);
    }

    public UserEntity getUser(String uuid) throws NoSuchUserException {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u == null) {
            throw new NoSuchUserException("no user for subject " + uuid);
        }

        return u;
    }

    public List<UserEntity> getUsers(String realm) {
        return userRepository.findByRealm(realm);
    }

	/**
	 * @param realm
	 * @return
	 */
	public Long countUsers(String realm) {
		return userRepository.countByRealm(realm);
	}
    public Page<UserEntity> searchUsers(String realm, String q, Pageable pageRequest) {
        Page<UserEntity> page = StringUtils.hasText(q) ? userRepository.findByRealm(realm.toLowerCase(), q, pageRequest)
                : userRepository.findByRealm(realm.toLowerCase(), pageRequest);
        return page;
    }

    public List<UserRoleEntity> getRoles(String uuid) throws NoSuchUserException {
        UserEntity u = getUser(uuid);
        return userRoleRepository.findBySubject(u.getUuid());
    }

    public List<UserRoleEntity> getRoles(String uuid, String realm) throws NoSuchUserException {
        UserEntity u = getUser(uuid);
        return userRoleRepository.findBySubjectAndRealm(u.getUuid(), realm);
    }

    public UserEntity updateUser(String uuid, String username) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        u.setUsername(username);
        u = userRepository.save(u);
        return u;

    }

    public List<UserRoleEntity> updateRoles(String uuid, String realm, Collection<String> roles)
            throws NoSuchUserException {

        UserEntity u = userRepository.findByUuid(uuid);
        if (u == null) {
            throw new NoSuchUserException("no user for subject " + uuid);
        }

        // fetch current roles
        List<UserRoleEntity> oldRoles = userRoleRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<UserRoleEntity> newRoles = roles.stream().map(r -> {
            UserRoleEntity re = new UserRoleEntity(uuid);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<UserRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r)).collect(Collectors.toSet());
        Set<UserRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());

        userRoleRepository.deleteAll(toDelete);
        userRoleRepository.saveAll(toAdd);

        return userRoleRepository.findBySubjectAndRealm(uuid, realm);

    }

    public List<UserRoleEntity> updateRoles(String uuid, Collection<Map.Entry<String, String>> roles)
            throws NoSuchUserException {

        UserEntity u = userRepository.findByUuid(uuid);
        if (u == null) {
            throw new NoSuchUserException("no user for subject " + uuid);
        }

        // fetch current roles
        List<UserRoleEntity> oldRoles = userRoleRepository.findBySubject(uuid);

        // unpack roles
        Set<UserRoleEntity> newRoles = roles.stream().map(e -> {
            UserRoleEntity re = new UserRoleEntity(uuid);
            re.setRealm(e.getKey());
            re.setRole(e.getValue());
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<UserRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r)).collect(Collectors.toSet());
        Set<UserRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());

        userRoleRepository.deleteAll(toDelete);
        userRoleRepository.saveAll(toAdd);

        return userRoleRepository.findBySubject(uuid);

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

    public UserEntity deleteUser(String uuid) {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            // also search roles
            List<UserRoleEntity> roles = userRoleRepository.findBySubject(uuid);
            userRoleRepository.deleteAll(roles);

            // remove entity
            userRepository.delete(u);
        }

        return u;
    }


}
