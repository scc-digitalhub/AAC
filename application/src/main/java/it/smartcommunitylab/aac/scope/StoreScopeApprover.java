/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.scope;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.users.model.User;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.Assert;

public class StoreScopeApprover implements ScopeApprover {

    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private SearchableApprovalStore approvalStore;

    private final String realm;
    private final String resourceId;
    private final String scope;

    private String userId;

    public StoreScopeApprover(String realm, String resourceId, String scope) {
        Assert.notNull(realm, "realm can not be null");
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.realm = realm;
        this.resourceId = resourceId;
        this.scope = scope;
        // userId is used for store lookups
        this.userId = resourceId;
    }

    public void setApprovalStore(SearchableApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
        throws InvalidDefinitionException, SystemException {
        if (approvalStore == null) {
            return null;
        }

        if (!this.scope.equals(scope)) {
            return null;
        }

        Approval approval = approvalStore.findApproval(userId, user.getUserId(), scope);
        if (approval == null) {
            return null;
        }

        if (!approval.isCurrentlyActive()) {
            // cleanup expired
            approvalStore.revokeApprovals(Collections.singleton(approval));

            return null;
        }

        return approval;
    }

    @Override
    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
        throws InvalidDefinitionException, SystemException {
        if (approvalStore == null) {
            return null;
        }

        if (!this.scope.equals(scope)) {
            return null;
        }

        Approval approval = approvalStore.findApproval(userId, client.getClientId(), scope);
        if (approval == null) {
            return null;
        }

        if (!approval.isCurrentlyActive()) {
            // cleanup expired
            approvalStore.revokeApprovals(Collections.singleton(approval));

            return null;
        }

        return approval;
    }

    @Override
    public String getRealm() {
        return realm;
    }
}
