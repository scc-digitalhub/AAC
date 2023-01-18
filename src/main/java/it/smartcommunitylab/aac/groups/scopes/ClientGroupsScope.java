package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ClientGroupsScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_CLIENT_GROUP;

    public ClientGroupsScope(String realm) {
        super(realm, GroupsResource.RESOURCE_ID, SCOPE);

        // require client
        this.subjectType = SystemKeys.RESOURCE_CLIENT;
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read client's groups";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Groups of the current client. Read access only.";
//    }

}
