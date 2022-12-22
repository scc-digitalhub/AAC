package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;

public interface ApiResourceProviderAuthority<S extends ApiResourceProvider<R>, R extends ApiResource>
        extends ProviderAuthority<S, R> {

}
