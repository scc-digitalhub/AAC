package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AbstractProfileScope extends AbstractInternalApiScope {

    public AbstractProfileScope(String realm, String scope) {
        super(ProfileResource.AUTHORITY, realm, ProfileResource.RESOURCE_ID, scope);
    }

}
