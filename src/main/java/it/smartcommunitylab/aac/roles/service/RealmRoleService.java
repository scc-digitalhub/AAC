package it.smartcommunitylab.aac.roles.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.roles.persistence.RealmRoleEntity;
import it.smartcommunitylab.aac.roles.persistence.RealmRoleEntityRepository;
import it.smartcommunitylab.aac.roles.persistence.SubjectRoleEntity;
import it.smartcommunitylab.aac.roles.persistence.SubjectRoleEntityRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class RealmRoleService {

    private final RealmRoleEntityRepository roleRepository;
    private final SubjectRoleEntityRepository rolesRepository;
    private final SubjectService subjectService;
    private SearchableApprovalStore approvalStore;

    public RealmRoleService(
        RealmRoleEntityRepository roleRepository,
        SubjectRoleEntityRepository rolesRepository,
        SubjectService subjectService
    ) {
        Assert.notNull(roleRepository, "role repository is mandatory");
        Assert.notNull(rolesRepository, "roles repository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.roleRepository = roleRepository;
        this.rolesRepository = rolesRepository;
        this.subjectService = subjectService;
    }

    @Autowired
    public void setApprovalStore(SearchableApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    /*
     * Role model
     */

    public RealmRole addRole(String roleId, String realm, String role, String name, String description) {
        RealmRoleEntity r = roleRepository.findByRealmAndRole(realm, role);
        if (r != null) {
            throw new IllegalArgumentException("role already exists with the same id");
        }

        if (!StringUtils.hasText(roleId)) {
            roleId = subjectService.generateUuid(SystemKeys.RESOURCE_ROLE);
        }

        // create a subject, will throw error if exists
        subjectService.addSubject(roleId, realm, SystemKeys.RESOURCE_ROLE, role);

        // create role
        r = new RealmRoleEntity(roleId);
        r.setRealm(realm);
        r.setRole(role);

        r.setName(name);
        r.setDescription(description);

        r = roleRepository.save(r);

        return toRole(r);
    }

    @Transactional(readOnly = true)
    public RealmRole fetchRole(String roleId) {
        RealmRoleEntity r = roleRepository.findOne(roleId);
        if (r == null) {
            return null;
        }

        return toRole(r);
    }

    @Transactional(readOnly = true)
    public RealmRole getRole(String roleId) throws NoSuchRoleException {
        RealmRoleEntity r = roleRepository.findOne(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        return toRole(r);
    }

    @Transactional(readOnly = true)
    public RealmRole getRole(String roleId, boolean withMembers) throws NoSuchRoleException {
        RealmRoleEntity r = roleRepository.findOne(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        long size = rolesRepository.countByRealmAndRole(r.getRealm(), r.getRole());
        if (withMembers) {
            Collection<SubjectRoleEntity> subjects = rolesRepository.findByRealmAndRole(r.getRealm(), r.getRole());
            return toRole(r, size, subjects);
        }
        return toRole(r, size);
    }

    @Transactional(readOnly = true)
    public RealmRole findRole(String realm, String role) {
        RealmRoleEntity r = roleRepository.findByRealmAndRole(realm, role);
        if (r == null) {
            return null;
        }

        return toRole(r);
    }

    @Transactional(readOnly = true)
    public RealmRole getRole(String realm, String role) throws NoSuchRoleException {
        return getRole(realm, role, true);
    }

    @Transactional(readOnly = true)
    public RealmRole getRole(String realm, String role, boolean withSubjects) throws NoSuchRoleException {
        RealmRoleEntity r = roleRepository.findByRealmAndRole(realm, role);
        if (r == null) {
            throw new NoSuchRoleException();
        }
        RealmRole rr = toRole(r);

        long size = rolesRepository.countByRealmAndRole(r.getRealm(), r.getRole());
        rr.setSize(size);

        if (withSubjects) {
            Collection<SubjectRoleEntity> subjects = rolesRepository.findByRealmAndRole(realm, role);
            List<String> ss = subjects.stream().map(SubjectRoleEntity::getSubject).collect(Collectors.toList());
            rr.setSubjects(ss);
        }

        return rr;
    }

    @Transactional(readOnly = true)
    public Collection<RealmRole> listRoles(String realm) {
        return roleRepository.findByRealm(realm).stream().map(r -> toRole(r)).collect(Collectors.toList());
    }

    public RealmRole updateRole(String roleId, String realm, String role, String name, String description)
        throws NoSuchRoleException {
        RealmRoleEntity r = roleRepository.findOne(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        // update role
        // disable role update since we use it for mapping
        //        r.setRole(role);

        r.setName(name);
        r.setDescription(description);

        r = roleRepository.save(r);

        // check if subject exists and update name
        Subject s = subjectService.findSubject(roleId);
        if (s == null) {
            s = subjectService.addSubject(roleId, r.getRealm(), SystemKeys.RESOURCE_ROLE, name);
        } else {
            try {
                s = subjectService.updateSubject(roleId, name);
            } catch (NoSuchSubjectException e) {}
        }

        return toRole(r);
    }

    public void deleteRole(String roleId) {
        RealmRoleEntity r = roleRepository.findOne(roleId);
        if (r != null) {
            roleRepository.delete(r);

            // remove subject if exists
            subjectService.deleteSubject(roleId);
        }
    }

    //    public Collection<Approval> getApprovals(String roleId) throws NoSuchRoleException {
    //        RealmRoleEntity r = roleRepository.findOne(roleId);
    //        if (r == null) {
    //            throw new NoSuchRoleException();
    //        }
    //
    //        Collection<Approval> approvals = approvalStore.findClientApprovals(roleId);
    //        return approvals;
    //    }

    private RealmRole toRole(RealmRoleEntity r) {
        RealmRole role = new RealmRole(r.getRealm(), r.getRole());
        role.setRoleId(r.getId());
        String name = r.getName() != null ? r.getName() : r.getRole();
        role.setName(name);
        role.setDescription(r.getDescription());

        if (approvalStore != null) {
            Collection<Approval> approvals = approvalStore.findClientApprovals(r.getId());
            Set<String> permissions = approvals.stream().map(a -> a.getScope()).collect(Collectors.toSet());
            role.setPermissions(permissions);
        }
        return role;
    }

    private RealmRole toRole(RealmRoleEntity re, Long size) {
        RealmRole r = toRole(re);
        r.setSize(size);
        return r;
    }

    private RealmRole toRole(RealmRoleEntity re, long size, Collection<SubjectRoleEntity> subjects) {
        RealmRole r = toRole(re, size);
        List<String> sj = subjects.stream().map(SubjectRoleEntity::getSubject).collect(Collectors.toList());
        r.setSubjects(sj);
        return r;
    }
}
