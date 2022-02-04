package it.smartcommunitylab.aac.core.service;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntityRepository;
import it.smartcommunitylab.aac.model.Subject;

/*
 * Manage persistence for user entities and authorities (roles) 
 */
@Service
@Transactional
public class UserEntityService {

    private final UserEntityRepository userRepository;

    // TODO move to userService when possible
    private final SubjectService subjectService;

    public UserEntityService(UserEntityRepository userRepository,
            SubjectService subjectService) {
        Assert.notNull(userRepository, "user repository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.userRepository = userRepository;
        this.subjectService = subjectService;
    }

    public UserEntity createUser(String realm) {
        String id = subjectService.generateUuid(SystemKeys.RESOURCE_USER);
        UserEntity u = new UserEntity(id, realm);

        return u;
    }

    public UserEntity addUser(
            String uuid, String realm,
            String username, String emailAddress) throws AlreadyRegisteredException {

        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            throw new AlreadyRegisteredException("user already exists");
        }

        // create subject
        Subject s = subjectService.addSubject(uuid, realm, SystemKeys.RESOURCE_USER, username);

        // create user
        u = new UserEntity(s.getSubjectId(), realm);
        u.setUsername(username);
        u.setEmailAddress(emailAddress);
        // ensure user is active
        u.setLocked(false);
        u.setBlocked(false);

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
    public List<UserEntity> listUsers() {
        return userRepository.findAll();
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
        Page<UserEntity> page = StringUtils.hasText(q) ? userRepository
                .findByRealmAndUsernameContainingIgnoreCaseOrRealmAndUuidContainingIgnoreCaseOrRealmAndEmailAddressContainingIgnoreCase(
                        realm, q,
                        realm, q,
                        realm, q,
                        pageRequest)
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

        // check if subject exists and update name
        Subject s = subjectService.findSubject(uuid);
        if (s == null) {
            s = subjectService.addSubject(uuid, u.getRealm(), SystemKeys.RESOURCE_USER, username);
        } else {
            try {
                s = subjectService.updateSubject(uuid, username);
            } catch (NoSuchSubjectException e) {
            }
        }

        return u;

    }

//    @Transactional(readOnly = true)
//    public List<UserRoleEntity> getRoles(String uuid) throws NoSuchUserException {
//        UserEntity u = getUser(uuid);
//        return userRoleRepository.findBySubject(u.getUuid());
//    }
//
//    @Transactional(readOnly = true)
//    public List<UserRoleEntity> getRoles(String uuid, String realm) throws NoSuchUserException {
//        UserEntity u = getUser(uuid);
//        return userRoleRepository.findBySubjectAndRealm(u.getUuid(), realm);
//    }
//

//
//    public List<UserRoleEntity> updateRoles(String uuid, String realm, Collection<String> roles)
//            throws NoSuchUserException {
//
//        UserEntity u = userRepository.findByUuid(uuid);
//        if (u == null) {
//            throw new NoSuchUserException("no user for subject " + uuid);
//        }
//
//        // fetch current roles
//        List<UserRoleEntity> oldRoles = userRoleRepository.findBySubjectAndRealm(uuid, realm);
//
//        // unpack roles
//        Set<UserRoleEntity> newRoles = roles.stream().map(r -> {
//            UserRoleEntity re = new UserRoleEntity(uuid);
//            re.setRealm(realm);
//            re.setRole(r);
//            return re;
//        }).collect(Collectors.toSet());
//
//        // update
//        Set<UserRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r)).collect(Collectors.toSet());
//        Set<UserRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());
//
//        userRoleRepository.deleteAll(toDelete);
//        userRoleRepository.saveAll(toAdd);
//
//        return userRoleRepository.findBySubjectAndRealm(uuid, realm);
//
//    }
//
//    public List<UserRoleEntity> updateRoles(String uuid, Collection<Map.Entry<String, String>> roles)
//            throws NoSuchUserException {
//
//        UserEntity u = userRepository.findByUuid(uuid);
//        if (u == null) {
//            throw new NoSuchUserException("no user for subject " + uuid);
//        }
//
//        // fetch current roles
//        List<UserRoleEntity> oldRoles = userRoleRepository.findBySubject(uuid);
//
//        // unpack roles
//        Set<UserRoleEntity> newRoles = roles.stream().map(e -> {
//            UserRoleEntity re = new UserRoleEntity(uuid);
//            re.setRealm(e.getKey());
//            re.setRole(e.getValue());
//            return re;
//        }).collect(Collectors.toSet());
//
//        // update
//        Set<UserRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r)).collect(Collectors.toSet());
//        Set<UserRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());
//
//        userRoleRepository.deleteAll(toDelete);
//        userRoleRepository.saveAll(toAdd);
//
//        return userRoleRepository.findBySubject(uuid);
//
//    }

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

    public UserEntity blockUser(String uuid) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        u.setBlocked(true);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity unblockUser(String uuid) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        u.setBlocked(false);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity lockUser(String uuid) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        u.setLocked(true);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity unlockUser(String uuid) throws NoSuchUserException {
        UserEntity u = getUser(uuid);

        u.setLocked(false);
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

    public UserEntity deleteUser(String uuid) {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {

            // remove entity
            userRepository.delete(u);

            // remove subject if exists
            subjectService.deleteSubject(uuid);

        }

        return u;
    }

}
