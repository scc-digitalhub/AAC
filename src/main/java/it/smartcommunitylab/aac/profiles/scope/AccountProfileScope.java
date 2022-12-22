package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AccountProfileScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_ACCOUNT_PROFILE;

    public AccountProfileScope(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, ProfileApiResource.RESOURCE_ID, SCOPE);

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's account profile";
    }

    @Override
    public String getDescription() {
        return "Account profile of the current platform user. Read access only.";
    }

}
