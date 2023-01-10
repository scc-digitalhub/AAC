package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class RolesScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public RolesScopeProvider(AbstractInternalApiScope s) {
        super(SystemKeys.AUTHORITY_INTERNAL, s.getProvider(), s.getRealm(), s);

        // build approver based on type
        SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(s);
        sa.setSubjectType(s.getSubjectType());

        setApprover(sa);
    }

}
