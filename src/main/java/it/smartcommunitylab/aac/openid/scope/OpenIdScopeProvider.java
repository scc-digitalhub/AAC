package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OpenIdScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public OpenIdScopeProvider(AbstractInternalApiScope s) {
        super(SystemKeys.AUTHORITY_OIDC, s.getProvider(), s.getRealm(), s);

        // build approver based on type
        SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(s);
        sa.setSubjectType(s.getSubjectType());

        setApprover(sa);
    }

}