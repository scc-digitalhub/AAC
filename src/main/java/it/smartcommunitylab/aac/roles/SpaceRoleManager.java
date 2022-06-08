package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.SpaceRoles;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;

/*
 * Space roles are bound to subjects and exists under a context+space hierarchy
 */

//TODO add permission checks
@Service
public class SpaceRoleManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private SpaceRoleService roleService;

    @Autowired
    private SubjectService subjectService;

    public Collection<SpaceRole> curRoles() {
        Authentication auth = authHelper.getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("invalid or missing authentication");
        }

        String subjectId = auth.getName();
        return roleService.getRoles(subjectId);
    }

    public Collection<SpaceRole> curContexts() {
        Authentication auth = authHelper.getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("invalid or missing authentication");
        }

        String subjectId = auth.getName();
        return roleService.getRoles(subjectId).stream().filter(r -> Config.R_PROVIDER.equals(r.getRole()))
                .collect(Collectors.toList());

    }

    public Collection<SpaceRole> getRoles(String subjectId) throws NoSuchSubjectException {
        return roleService.getRoles(subjectId);
    }

    public Collection<SpaceRole> addRoles(String subjectId, Collection<SpaceRole> spaceRoles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // get my roles
        Collection<SpaceRole> myRoles = curRoles();

        // filter request based on permissions
        Collection<SpaceRole> values = spaceRoles.stream().filter(r -> isOwner(myRoles, r.getContext(), r.getSpace()))
                .collect(Collectors.toList());

        // add only to owned context/spaces
        return roleService.addRoles(s.getSubjectId(), values);
    }

//    public Collection<SpaceRole> addRoles(String subjectId, String context, String space, List<String> roles)
//            throws NoSuchSubjectException {
//        // check if subject exists
//        Subject s = subjectService.getSubject(subjectId);
//        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
//                .collect(Collectors.toList());
//
//        return roleService.addRoles(s.getSubjectId(), spaceRoles);
//
//    }

//    public Collection<SpaceRole> setRoles(String subjectId, String context, String space, List<String> roles)
//            throws NoSuchSubjectException {
//        // check if subject exists
//        Subject s = subjectService.getSubject(subjectId);
//        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
//                .collect(Collectors.toList());
//
//        return roleService.setRoles(s.getSubjectId(), spaceRoles);
//    }

    public Collection<SpaceRole> setRoles(String subjectId, Collection<SpaceRole> spaceRoles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // get my roles
        Collection<SpaceRole> myRoles = curRoles();

        // get all subject roles, we'll cleanup those not in list
        // also filter curRoles based on permission
        Collection<SpaceRole> curRoles = roleService.getRoles(subjectId).stream()
                .filter(r -> isOwner(myRoles, r.getContext(), r.getSpace()))
                .collect(Collectors.toList());

        // filter request based on permissions
        Collection<SpaceRole> values = spaceRoles.stream().filter(r -> isOwner(myRoles, r.getContext(), r.getSpace()))
                .collect(Collectors.toList());

        // any cur roles not set will be removed
        Collection<SpaceRole> toRemove = curRoles.stream().filter(r -> !values.contains(r))
                .collect(Collectors.toList());
        // new will be added
        Collection<SpaceRole> toAdd = values.stream().filter(r -> !curRoles.contains(r))
                .collect(Collectors.toList());

        roleService.removeRoles(subjectId, toRemove);
        roleService.addRoles(subjectId, toAdd);

        return roleService.getRoles(subjectId);
    }

    public Collection<SpaceRole> setRoles(String subjectId, String context, String space,
            Collection<SpaceRole> spaceRoles)
            throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // get my roles
        Collection<SpaceRole> myRoles = curRoles();

        // check if owner
        if (!isOwner(myRoles, context, space)) {
            throw new IllegalArgumentException("subject is not owner of the selected space");
        }

        return roleService.setRoles(s.getSubjectId(), context, space, spaceRoles);
    }

    public void removeRoles(String subjectId, Collection<SpaceRole> spaceRoles) throws NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subjectId);

        // get my roles
        Collection<SpaceRole> myRoles = curRoles();

        // filter request based on permissions
        Collection<SpaceRole> values = spaceRoles.stream().filter(r -> isOwner(myRoles, r.getContext(), r.getSpace()))
                .collect(Collectors.toList());

        // remove only from owned context/spaces
        roleService.removeRoles(s.getSubjectId(), values);
    }

//    public void removeRoles(String subjectId, String context, String space, List<String> roles)
//            throws NoSuchSubjectException {
//        // check if subject exists
//        Subject s = subjectService.getSubject(subjectId);
//        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
//                .collect(Collectors.toList());
//
//        roleService.removeRoles(s.getSubjectId(), spaceRoles);
//    }

    public Page<SpaceRoles> searchRoles(String context, String space, String q, Pageable pageRequest) {
        return roleService.searchRoles(context, space, q, pageRequest);
    }

//    public SpaceRoles setRoles(String subject, String context, String space, List<String> roles) {
//        Collection<SpaceRole> spaceRoles = roles.stream().map(r -> new SpaceRole(context, space, r))
//                .collect(Collectors.toList());
//
//        Collection<SpaceRole> oldRoles = roleService.getRoles(subject, context, space);
//        roleService.removeRoles(subject, oldRoles);
//        roleService.addRoles(subject, spaceRoles);
//
//        SpaceRoles res = new SpaceRoles();
//        res.setSubject(subject);
//        res.setSpace(space);
//        res.setContext(space);
//        res.setRoles(roles);
//        return res;
//    }

    /*
     * Helpers
     */

    private boolean isOwner(Collection<SpaceRole> myRoles, String context, String space) {
        // current subject should be owner of the space or of the DIRECT parent
        // TODO evaluate whole chain
        SpaceRole spaceOwner = new SpaceRole(context, space, Config.R_PROVIDER);
        SpaceRole parentOwner = context != null ? SpaceRole.ownerOf(context) : null;
        return myRoles.stream().anyMatch(r -> r.equals(spaceOwner) || r.equals(parentOwner));
    }

}
