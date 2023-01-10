package it.smartcommunitylab.aac.profiles.scope;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public class ProfileApiResourceProvider extends AbstractResourceProvider<ProfileApiResource, AbstractInternalApiScope> {

    private Map<String, ApiScopeProvider<?>> providers;

    protected ProfileApiResourceProvider(String authority, String provider, String realm, ProfileApiResource resource) {
        super(authority, provider, realm, resource);

        // TODO handle dynamic definition of scopes based on attribute sets!!
        providers = Collections.emptyMap();

//        // build all providers eagerly, internal resources are static
//        providers = resource.getScopes().stream()
//                .map(s -> new ProfileScopeProvider(s))
//                .collect(Collectors.toMap(p -> p.getScope().getScope(), p -> p));

    }

    @Override
    public ApiScopeProvider<?> getScopeProvider(String scope) throws NoSuchScopeException {
        ApiScopeProvider<?> sp = providers.get(scope);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

}