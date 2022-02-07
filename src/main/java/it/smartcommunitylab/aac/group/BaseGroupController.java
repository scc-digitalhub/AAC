package it.smartcommunitylab.aac.group;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.model.Group;

/*
 * Base controller for realm groups
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseGroupController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected GroupManager groupManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Realm groups
     */

    @GetMapping("/groups/{realm}")
    public Collection<Group> getGroups(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list groups for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return groupManager.getGroups(realm);
    }

    @PostMapping("/groups/{realm}")
    public Group createGroup(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull Group reg)
            throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("add role to realm {}",
                StringUtils.trimAllWhitespace(realm));

        // unpack and build model
        String group = reg.getGroup();
        String parentGroup = reg.getParentGroup();
        String name = reg.getName();
        String description = reg.getDescription();

        Group g = new Group();
        g.setRealm(realm);
        g.setGroup(group);
        g.setParentGroup(parentGroup);
        g.setName(name);
        g.setDescription(description);

        if (logger.isTraceEnabled()) {
            logger.trace("group bean: " + StringUtils.trimAllWhitespace(g.toString()));
        }

        g = groupManager.addGroup(realm, g);
        return g;
    }

    @GetMapping("/groups/{realm}/{groupId}")
    public Group getGroup(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId)
            throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("get group {} for realm {}",
                StringUtils.trimAllWhitespace(groupId), StringUtils.trimAllWhitespace(realm));

        Group g = groupManager.getGroup(realm, groupId, true);
        return g;
    }

    @PutMapping("/groups/{realm}/{groupId}")
    public Group getGroup(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
            @RequestBody @Valid @NotNull Group reg)
            throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("update group {} for realm {}",
                StringUtils.trimAllWhitespace(groupId), StringUtils.trimAllWhitespace(realm));

        Group g = groupManager.getGroup(realm, groupId, false);

        // unpack and build model
        String group = reg.getGroup();
        String parentGroup = reg.getParentGroup();
        String name = reg.getName();
        String description = reg.getDescription();

        g.setParentGroup(parentGroup);
        g.setName(name);
        g.setDescription(description);

        if (logger.isTraceEnabled()) {
            logger.trace("group bean: " + StringUtils.trimAllWhitespace(g.toString()));
        }

        g = groupManager.updateGroup(realm, groupId, reg);

        // enable group rename if requested
        if (!g.getGroup().equals(group)) {
            logger.debug("rename group {} for realm {}",
                    StringUtils.trimAllWhitespace(groupId), StringUtils.trimAllWhitespace(realm));

            g = groupManager.renameGroup(realm, groupId, group);
        }

        return g;
    }

    @DeleteMapping("/groups/{realm}/{groupId}")
    public void removeGroup(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId)
            throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("delete group {} for realm {}",
                StringUtils.trimAllWhitespace(groupId), StringUtils.trimAllWhitespace(realm));

        groupManager.deleteGroup(realm, groupId);
    }
}
