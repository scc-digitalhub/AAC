package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SpaceRoles;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;

@Service
public class SpaceRoleManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private SpaceRoleService roleService;

    @Autowired
    private SubjectService subjectService;

    /*
     * Current user, from context
     */
    public Collection<SpaceRole> curUserRoles() {
        UserDetails userDetails = curUserDetails();
        String subjectId = userDetails.getSubjectId();
        return roleService.getRoles(subjectId);
    }

    /*
     * Users from db
     */
    public Collection<SpaceRole> getRoles(String subjectId) throws NoSuchUserException {
        return roleService.getRoles(subjectId);
    }

    /*
     * Subject
     */
    public Collection<SpaceRole> addRoles(String subjectId, Collection<SpaceRole> spaceRoles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);
        return roleService.addRoles(s.getSubjectId(), spaceRoles);
    }

    public void removeRoles(String subjectId, Collection<SpaceRole> spaceRoles) throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);
        roleService.removeRoles(s.getSubjectId(), spaceRoles);
    }

    public Collection<SpaceRole> addRoles(String subjectId, String context, String space, List<String> roles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);
        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
                .collect(Collectors.toList());

        return roleService.addRoles(subjectId, spaceRoles);

    }

    public void removeRoles(String subjectId, String context, String space, List<String> roles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);
        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
                .collect(Collectors.toList());

        roleService.removeRoles(s.getSubjectId(), spaceRoles);
    }

    /*
     * Context roles
     */
    public Page<SpaceRoles> getContextRoles(String context, String space, String q, Pageable pageRequest) {
        return roleService.getContextRoles(context, space, q, pageRequest);
    }

    public SpaceRoles saveContextRoles(String subject, String context, String space, List<String> roles) {
        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
                .collect(Collectors.toList());

        Collection<SpaceRole> oldRoles = roleService.getRoles(subject, context, space);
        roleService.removeRoles(subject, oldRoles);
        roleService.addRoles(subject, spaceRoles);

        SpaceRoles res = new SpaceRoles();
        res.setSubject(subject);
        res.setSpace(space);
        res.setContext(space);
        res.setRoles(roles);
        return res;
    }

    /*
     * Helpers
     */

    private UserDetails curUserDetails() {
        UserDetails details = authHelper.getUserDetails();
        if (details == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        return details;
    }
}
