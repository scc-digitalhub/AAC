package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;

public class OpenIdResourceProvider extends AbstractInternalResourceProvider<OpenIdResource> {

    protected OpenIdResourceProvider(OpenIdResource resource) {
        super(resource);
    }

    @Override
    protected OpenIdScopeProvider buildScopeProvider(AbstractInternalApiScope scope) {
        return new OpenIdScopeProvider(scope);
    }

}