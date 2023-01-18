package it.smartcommunitylab.aac.groups.provider;

import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;

public class GroupsResourceProvider extends AbstractInternalResourceProvider<GroupsResource> {

    protected GroupsResourceProvider(GroupsResource resource) {
        super(resource);
    }

    @Override
    protected GroupsScopeProvider buildScopeProvider(AbstractInternalApiScope scope) {
        return new GroupsScopeProvider(scope);
    }

    @Override
    protected GroupsClaimsExtractor buildClaimsExtractor(GroupsResource resource) {
        return new GroupsClaimsExtractor(resource);
    }

}