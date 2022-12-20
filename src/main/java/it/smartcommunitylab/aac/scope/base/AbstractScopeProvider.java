package it.smartcommunitylab.aac.scope.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public abstract class AbstractScopeProvider<S extends ApiScope> implements ApiScopeProvider<S> {

    private String authority;
    private String provider;

    protected String realm;

    protected AbstractScopeProvider(String authority, String provider) {
        this.authority = authority;
        this.provider = provider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SCOPE;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}
