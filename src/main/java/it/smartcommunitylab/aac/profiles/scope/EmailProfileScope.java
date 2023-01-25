package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.SubjectType;

public class EmailProfileScope extends AbstractProfileScope {

    public static final String SCOPE = Config.SCOPE_EMAIL_PROFILE;

    public EmailProfileScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SubjectType.USER;
    }

}
