package it.smartcommunitylab.aac.roles;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.roles.service.RealmRoleService;
import it.smartcommunitylab.aac.roles.service.SubjectRoleService;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.services.ServiceScope;

/*
 * Realm roles are bound to subjects and exists within a given realm
 * 
 * TODO realm permissions
 */

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class RealmRoleManager {
    private static final int DEFAULT_DURATION = 3650;

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

    @Autowired
    private ScopeRegistry scopeRegistry;

    /*
     * Realm roles
     */

    public Collection<RealmRole> getRealmRoles(String realm) {
        return rolesService.listRoles(realm);
    }

    public RealmRole getRealmRole(String realm, String roleId) throws NoSuchRoleException {
        return getRealmRole(realm, roleId, false);
    }

    public RealmRole getRealmRole(String realm, String roleId, boolean withMembers) throws NoSuchRoleException {
        RealmRole r = rolesService.getRole(roleId, withMembers);
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

        RealmRole res = rolesService.addRole(roleId, realm, role, name, description);
        roleId = res.getRoleId();

        // add permissions if defined
        if (r.getPermissions() != null) {
            try {
                Map<String, Boolean> scopesMap = r.getPermissions().stream()
                        .collect(Collectors.toMap(s -> s, s -> true));
                Collection<Approval> approvals = setRealmRoleApprovals(realm, roleId, scopesMap);
                Set<String> permissions = approvals.stream().map(a -> a.getScope()).collect(Collectors.toSet());
                res.setPermissions(permissions);
            } catch (NoSuchRoleException e) {
                // ignore
            }
        }

        // add subjects if defined
        if (r.getSubjects() != null) {
            Collection<String> subjects = roleService.setRoleSubjects(realm, role, r.getSubjects());
            res.setSubjects(new ArrayList<>(subjects));
        }

        return res;
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

        RealmRole res = rolesService.updateRole(roleId, realm, role, name, description);

        // add permissions if defined
        if (r.getPermissions() != null) {
            try {
                Map<String, Boolean> scopesMap = r.getPermissions().stream()
                        .collect(Collectors.toMap(s -> s, s -> true));
                Collection<Approval> approvals = setRealmRoleApprovals(realm, roleId, scopesMap);
                Set<String> permissions = approvals.stream().map(a -> a.getScope()).collect(Collectors.toSet());
                res.setPermissions(permissions);
            } catch (NoSuchRoleException e) {
                // ignore
            }
        }

        // update subjects if defined
        if (r.getSubjects() != null) {
            Collection<String> subjects = roleService.setRoleSubjects(realm, role, r.getSubjects());
            res.setSubjects(new ArrayList<>(subjects));
        }

        return res;
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

    public Approval addRealmRoleApproval(String realm, String roleId, String scope, boolean approved)
            throws NoSuchRoleException, NoSuchScopeException {
        RealmRole r = rolesService.fetchRole(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        // lookup scope in registry
        Scope s = scopeRegistry.getScope(scope);

        // TODO evaluate same realm check
        String resourceId = s.getResourceId();
        if (s instanceof ServiceScope) {
            resourceId = ((ServiceScope) s).getServiceId();
        }

        // add approval to store, will refresh if present
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, DEFAULT_DURATION);
        Date expiresAt = calendar.getTime();
        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        Approval approval = new Approval(resourceId, roleId, scope, expiresAt, approvalStatus);
        approvalStore.addApprovals(Collections.singleton(approval));

        return approvalStore.findApproval(resourceId, roleId, scope);
    }

    public Collection<Approval> addRealmRoleApprovals(String realm, String roleId, Map<String, Boolean> scopesMap)
            throws NoSuchRoleException {
        RealmRole r = rolesService.fetchRole(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        List<Approval> approvals = new ArrayList<>();
        // unpack map and process
        scopesMap.entrySet().forEach(e -> {
            String scope = e.getKey();
            boolean approved = e.getValue() != null ? e.getValue().booleanValue() : true;
            try {
                // lookup scope in registry
                Scope s = scopeRegistry.getScope(scope);

                // TODO evaluate same realm check
                String resourceId = s.getResourceId();
                if (s instanceof ServiceScope) {
                    resourceId = ((ServiceScope) s).getServiceId();
                }

                // add approval to store, will refresh if present
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, DEFAULT_DURATION);
                Date expiresAt = calendar.getTime();
                ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

                Approval approval = new Approval(resourceId, roleId, scope, expiresAt, approvalStatus);
                approvalStore.addApprovals(Collections.singleton(approval));
                approvals.add(approval);
            } catch (NoSuchScopeException ex) {
                // skip
            }

        });

        return approvals;
    }

    public Collection<Approval> setRealmRoleApprovals(String realm, String roleId, Map<String, Boolean> scopesMap)
            throws NoSuchRoleException {
        RealmRole r = rolesService.fetchRole(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        // unpack map and process
        scopesMap.entrySet().forEach(e -> {
            String scope = e.getKey();
            boolean approved = e.getValue() != null ? e.getValue().booleanValue() : true;
            try {
                // lookup scope in registry
                Scope s = scopeRegistry.getScope(scope);

                // TODO evaluate same realm check
                String resourceId = s.getResourceId();
                if (s instanceof ServiceScope) {
                    resourceId = ((ServiceScope) s).getServiceId();
                }

                // add approval to store, will refresh if present
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, DEFAULT_DURATION);
                Date expiresAt = calendar.getTime();
                ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

                Approval approval = new Approval(resourceId, roleId, scope, expiresAt, approvalStatus);
                approvalStore.addApprovals(Collections.singleton(approval));
            } catch (NoSuchScopeException ex) {
                // skip
            }

        });

        return approvalStore.findClientApprovals(roleId);
    }

    public void removeRealmRoleApproval(String realm, String roleId, String scope)
            throws NoSuchRoleException, NoSuchScopeException {
        RealmRole r = rolesService.fetchRole(roleId);
        if (r == null) {
            throw new NoSuchRoleException();
        }

        // lookup scope in registry
        Scope s = scopeRegistry.getScope(scope);
        String resourceId = s.getResourceId();

        Approval approval = approvalStore.findApproval(resourceId, roleId, scope);
        if (approval != null) {
            approvalStore.revokeApprovals(Collections.singleton(approval));
        }

    }

    /*
     * Role assignment
     */
    public Collection<String> getRoleSubjects(String realm, String role)
            throws NoSuchRealmException, NoSuchRoleException {
        return roleService.getRoleSubjects(realm, role);
    }

    public String addRoleSubject(String realm, String role, String subject)
            throws NoSuchRealmException, NoSuchRoleException, NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subject);

        // TODO evaluate checking if subject matches the realm
        return roleService.addRoleSubject(realm, role, s.getSubjectId());
    }

    public Collection<String> addRoleSubjects(String realm, String role, Collection<String> subjects)
            throws NoSuchRealmException, NoSuchRoleException, NoSuchSubjectException {

        // check if all subjects exists
        if (subjects.stream().anyMatch(s -> (subjectService.findSubject(s) == null))) {
            throw new NoSuchSubjectException();
        }

        // TODO evaluate checking if subjects match the realm
        subjects.stream()
                .map(s -> roleService.addRoleSubject(realm, role, s))
                .collect(Collectors.toList());

        return roleService.getRoleSubjects(realm, role);
    }

    public Collection<String> setRoleSubjects(String realm, String role, Collection<String> subjects)
            throws NoSuchRealmException, NoSuchRoleException, NoSuchSubjectException {

        // check if all subjects exists
        if (subjects.stream().anyMatch(s -> (subjectService.findSubject(s) == null))) {
            throw new NoSuchSubjectException();
        }

        // TODO evaluate checking if subjects match the realm
        return roleService.setRoleSubjects(realm, role, subjects);
    }

    public void removeRoleSubject(String realm, String role, String subject)
            throws NoSuchRealmException, NoSuchRoleException {
        roleService.removeRoleSubject(realm, role, subject);
    }

    public Collection<String> removeRoleSubjects(String realm, String role, Collection<String> subjects)
            throws NoSuchRealmException, NoSuchRoleException {
        subjects.stream()
                .forEach(s -> roleService.removeRoleSubject(realm, role, s));

        return roleService.getRoleSubjects(realm, role);
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

    public Collection<RealmRole> getSubjectRoles(String realm, String subjectId) throws NoSuchSubjectException {
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
