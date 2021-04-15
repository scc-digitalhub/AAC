package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;

public class WhitelistScopeApprover implements ScopeApprover {

    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private final String realm;
    private final String resourceId;
    private final String scope;
    private int duration;

    public WhitelistScopeApprover(String realm, String resourceId, String scope) {
        Assert.notNull(realm, "realm can not be null");
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.realm = realm;
        this.resourceId = resourceId;
        this.scope = scope;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        return new Approval(resourceId, client.getClientId(), scope, duration, ApprovalStatus.APPROVED);
    }

    @Override
    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        return new Approval(resourceId, client.getClientId(), scope, duration, ApprovalStatus.APPROVED);
    }

    @Override
    public String getRealm() {
        return realm;
    }

};