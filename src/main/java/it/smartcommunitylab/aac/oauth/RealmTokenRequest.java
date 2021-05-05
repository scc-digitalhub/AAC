package it.smartcommunitylab.aac.oauth;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;

public class RealmTokenRequest extends TokenRequest {

    private String realm;
    private Set<String> resourceIds = new HashSet<String>();
    private Set<String> audience = new HashSet<>();
    private Collection<? extends GrantedAuthority> authorities = new HashSet<GrantedAuthority>();

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Set<String> audience) {
        this.audience = audience;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    public RealmTokenRequest(Map<String, String> requestParameters,
            String clientId, String realm,
            Collection<String> scope, Set<String> resourceIds,
            Collection<? extends GrantedAuthority> authorities,
            String grantType) {
        super(requestParameters, clientId, scope, grantType);
        if (resourceIds != null) {
            this.resourceIds = new HashSet<String>(resourceIds);
        }
        if (authorities != null) {
            this.authorities = new HashSet<GrantedAuthority>(authorities);
        }
        this.realm = realm;
    }

    @Override
    public OAuth2Request createOAuth2Request(ClientDetails client) {

        Map<String, String> requestParameters = getRequestParameters();
        HashMap<String, String> modifiable = new HashMap<String, String>(requestParameters);
        // Remove password if present to prevent leaks
        modifiable.remove("password");
        modifiable.remove("client_secret");
        // Add grant type so it can be retrieved from OAuth2Request
        modifiable.put(OAuth2Utils.GRANT_TYPE, getGrantType());

        RealmOAuth2Request request = new RealmOAuth2Request(modifiable, client.getClientId(), client.getAuthorities(),
                true,
                this.getScope(),
                client.getResourceIds(), null, null, null);

        request.setRealm(getRealm());

        return request;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        // we use as storage for request duration, also for realm-filtered authorities
        // to be exposed to
        // service approval, we need those in flight bound to request ( since we store
        // this in db)
        return authorities;
    }

}
