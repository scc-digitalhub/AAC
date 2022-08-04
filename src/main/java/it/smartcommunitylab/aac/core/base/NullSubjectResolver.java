package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;

public class NullSubjectResolver<U extends UserAccount> implements SubjectResolver<U> {

    @Override
    public Subject resolveByAccountId(String accountId) {
        return null;
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        return null;
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        return null;
    }

    @Override
    public Subject resolveByUsername(String accountId) {
        return null;
    }

    @Override
    public Subject resolveByEmailAddress(String accountId) {
        return null;
    }

}
