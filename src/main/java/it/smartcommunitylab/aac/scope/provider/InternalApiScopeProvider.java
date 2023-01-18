package it.smartcommunitylab.aac.scope.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AdminApiResource;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.approver.CombinedScopeApprover;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApproval;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;

@Deprecated
public class InternalApiScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public InternalApiScopeProvider(AbstractInternalApiScope scope) {
        super(SystemKeys.AUTHORITY_INTERNAL, scope.getProvider(), scope.getRealm(), scope);

        // build approver
        List<AbstractScopeApprover<AbstractInternalApiScope, ? extends AbstractScopeApproval>> approvers = new ArrayList<>();
        if (scope.getAuthorities() != null && !scope.getAuthorities().isEmpty()) {
            // do not scope admin resources
            if (scope.getApiResourceId().equals(AdminApiResource.RESOURCE_ID)) {
                approvers.add(buildAuthorityApprover(false));
            }

            // scope to realm by default
            approvers.add(buildAuthorityApprover(true));

        }

        if (StringUtils.hasText(scope.getSubjectType())) {
            approvers.add(buildSubjectTypeApprover());
        }

        if (approvers.size() == 1) {
            // use the single approver
            setApprover(approvers.iterator().next());
        } else if (approvers.size() > 0) {
            // use a combined approver to require consensus
            setApprover(new CombinedScopeApprover<AbstractInternalApiScope>(scope, approvers));
        }

        // whitelist by default

    }

    protected SubjectTypeScopeApprover<AbstractInternalApiScope> buildSubjectTypeApprover() {
        // build approver based on type
        SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(scope);
        sa.setSubjectType(scope.getSubjectType());

        return sa;
    }

    protected AuthorityScopeApprover<AbstractInternalApiScope> buildAuthorityApprover(boolean scoped) {
        AuthorityScopeApprover<AbstractInternalApiScope> sa = new AuthorityScopeApprover<>(scope);

        if (scoped) {
            // map all to realm role,
            // will work only for realm matching requests thanks to
            // user translation, ie a client can ask for a user to
            // consent scopes for managing the client's realm, if the user has those
            // authorities. We don't support a global client
            Set<? extends GrantedAuthority> authorities = scope.getAuthorities().stream()
                    .map(a -> new RealmGrantedAuthority(scope.getRealm(), a)).collect(Collectors.toSet());
            sa.setGrantedAuthorities(authorities);
        } else {

            // ask exact match on global authority
            Set<? extends GrantedAuthority> authorities = scope.getAuthorities().stream()
                    .map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toSet());
            sa.setGrantedAuthorities(authorities);
        }

        return sa;
    }

}
