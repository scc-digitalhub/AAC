package it.smartcommunitylab.aac.scope.provider;

import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;

public class SubjectTypeInternalScopeProvider<S extends AbstractInternalApiScope> extends AbstractScopeProvider<S> {

    public SubjectTypeInternalScopeProvider(S scope) {
        super(scope.getAuthority(), scope.getProvider(), scope.getRealm(), scope);

        // build approver based on type
        SubjectTypeScopeApprover<S> sa = new SubjectTypeScopeApprover<>(scope);
        sa.setSubjectType(scope.getSubjectType());

        // set custom approver
        this.approver = sa;
    }

}
