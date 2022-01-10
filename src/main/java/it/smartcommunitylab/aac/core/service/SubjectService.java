package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.SubjectAuthorityEntity;
import it.smartcommunitylab.aac.core.persistence.SubjectAuthorityEntityRepository;
import it.smartcommunitylab.aac.core.persistence.SubjectEntity;
import it.smartcommunitylab.aac.core.persistence.SubjectEntityRepository;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.group.persistence.GroupEntity;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.roles.persistence.RealmRoleEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceEntity;

@Service
@Transactional
public class SubjectService {

    private final SubjectEntityRepository subjectRepository;

    private final SubjectAuthorityEntityRepository authorityRepository;

    // TODO add dynamic subject type registration via beans?
    // private Map<String, SubjectType> types;

    // TODO add uuid generator as component
//    private UUIDGenerator uuidGenerator;

    public SubjectService(SubjectEntityRepository subjectRepository,
            SubjectAuthorityEntityRepository authorityRepository) {
        Assert.notNull(subjectRepository, "subject repository is mandatory");
        Assert.notNull(authorityRepository, "authorities repository is mandatory");

        this.subjectRepository = subjectRepository;
        this.authorityRepository = authorityRepository;

    }

    public String generateUuid(String type) {
        // generate random
        // TODO ensure unique on multi node deploy: replace with idGenerator
        // (given that UUID is derived from timestamp we consider this safe enough)
        String uuid = UUID.randomUUID().toString();

        String prefix = "";

        // TODO replace resource type inference with dynamic type registration at boot
        if (SystemKeys.RESOURCE_CLIENT.equals(type)) {
            prefix = ClientEntity.ID_PREFIX;
        } else if (SystemKeys.RESOURCE_USER.equals(type)) {
            prefix = UserEntity.ID_PREFIX;
        } else if (SystemKeys.RESOURCE_ROLE.equals(type)) {
            prefix = RealmRoleEntity.ID_PREFIX;
        } else if (SystemKeys.RESOURCE_ROLE.equals(type)) {
            prefix = RealmRoleEntity.ID_PREFIX;
        } else if (SystemKeys.RESOURCE_SERVICE.equals(type)) {
            prefix = ServiceEntity.ID_PREFIX;
        } else if (SystemKeys.RESOURCE_GROUP.equals(type)) {
            prefix = GroupEntity.ID_PREFIX;
        }

        // we prepend a fixed prefix to enable discovery of entity type from uuid
        // TODO remove, we need to avoid disclose of info
        // TODO remove prefix, we need a robust collision avoidance directly on uuid
        // UUID should leverage resource type as namespace (eg v5)
        String id = prefix + uuid;

        return id;
    }

    public Subject createSubject(String id, String type) {
        if (!StringUtils.hasText(id)) {
            id = generateUuid(type);
        }

        // create a subject
        SubjectEntity s = new SubjectEntity(id);
        s.setType(type);

        return toSubject(s);
    }

    public Subject addSubject(String id, String realm, String type, String name) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("subject id can not be null or empty");
        }

        if (id.length() < 8 || !Pattern.matches(SystemKeys.SLUG_PATTERN, id)) {
            throw new IllegalArgumentException("invalid id");
        }

        // create a subject, will throw error if exists
        SubjectEntity s = new SubjectEntity(id);
        s.setRealm(realm);
        s.setType(type);
        s.setName(name);
        s = subjectRepository.save(s);

        return toSubject(s);
    }

    @Transactional(readOnly = true)
    public Subject getSubject(String id) throws NoSuchSubjectException {
        Subject s = findSubject(id);
        if (s == null) {
            throw new NoSuchSubjectException();
        }

        return s;
    }

    @Transactional(readOnly = true)
    public Subject findSubject(String id) {
        SubjectEntity s = subjectRepository.findBySubjectId(id);
        if (s == null) {
            return null;
        }

        return toSubject(s);
    }

    public Subject updateSubject(String id, String name) throws NoSuchSubjectException {
        SubjectEntity s = subjectRepository.findBySubjectId(id);
        if (s == null) {
            throw new NoSuchSubjectException();
        }

        s.setName(name);
        s = subjectRepository.save(s);

        return toSubject(s);
    }

    public void deleteSubject(String id) {
        // delete authorities
        deleteAuthorities(id);

        SubjectEntity s = subjectRepository.findBySubjectId(id);
        if (s != null) {
            subjectRepository.delete(s);
        }
    }

    @Transactional(readOnly = true)
    public List<Subject> listSubjects(String realm) {
        return subjectRepository.findByRealm(realm).stream().map(s -> toSubject(s)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Subject> listSubjectsByAuthorities(String realm) {
        Set<String> ids = authorityRepository.findByRealm(realm).stream().map(a -> a.getSubject())
                .collect(Collectors.toSet());
        List<SubjectEntity> subjects = ids.stream().map(id -> subjectRepository.findBySubjectId(id))
                .filter(s -> s != null).collect(Collectors.toList());
        return subjects.stream().map(s -> toSubject(s)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Subject> listSubjectsByAuthorities(String realm, String role) {
        Set<String> ids = authorityRepository.findByRealmAndRole(realm, role).stream().map(a -> a.getSubject())
                .collect(Collectors.toSet());
        List<SubjectEntity> subjects = ids.stream().map(id -> subjectRepository.findBySubjectId(id))
                .filter(s -> s != null).collect(Collectors.toList());
        return subjects.stream().map(s -> toSubject(s)).collect(Collectors.toList());
    }

//    @Transactional(readOnly = true)
//    public Subject getSubjectByClientId(String clientId) throws NoSuchSubjectException {
//        SubjectEntity s = subjectRepository.findByClientId(clientId);
//        if (s == null) {
//            return null;
//        }
//
//        return new Subject(s.getSubjectId(), s.getRealm(), s.getName(), s.getType());
//    }
//
//    @Transactional(readOnly = true)
//    public Subject getSubjectBySubjectId(String userId) throws NoSuchSubjectException {
//        SubjectEntity s = subjectRepository.findBySubjectId(userId);
//        if (s == null) {
//            return null;
//        }
//
//        return new Subject(s.getSubjectId(), s.getRealm(), s.getName(), s.getType());
//    }

    @Transactional(readOnly = true)
    public List<GrantedAuthority> getAuthorities(String subjectId) {
        return authorityRepository.findBySubject(subjectId).stream().map(a -> toAuthority(a))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GrantedAuthority> getAuthorities(String subjectId, String realm) {
        return authorityRepository.findBySubjectAndRealm(subjectId, realm).stream().map(a -> toAuthority(a))
                .collect(Collectors.toList());
    }

    public List<GrantedAuthority> addAuthorities(String uuid, String realm, Collection<String> roles)
            throws NoSuchSubjectException {

        SubjectEntity s = subjectRepository.findBySubjectId(uuid);

        if (s == null) {
            throw new NoSuchSubjectException("no user for subject " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles.stream().map(r -> {
            SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r))
                .collect(Collectors.toSet());

        return authorityRepository.saveAll(toAdd).stream().map(a -> toAuthority(a))
                .collect(Collectors.toList());
    }

    public List<GrantedAuthority> addAuthorities(String uuid, Collection<Map.Entry<String, String>> roles)
            throws NoSuchSubjectException {

        SubjectEntity s = subjectRepository.findBySubjectId(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("no user for subject " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubject(uuid);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles.stream().map(e -> {
            SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
            re.setRealm(e.getKey());
            re.setRole(e.getValue());
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r))
                .collect(Collectors.toSet());

        return authorityRepository.saveAll(toAdd).stream().map(a -> toAuthority(a))
                .collect(Collectors.toList());
    }

    public void removeAuthorities(String uuid, String realm, Collection<String> roles)
            throws NoSuchSubjectException {

        SubjectEntity s = subjectRepository.findBySubjectId(uuid);

        if (s == null) {
            throw new NoSuchSubjectException("no user for subject " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles.stream().map(r -> {
            SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles.stream().filter(r -> newRoles.contains(r))
                .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
    }

    public void removeAuthorities(String uuid, Collection<Map.Entry<String, String>> roles)
            throws NoSuchSubjectException {

        SubjectEntity s = subjectRepository.findBySubjectId(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("no user for subject " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubject(uuid);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles.stream().map(e -> {
            SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
            re.setRealm(e.getKey());
            re.setRole(e.getValue());
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles.stream().filter(r -> newRoles.contains(r))
                .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
    }

    public List<GrantedAuthority> updateAuthorities(String uuid, String realm, Collection<String> roles)
            throws NoSuchSubjectException {

        SubjectEntity s = subjectRepository.findBySubjectId(uuid);

        if (s == null) {
            throw new NoSuchSubjectException("no user for subject " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles.stream().map(r -> {
            SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
                .collect(Collectors.toSet());
        Set<SubjectAuthorityEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r))
                .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
        authorityRepository.saveAll(toAdd);

        return authorityRepository.findBySubjectAndRealm(uuid, realm).stream().map(a -> toAuthority(a))
                .collect(Collectors.toList());

    }

    public List<GrantedAuthority> updateAuthorities(String uuid, Collection<Map.Entry<String, String>> roles)
            throws NoSuchSubjectException {

        SubjectEntity s = subjectRepository.findBySubjectId(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("no user for subject " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubject(uuid);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles.stream().map(e -> {
            SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
            re.setRealm(e.getKey());
            re.setRole(e.getValue());
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
                .collect(Collectors.toSet());
        Set<SubjectAuthorityEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r))
                .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
        authorityRepository.saveAll(toAdd);

        return authorityRepository.findBySubject(uuid).stream().map(a -> toAuthority(a)).collect(Collectors.toList());

    }

    public void deleteAuthorities(String subjectId) {
        List<SubjectAuthorityEntity> roles = authorityRepository.findBySubject(subjectId);
        if (!roles.isEmpty()) {
            // remove
            authorityRepository.deleteAll(roles);
        }
    }

    public void deleteAuthorities(String subjectId, String realm) {
        List<SubjectAuthorityEntity> roles = authorityRepository.findBySubjectAndRealm(subjectId, realm);
        if (!roles.isEmpty()) {
            // remove
            authorityRepository.deleteAll(roles);
        }
    }

    private Subject toSubject(SubjectEntity s) {
        return new Subject(s.getSubjectId(), s.getRealm(), s.getName(), s.getType());
    }

    private GrantedAuthority toAuthority(SubjectAuthorityEntity authority) {
        if (StringUtils.hasText(authority.getRealm())) {
            return new RealmGrantedAuthority(authority.getRealm(), authority.getRole());
        } else {
            return new SimpleGrantedAuthority(authority.getRole());
        }
    }
}
