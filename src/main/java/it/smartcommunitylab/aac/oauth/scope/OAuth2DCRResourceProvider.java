package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;

public class OAuth2DCRResourceProvider extends AbstractInternalResourceProvider<OAuth2DCRResource> {

    protected OAuth2DCRResourceProvider(OAuth2DCRResource resource) {
        super(resource);
    }

    @Override
    protected OAuth2DCRScopeProvider buildScopeProvider(AbstractInternalApiScope scope) {
        return new OAuth2DCRScopeProvider(scope);
    }

}