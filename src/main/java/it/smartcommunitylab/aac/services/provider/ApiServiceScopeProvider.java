package it.smartcommunitylab.aac.services.provider;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.approver.CombinedScopeApprover;
import it.smartcommunitylab.aac.scope.approver.DelegateScopeApprover;
import it.smartcommunitylab.aac.scope.approver.RoleScopeApprover;
import it.smartcommunitylab.aac.scope.approver.ScriptScopeApprover;
import it.smartcommunitylab.aac.scope.approver.StoreScopeApprover;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.approver.WhitelistScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApproval;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.services.model.ApiServiceScope;

public class ApiServiceScopeProvider extends AbstractScopeProvider<ApiServiceScope> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ApiServiceScopeProvider(ApiServiceScope scope,
            ScriptExecutionService executionService, SearchableApprovalStore approvalStore) {
        super(SystemKeys.AUTHORITY_SERVICE, scope.getProvider(), scope.getRealm(), scope);

        // build approver evaluating all requirements
        logger.debug("build scope approvers for {}", scope.getScope());
        List<AbstractScopeApprover<ApiServiceScope, ? extends AbstractScopeApproval>> approvers = new ArrayList<>();

        if (scope.isApprovalRequired() && approvalStore != null) {
            // store approver
            StoreScopeApprover<ApiServiceScope> sa = new StoreScopeApprover<>(scope);
            sa.setApprovalStore(approvalStore);
            // serviceId is the owner
            sa.setUserId(scope.getServiceId());
            approvers.add(sa);
        }

        if (scope.getApprovalType() != null) {
            // subject type
            SubjectTypeScopeApprover<ApiServiceScope> sa = new SubjectTypeScopeApprover<>(scope);
            sa.setSubjectType(scope.getApprovalType());
            approvers.add(sa);
        }

        if (scope.getApprovalRoles() != null && !scope.getApprovalRoles().isEmpty()) {
            // realm roles
            // note: we use (any role) as policy
            RoleScopeApprover<ApiServiceScope> ra = new RoleScopeApprover<>(scope);
            ra.setRoles(scope.getApprovalRoles());
            ra.setRequireAll(false);
            approvers.add(ra);
        }
        if (StringUtils.hasText(scope.getApprovalFunction()) && executionService != null) {
            // script function
            ScriptScopeApprover<ApiServiceScope> sa = new ScriptScopeApprover<>(scope);
            sa.setExecutionService(executionService);
            sa.setFunctionCode(scope.getApprovalFunction());
            approvers.add(sa);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("scope: {}", String.valueOf(scope));
            logger.trace("approvers: {}", String.valueOf(approvers));
        }

        if (approvers.size() == 0) {
            // nothing to do, use whitelist
            this.approver = new WhitelistScopeApprover<>(scope);
            return;
        }

        // check if any composition is required
        if (approvers.size() == 1) {
            // single criteria, use as is
            this.approver = approvers.get(0);
        } else {
            if (scope.isApprovalAny()) {
                // use OR
                this.approver = new DelegateScopeApprover<ApiServiceScope>(scope, approvers);

            } else {
                // use AND
                this.approver = new CombinedScopeApprover<ApiServiceScope>(scope, approvers);

            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("approver: {}", String.valueOf(approver));
        }

    }

}
