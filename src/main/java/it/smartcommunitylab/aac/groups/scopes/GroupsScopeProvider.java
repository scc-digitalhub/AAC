package it.smartcommunitylab.aac.groups.scopes;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractApiScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class GroupsScopeProvider extends AbstractApiScopeProvider<AbstractInternalApiScope> {

    public GroupsScopeProvider(GroupsResource resource) {
        super(SystemKeys.AUTHORITY_INTERNAL, resource.getProvider(), resource.getScopes());
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