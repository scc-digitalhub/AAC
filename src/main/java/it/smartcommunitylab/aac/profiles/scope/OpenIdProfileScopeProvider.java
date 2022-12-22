package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractApiScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

/*
 * A simple scope provider which return openid profile scopes
 */

public class OpenIdProfileScopeProvider extends AbstractApiScopeProvider<AbstractInternalApiScope> {

    public OpenIdProfileScopeProvider(OpenIdUserInfoResource resource) {
        super(SystemKeys.AUTHORITY_OIDC, resource.getProvider(), resource.getScopes());
        Assert.notNull(resource, "resource can not be null");
    }

    @Override
    protected SubjectTypeScopeApprover<AbstractInternalApiScope> buildScopeApprover(AbstractInternalApiScope s) {

        // build approver based on type
        SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(s);
        sa.setSubjectType(s.getSubjectType());

        return sa;
    }

}
