package it.smartcommunitylab.aac.oauth;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;

public class RealmOAuth2Request extends OAuth2Request {

    private String realm;

    private TokenRequest refresh = null;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public RealmOAuth2Request(Map<String, String> requestParameters,
            String clientId, String realm,
            Collection<? extends GrantedAuthority> authorities, boolean approved, Set<String> scope,
            Set<String> resourceIds, String redirectUri, Set<String> responseTypes,
            Map<String, Serializable> extensionProperties) {
        super(requestParameters, clientId,
                authorities, approved, scope,
                resourceIds, redirectUri, responseTypes,
                extensionProperties);
        this.realm = realm;
    }

    public RealmOAuth2Request(Map<String, String> requestParameters, String clientId,
            Collection<? extends GrantedAuthority> authorities, boolean approved, Set<String> scope,
            Set<String> resourceIds, String redirectUri, Set<String> responseTypes,
            Map<String, Serializable> extensionProperties) {
        super(requestParameters, clientId,
                authorities, approved, scope,
                resourceIds, redirectUri, responseTypes,
                extensionProperties);

    }

    /**
     * Update the request parameters and return a new object with the same
     * properties except the parameters.
     * 
     * @param parameters new parameters replacing the existing ones
     * @return a new OAuth2Request
     */
    @Override
    public RealmOAuth2Request createOAuth2Request(Map<String, String> parameters) {
        RealmOAuth2Request request = new RealmOAuth2Request(parameters, getClientId(), getAuthorities(), isApproved(),
                getScope(), getResourceIds(),
                getRedirectUri(), getResponseTypes(), getExtensions());

        request.realm = this.realm;
        return request;
    }

    /**
     * Update the scope and create a new request. All the other properties are the
     * same (including the request parameters).
     * 
     * @param scope the new scope
     * @return a new request with the narrowed scope
     */
    @Override
    public RealmOAuth2Request narrowScope(Set<String> scope) {
        RealmOAuth2Request request = new RealmOAuth2Request(getRequestParameters(), getClientId(), getAuthorities(),
                isApproved(),
                scope, getResourceIds(),
                getRedirectUri(), getResponseTypes(), getExtensions());
        request.refresh = this.refresh;
        request.realm = this.realm;

        return request;
    }

    @Override
    public RealmOAuth2Request refresh(TokenRequest tokenRequest) {
        RealmOAuth2Request request = new RealmOAuth2Request(getRequestParameters(), getClientId(), getAuthorities(),
                isApproved(),
                getScope(), getResourceIds(),
                getRedirectUri(), getResponseTypes(), getExtensions());
        request.refresh = tokenRequest;
        request.realm = this.realm;
        return request;
    }

}
