package it.smartcommunitylab.aac.scope.base;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ApiScopeApproval;

public abstract class AbstractScopeApprover<S extends ApiScope, A extends ApiScopeApproval>
        implements ScopeApprover<A> {

    private String authority;
    private String provider;

    protected String realm;

    protected final S scope;

    public AbstractScopeApprover(S scope) {
        Assert.notNull(scope, "scope can not be blank or null");
        this.scope = scope;

        // extract from scope definition
        this.authority = scope.getAuthority();
        this.provider = scope.getProvider();
        this.realm = scope.getRealm();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SCOPE_APPROVAL;
    }

    @Override
    public String getScope() {
        return scope.getScope();
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
