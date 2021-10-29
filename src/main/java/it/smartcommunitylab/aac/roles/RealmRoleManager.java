package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.roles.persistence.RealmRoleEntity;
import it.smartcommunitylab.aac.roles.service.RealmRoleService;
import it.smartcommunitylab.aac.roles.service.SubjectRoleService;

/*
 * Realm roles are bound to subjects and exists within a given realm
 * 
 * TODO realm permissions
 */

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class RealmRoleManager {
    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private RealmRoleService rolesService;

    @Autowired
    private SubjectRoleService roleService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SearchableApprovalStore approvalStore;

    /*
     * Realm roles
     */

    public Collection<RealmRole> getRealmRoles(String realm) {
        return rolesService.listRoles(realm);
    }

    public RealmRole getRealmRole(String realm, String roleId) throws NoSuchRoleException {
        RealmRole r = rolesService.getRole(roleId);
        if (!realm.equals(r.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        return r;
    }

    public RealmRole addRealmRole(String realm, RealmRole r) {

        String role = r.getRole();
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("role can not be null or empty");
        }

        role = Jsoup.clean(role, Safelist.none());

        String roleId = r.getRoleId();
        String name = r.getName();
        String description = r.getDescription();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        return rolesService.addRole(roleId, realm, role, name, description);

    }

    public RealmRole updateRealmRole(String realm, String roleId, RealmRole r) throws NoSuchRoleException {

        RealmRole rl = rolesService.getRole(roleId);
        if (!realm.equals(rl.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        String name = r.getName();
        String description = r.getDescription();
        String role = r.getRole();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        return rolesService.updateRole(roleId, realm, role, name, description);

    }

    public void deleteRealmRole(String realm, String roleId) {
        RealmRole r = rolesService.fetchRole(roleId);
        if (r != null) {
            if (!realm.equals(r.getRealm())) {
                throw new IllegalArgumentException("realm mismatch");
            }

            String role = r.getRole();

            // remove role model
            rolesService.deleteRole(r.getRoleId());

            // remove all assignments
            roleService.removeRoles(realm, role);
        }

    }

    public Collection<Approval> getRealmRoleApprovals(String realm, String roleId) throws NoSuchRoleException {
        RealmRole r = rolesService.fetchRole(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        Collection<Approval> approvals = approvalStore.findClientApprovals(roleId);
        return approvals;
    }

    /*
     * Subject roles
     */

    public Collection<RealmRole> curSubjectRoles(String realm) {
        Authentication auth = authHelper.getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("invalid or missing authentication");
        }

        String subjectId = auth.getName();
        return roleService.getRoles(subjectId, realm);
    }

    public Collection<RealmRole> getSubjectRoles(String realm, String subjectId) throws NoSuchUserException {
        return roleService.getRoles(subjectId, realm);
    }

    public Collection<RealmRole> addSubjectRoles(String realm, String subjectId, Collection<RealmRole> roles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // unpack
        Collection<String> toAdd = roles.stream()
                .filter(r -> realm.equals(r.getRealm()) || r.getRealm() == null)
                .filter(r -> rolesService.findRole(realm, r.getRole()) != null)
                .map(r -> r.getRole()).collect(Collectors.toList());

        roleService.addRoles(s.getSubjectId(), realm, toAdd);

        return roleService.getRoles(subjectId, realm);
    }

    public Collection<RealmRole> removeSubjectRoles(String realm, String subjectId, Collection<RealmRole> roles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // unpack
        Collection<String> toRemove = roles.stream()
                .filter(r -> realm.equals(r.getRealm()) || r.getRealm() == null)
                .map(r -> r.getRole()).collect(Collectors.toList());

        roleService.removeRoles(s.getSubjectId(), realm, toRemove);

        return roleService.getRoles(subjectId, realm);
    }

    public Collection<RealmRole> setSubjectRoles(String realm, String subjectId, Collection<RealmRole> roles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // unpack
        Collection<String> toSet = roles.stream()
                .filter(r -> realm.equals(r.getRealm()) || r.getRealm() == null)
                .filter(r -> rolesService.findRole(realm, r.getRole()) != null)
                .map(r -> r.getRole()).collect(Collectors.toList());

        roleService.removeRoles(s.getSubjectId(), realm, toSet);

        return roleService.setRoles(subjectId, realm, toSet);
    }
}
