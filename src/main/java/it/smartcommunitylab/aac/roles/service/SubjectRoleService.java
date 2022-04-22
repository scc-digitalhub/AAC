package it.smartcommunitylab.aac.roles.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.persistence.RealmRoleEntityRepository;
import it.smartcommunitylab.aac.roles.persistence.SubjectRoleEntity;
import it.smartcommunitylab.aac.roles.persistence.SubjectRoleEntityRepository;

@Service
@Transactional
public class SubjectRoleService {

    private final RealmRoleEntityRepository roleRepository;
    private final SubjectRoleEntityRepository rolesRepository;

    public SubjectRoleService(
            RealmRoleEntityRepository roleRepository,
            SubjectRoleEntityRepository rolesRepository) {
        Assert.notNull(roleRepository, "role repository is mandatory");
        Assert.notNull(rolesRepository, "roles repository is mandatory");

        this.roleRepository = roleRepository;
        this.rolesRepository = rolesRepository;
    }

    /*
     * Roles assignment per role
     */
    public String addRoleSubject(String realm, String role, String subjectId) {
        SubjectRoleEntity sr = rolesRepository.findByRealmAndRoleAndSubject(realm, role, subjectId);
        if (sr == null) {
            sr = new SubjectRoleEntity(subjectId);
            sr.setRealm(realm);
            sr.setRole(role);
            sr = rolesRepository.save(sr);
        }

        return sr.getSubject();
    }

    @Transactional(readOnly = true)
    public long countRoleSubjects(String realm, String role) {
        return rolesRepository.countByRealmAndRole(realm, role);
    }

    @Transactional(readOnly = true)
    public Collection<String> getRoleSubjects(String realm, String role) {
        return rolesRepository.findByRealmAndRole(realm, role).stream()
                .map(m -> m.getSubject())
                .collect(Collectors.toList());
    }

    public Collection<String> setRoleSubjects(String realm, String role, Collection<String> subjects) {
        // fetch current
        List<SubjectRoleEntity> oldSubjects = rolesRepository.findByRealmAndRole(realm, role);

        // unpack and merge
        Set<SubjectRoleEntity> newSubjects = subjects.stream().map(s -> {
            SubjectRoleEntity gm = new SubjectRoleEntity(s);
            gm.setRealm(realm);
            gm.setRole(role);
            return gm;
        }).collect(Collectors.toSet());

        Set<SubjectRoleEntity> toDelete = oldSubjects.stream().filter(sr -> !newSubjects.contains(sr))
                .collect(Collectors.toSet());
        Set<SubjectRoleEntity> toAdd = newSubjects.stream().filter(sr -> !oldSubjects.contains(sr))
                .collect(Collectors.toSet());

        // update
        rolesRepository.deleteAllInBatch(toDelete);
        rolesRepository.saveAll(toAdd);

        return getRoleSubjects(realm, role);
    }

    public void removeRoleSubject(String realm, String role, String subjectId) {
        SubjectRoleEntity sr = rolesRepository.findByRealmAndRoleAndSubject(realm, role, subjectId);
        if (sr != null) {
            rolesRepository.delete(sr);
        }
    }

    /*
     * Roles assignment per subject
     */
    @Transactional(readOnly = true)
    public Collection<RealmRole> getRoles(String subjectId) {
        return rolesRepository.findBySubject(subjectId).stream().map(r -> toRole(r)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Collection<RealmRole> getRoles(String subjectId, String realm) {
        return rolesRepository.findBySubjectAndRealm(subjectId, realm).stream().map(r -> toRole(r))
                .collect(Collectors.toList());
    }

    public Collection<RealmRole> addRoles(String subjectId, String realm, Collection<String> roles) {

        // fetch current roles
        List<SubjectRoleEntity> oldRoles = rolesRepository.findBySubjectAndRealm(subjectId, realm);

        // unpack roles
        Set<SubjectRoleEntity> newRoles = roles.stream().map(r -> {
            SubjectRoleEntity re = new SubjectRoleEntity(subjectId);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // add
        Set<SubjectRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());
        Collection<SubjectRoleEntity> result = rolesRepository.saveAll(toAdd);

        return result.stream().map(r -> toRole(r)).collect(Collectors.toList());
    }

    public Collection<RealmRole> setRoles(String subjectId, String realm, Collection<String> roles) {

        // fetch current roles
        List<SubjectRoleEntity> oldRoles = rolesRepository.findBySubjectAndRealm(subjectId, realm);

        // unpack roles
        Set<SubjectRoleEntity> newRoles = roles.stream().map(r -> {
            SubjectRoleEntity re = new SubjectRoleEntity(subjectId);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
                .collect(Collectors.toSet());
        Set<SubjectRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());

        rolesRepository.deleteAll(toDelete);
        rolesRepository.saveAll(toAdd);

        return getRoles(subjectId, realm);

    }

    public Collection<RealmRole> setRoles(String subjectId, Collection<Map.Entry<String, String>> roles) {

        // fetch current roles
        List<SubjectRoleEntity> oldRoles = rolesRepository.findBySubject(subjectId);

        // unpack roles
        Set<SubjectRoleEntity> newRoles = roles.stream().map(e -> {
            SubjectRoleEntity re = new SubjectRoleEntity(subjectId);
            re.setRealm(e.getKey());
            re.setRole(e.getValue());
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
                .collect(Collectors.toSet());
        Set<SubjectRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());

        rolesRepository.deleteAll(toDelete);
        rolesRepository.saveAll(toAdd);

        return getRoles(subjectId);

    }

    public void removeRoles(String subjectId, String realm, Collection<String> roles) {

        // fetch current roles
        List<SubjectRoleEntity> oldRoles = rolesRepository.findBySubjectAndRealm(subjectId, realm);

        // unpack roles
        Set<SubjectRoleEntity> newRoles = roles.stream().map(r -> {
            SubjectRoleEntity re = new SubjectRoleEntity(subjectId);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<SubjectRoleEntity> toDelete = oldRoles.stream().filter(r -> newRoles.contains(r))
                .collect(Collectors.toSet());

        rolesRepository.deleteAll(toDelete);

    }

    public void removeRoles(String realm, String role) {
        List<SubjectRoleEntity> roles = rolesRepository.findByRealmAndRole(realm, role);
        if (!roles.isEmpty()) {
            // remove
            rolesRepository.deleteAll(roles);
        }
    }

    public void deleteRoles(String subjectId) {
        List<SubjectRoleEntity> roles = rolesRepository.findBySubject(subjectId);
        if (!roles.isEmpty()) {
            // remove
            rolesRepository.deleteAll(roles);
        }
    }

    public void deleteRoles(String subjectId, String realm) {
        List<SubjectRoleEntity> roles = rolesRepository.findBySubjectAndRealm(subjectId, realm);
        if (!roles.isEmpty()) {
            // remove
            rolesRepository.deleteAll(roles);
        }
    }

    private RealmRole toRole(SubjectRoleEntity r) {
        RealmRole role = new RealmRole(r.getRealm(), r.getRole());
        // TODO evaluate loading role model to fill properties

        return role;
    }

}
