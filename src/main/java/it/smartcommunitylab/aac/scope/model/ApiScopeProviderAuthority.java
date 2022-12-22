package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;

public interface ApiScopeProviderAuthority<S extends ApiScopeProvider<R>, R extends ApiScope>
        extends ProviderAuthority<S, R> {

}
