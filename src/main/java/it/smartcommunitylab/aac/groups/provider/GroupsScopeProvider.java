package it.smartcommunitylab.aac.groups.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class GroupsScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public GroupsScopeProvider(AbstractInternalApiScope s) {
        super(SystemKeys.AUTHORITY_INTERNAL, s.getProvider(), s.getRealm(), s);

        // build approver based on type
        SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(s);
        sa.setSubjectType(s.getSubjectType());

        // set custom approver
        this.approver = sa;
    }

}