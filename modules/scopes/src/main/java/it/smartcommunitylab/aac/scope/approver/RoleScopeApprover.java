package it.smartcommunitylab.aac.scope.approver;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.Scope;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class RoleScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private int duration;

    private Set<String> roles;
    private boolean requireAll = false;

    public RoleScopeApprover(S scope) {
        super(scope);
        this.duration = DEFAULT_DURATION_S;
        this.roles = Collections.emptySet();
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setRoles(Set<String> roles) {
        this.roles = new HashSet<>(roles);
    }

    public void setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        logger.debug(
            "approve user {} for client {} with scopes {}",
            String.valueOf(user.getSubjectId()),
            String.valueOf(client.getClientId()),
            String.valueOf(scopes)
        );

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        Set<String> userRoles = user.getRealmRoles().stream().map(r -> r.getAuthority()).collect(Collectors.toSet());

        boolean approved = false;
        if (requireAll) {
            // user needs to possess all the defined roles
            approved = roles
                .stream()
                .allMatch(a -> {
                    return userRoles.stream().allMatch(c -> matches(c, a));
                });
        } else {
            // we look for at least one
            approved = roles
                .stream()
                .anyMatch(a -> {
                    return userRoles.stream().anyMatch(c -> matches(c, a));
                });
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        logger.debug(
            "approve user {} for client {} with scopes {}: {}",
            user.getSubjectId(),
            client.getClientId(),
            String.valueOf(scopes),
            approvalStatus
        );

        return new LimitedApiScopeApproval(
            scope.getResourceId(),
            scope.getScope(),
            user.getSubjectId(),
            client.getClientId(),
            duration,
            approvalStatus
        );
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        logger.debug("approve client {} with scopes {}", String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        // TODO update after replacing ClientDetails with Client model including roles
        //        Set<String> clientRoles = client.getRoles().stream().map(r -> r.getAuthority())
        //                .collect(Collectors.toSet());

        boolean approved = false;
        //        if (requireAll) {
        //            // client needs to possess all the defined roles
        //            approved = roles.stream().allMatch(a -> clientRoles.contains(a));
        //        } else {
        //            // we look for at least one
        //            approved = roles.stream().anyMatch(a -> clientRoles.contains(a));
        //        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        logger.debug(
            "approve client {} with scopes {}: {}",
            client.getClientId(),
            String.valueOf(scopes),
            approvalStatus
        );

        return new LimitedApiScopeApproval(
            scope.getResourceId(),
            scope.getScope(),
            client.getClientId(),
            client.getClientId(),
            duration,
            approvalStatus
        );
    }

    @Override
    public String getRealm() {
        return realm;
    }

    private boolean matches(String authority, String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }

        // spaceroles always have context otherwise are invalid
        String[] rr = authority.split(Pattern.quote(":"));
        if (rr.length != 2) {
            return false;
        }

        String rCtx = rr[0];
        String rRole = rr[1];

        // roles can be without context or absolute or contain wildcards
        if (!role.contains(":")) {
            // only role
            return rRole.toLowerCase().equals(role.toLowerCase());
        } else {
            // split
            String[] rs = role.split(Pattern.quote(":"));
            if (rs.length != 2) {
                return false;
            }

            String c = rs[0];
            String a = rs[1];

            boolean cmatch = false;
            boolean rmatch = rRole.toLowerCase().equals(a.toLowerCase());

            // check context with wildcards
            if (c.contains("*") || c.contains("?")) {
                // glob like
                cmatch = matchesGlob(rCtx.toLowerCase(), c.toLowerCase());
            } else {
                cmatch = rCtx.toLowerCase().equals(c.toLowerCase());
            }

            return cmatch && rmatch;
        }
    }

    public boolean matchesGlob(String text, String glob) {
        String rest = null;
        int pos = glob.indexOf('*');
        if (pos != -1) {
            rest = glob.substring(pos + 1);
            glob = glob.substring(0, pos);
        }

        if (glob.length() > text.length()) return false;

        // handle the part up to the first *
        for (int i = 0; i < glob.length(); i++) if (
            glob.charAt(i) != '?' && !glob.substring(i, i + 1).equalsIgnoreCase(text.substring(i, i + 1))
        ) return false;

        // recurse for the part after the first *, if any
        if (rest == null) {
            return glob.length() == text.length();
        } else {
            for (int i = glob.length(); i <= text.length(); i++) {
                if (matchesGlob(text.substring(i), rest)) return true;
            }
            return false;
        }
    }
}
