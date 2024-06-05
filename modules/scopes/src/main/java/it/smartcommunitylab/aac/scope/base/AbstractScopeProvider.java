package it.smartcommunitylab.aac.scope.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.scope.approver.WhitelistScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScopeApproval;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;
import org.springframework.util.Assert;

public abstract class AbstractScopeProvider<S extends AbstractApiScope> implements ApiScopeProvider<S> {

    protected final S scope;
    protected AbstractScopeApprover<S, ? extends ApiScopeApproval> approver;

    public AbstractScopeProvider(String authority, String provider, String realm, S scope) {
        Assert.notNull(scope, "scope can not be null");

        this.scope = scope;
        // by default no scope approver is provided, use whitelist
        this.approver = new WhitelistScopeApprover<>(scope);
    }

    protected void setApprover(AbstractScopeApprover<S, ? extends ApiScopeApproval> approver) {
        if (approver != null) {
            this.approver = approver;
        }
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SCOPE;
    }

    @Override
    public S getScope() {
        return scope;
    }

    @Override
    public AbstractScopeApprover<S, ?> getScopeApprover() {
        return approver;
    }
}
