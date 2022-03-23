package it.smartcommunitylab.aac.groups;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.groups.scopes.ClientGroupsScope;
import it.smartcommunitylab.aac.groups.scopes.UserGroupsScope;
import it.smartcommunitylab.aac.groups.service.GroupService;
import it.smartcommunitylab.aac.model.Group;

@RestController
@Tag(name = "AAC Groups")
public class GroupController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GroupService groupService;

//    @Operation(summary = "Get groups of the current user")
//    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + UserGroupsScope.SCOPE + "')")
//    @RequestMapping(method = RequestMethod.GET, value = "/usergroups/me")
//    public Collection<Group> getUserGroups(BearerTokenAuthentication auth)
//            throws InvalidDefinitionException, NoSuchSubjectException {
//        if (auth == null) {
//            logger.error("invalid authentication");
//            throw new IllegalArgumentException("invalid authentication");
//        }
//
//        String subject = (String) auth.getTokenAttributes().get("sub");
//
//        if (!StringUtils.hasText(subject)) {
//            logger.error("invalid authentication");
//            throw new IllegalArgumentException("invalid authentication");
//        }
//
//        // return all the user roles
//        return groupService.getSubjectGroups(subject);
//    }
//
//    @Operation(summary = "Get groups of the current client")
//    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + ClientGroupsScope.SCOPE + "')")
//    @RequestMapping(method = RequestMethod.GET, value = "/clientgroups/me")
//    public Collection<Group> getClientGroups(BearerTokenAuthentication auth)
//            throws InvalidDefinitionException, NoSuchSubjectException {
//        if (auth == null) {
//            logger.error("invalid authentication");
//            throw new IllegalArgumentException("invalid authentication");
//        }
//
//        String subject = (String) auth.getTokenAttributes().get("sub");
//
//        if (!StringUtils.hasText(subject)) {
//            logger.error("invalid authentication");
//            throw new IllegalArgumentException("invalid authentication");
//        }
//
//        // return all the client roles
//        return groupService.getSubjectGroups(subject);
//    }

    @Operation(summary = "Get groups for the current subject")
    @PreAuthorize("(hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + UserGroupsScope.SCOPE
            + "')) or (hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + ClientGroupsScope.SCOPE
            + "'))")
    @RequestMapping(method = RequestMethod.GET, value = "/groups/me")
    public Collection<Group> getSubjectGroups(BearerTokenAuthentication auth)
            throws InvalidDefinitionException, NoSuchSubjectException {
        if (auth == null) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        String subject = (String) auth.getTokenAttributes().get("sub");

        if (!StringUtils.hasText(subject)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        // return all the subject roles
        return groupService.getSubjectGroups(subject);
    }

}
