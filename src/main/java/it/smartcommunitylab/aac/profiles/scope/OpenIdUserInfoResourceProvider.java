package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.profiles.claims.OpenIdUserInfoClaimsExtractor;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;

public class OpenIdUserInfoResourceProvider extends AbstractInternalResourceProvider<OpenIdUserInfoResource> {

    protected OpenIdUserInfoResourceProvider(OpenIdUserInfoResource resource) {
        super(resource);
    }

    @Override
    protected ProfileScopeProvider buildScopeProvider(AbstractInternalApiScope scope) {
        return new ProfileScopeProvider(scope);
    }

    @Override
    protected OpenIdUserInfoClaimsExtractor buildClaimsExtractor(OpenIdUserInfoResource resource) {
        return new OpenIdUserInfoClaimsExtractor(resource);
    }

}