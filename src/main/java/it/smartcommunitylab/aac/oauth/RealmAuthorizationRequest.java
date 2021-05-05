package it.smartcommunitylab.aac.oauth;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Request;

public class RealmAuthorizationRequest extends AuthorizationRequest {

    private String realm;
    private Set<String> audience = new HashSet<>();

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public RealmAuthorizationRequest(
            Map<String, String> authorizationParameters, Map<String, String> approvalParameters,
            String clientId, String realm,
            Set<String> scope, Set<String> resourceIds,
            Collection<? extends GrantedAuthority> authorities, boolean approved,
            String state, String redirectUri,
            Set<String> responseTypes) {
        super(authorizationParameters, approvalParameters,
                clientId,
                scope, resourceIds,
                authorities, approved,
                state, redirectUri,
                responseTypes);
        this.realm = realm;
    }

    @Override
    public RealmOAuth2Request createOAuth2Request() {
        return new RealmOAuth2Request(getRequestParameters(),
                getClientId(), getRealm(), getAuthorities(),
                isApproved(),
                getScope(), getResourceIds(),
                getRedirectUri(), getResponseTypes(), getExtensions());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // we use as storage for request duration, also for realm-filtered authorities
        // to be exposed to
        // service approval, we need those in flight bound to request ( since we store
        // this in db)
        return super.getAuthorities();
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Set<String> audience) {
        this.audience = audience;
    }

}
