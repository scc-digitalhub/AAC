package it.smartcommunitylab.aac.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.model.User;

public class AuthorityScopeApprover implements ScopeApprover {

    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private final String realm;
    private final String resourceId;
    private final String scope;
    private int duration;
    private Set<String> authorities;
    private Set<? extends GrantedAuthority> grantedAuthorities;

    private boolean requireAll = false;

    public AuthorityScopeApprover(String realm, String resourceId, String scope) {
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.realm = realm;
        this.resourceId = resourceId;
        this.scope = scope;
        this.authorities = Collections.emptySet();
        this.grantedAuthorities = null;
        this.duration = DEFAULT_DURATION_MS;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = new HashSet<>(authorities);
    }

    public void setGrantedAuthorities(Set<? extends GrantedAuthority> authorities) {
        this.grantedAuthorities = new HashSet<>(authorities);
    }

    public void setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
    }

    @Override
    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        boolean approved = false;

        if (grantedAuthorities != null) {
            // evaluate authorities
            Set<GrantedAuthority> userAuthorities = user.getAuthorities();
            if (requireAll) {
                // user needs to possess all the defined authorities
                approved = grantedAuthorities.stream().allMatch(a -> userAuthorities.contains(a));
            } else {
                // we look for at least one
                approved = grantedAuthorities.stream().anyMatch(a -> userAuthorities.contains(a));
            }

        } else {
            // fetch authorities from user, in relation to realm
            // if we have a realm set, read only global or realm matching
            // if we have no realm, evaluate those matching client realm
            String rlm = realm != null ? realm : client.getRealm();
            Set<String> userAuthorities = exportRealmAuthorities(rlm, user.getAuthorities());

            if (requireAll) {
                // user needs to possess all the defined authorities
                approved = authorities.stream().allMatch(a -> userAuthorities.contains(a));
            } else {
                // we look for at least one
                approved = authorities.stream().anyMatch(a -> userAuthorities.contains(a));
            }
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;
        return new Approval(resourceId, client.getClientId(), scope, duration, approvalStatus);
    }

    @Override
    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        boolean approved = false;

        if (grantedAuthorities != null) {
            // evaluate authorities
            Set<GrantedAuthority> clientAuthorities = client.getAuthorities();
            if (requireAll) {
                // client needs to possess all the defined authorities
                approved = grantedAuthorities.stream().allMatch(a -> clientAuthorities.contains(a));
            } else {
                // we look for at least one
                approved = grantedAuthorities.stream().anyMatch(a -> clientAuthorities.contains(a));
            }

        } else {
            String rlm = realm != null ? realm : client.getRealm();
            Set<String> clientAuthorities = exportRealmAuthorities(rlm, client.getAuthorities());

            if (requireAll) {
                // client needs to possess all the defined authorities
                approved = authorities.stream().allMatch(a -> clientAuthorities.contains(a));
            } else {
                // we look for at least one
                approved = authorities.stream().anyMatch(a -> clientAuthorities.contains(a));
            }
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;
        return new Approval(resourceId, client.getClientId(), scope, duration, approvalStatus);
    }

    @Override
    public String getRealm() {
        return realm;
    }

    private Set<String> exportRealmAuthorities(String realm, Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(a -> {
                    if (a instanceof SimpleGrantedAuthority) {
                        return a.getAuthority();
                    } else if (a instanceof RealmGrantedAuthority) {
                        if (((RealmGrantedAuthority) a).getRealm().equals(realm)) {
                            return ((RealmGrantedAuthority) a).getRole();
                        }
                    }

                    return null;
                })
                .filter(a -> StringUtils.hasText(a))
                .collect(Collectors.toSet());
    }
};