package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource.AbstractGroupsScope;
import it.smartcommunitylab.aac.model.SubjectType;

public class ClientGroupsScope extends AbstractGroupsScope {

    public static final String SCOPE = Config.SCOPE_CLIENT_GROUP;

    public ClientGroupsScope(String realm) {
        super(realm, SCOPE);

        // require client
        this.subjectType = SubjectType.CLIENT;
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
