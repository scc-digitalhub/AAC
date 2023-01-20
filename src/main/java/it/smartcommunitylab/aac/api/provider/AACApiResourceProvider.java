package it.smartcommunitylab.aac.api.provider;

import it.smartcommunitylab.aac.api.scopes.AACApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public class AACApiResourceProvider
        extends AbstractInternalResourceProvider<AACApiResource, AACApiResourceProviderConfig> {

    public AACApiResourceProvider(AACApiResourceProviderConfig config) {
        super(config.getAuthority(), config.getProvider(), config.getRealm(), config);
    }

    @Override
    protected ApiScopeProvider<AbstractInternalApiScope> buildScopeProvider(AbstractInternalApiScope scope) {
        return new AACApiScopeProvider(scope);
    }

}