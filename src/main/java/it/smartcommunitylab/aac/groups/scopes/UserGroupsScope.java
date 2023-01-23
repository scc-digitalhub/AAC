package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource.AbstractGroupsScope;

public class UserGroupsScope extends AbstractGroupsScope {

    public static final String SCOPE = Config.SCOPE_USER_GROUP;

    public UserGroupsScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
    }
//
//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read user's groups";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Groups of the current platform user. Read access only.";
//    }

}
