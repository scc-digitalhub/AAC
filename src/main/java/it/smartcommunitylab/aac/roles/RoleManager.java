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

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SpaceRoles;

@Service
public class RoleManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserEntityService userService;

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
    public Collection<SpaceRole> getUserRoles(String subjectId) throws NoSuchUserException {
        UserEntity user = userService.getUser(subjectId);
        return roleService.getRoles(subjectId);
    }

    public Collection<SpaceRole> getUserRoles(String realm, String subjectId) throws NoSuchUserException {
        // we don't filter space roles per realm, so read all
        return getUserRoles(subjectId);
    }

    /*
     * Manage
     */
    public Collection<SpaceRole> addRoles(String subjectId, Collection<SpaceRole> spaceRoles) {
        return roleService.addRoles(subjectId, spaceRoles);
    }

    public void removeRoles(String subject, Collection<SpaceRole> spaceRoles) {
        roleService.removeRoles(subject, spaceRoles);
    }

    public Collection<SpaceRole> addRoles(String subjectId, String context, String space, List<String> roles) {
        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
                .collect(Collectors.toList());

        return roleService.addRoles(subjectId, spaceRoles);

    }

    public void removeRoles(String subject, String context, String space, List<String> roles) {
        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
                .collect(Collectors.toList());

        roleService.removeRoles(subject, spaceRoles);
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

        Set<SpaceRole> oldRoles = roleService.getRoles(subject, context, space);
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
