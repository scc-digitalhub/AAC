package it.smartcommunitylab.aac.audit;

import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;

public class UserAuthenticationSuccessEvent extends AuthenticationSuccessEvent {

    private final String authority;
    private final String provider;
    private final String realm;

    public UserAuthenticationSuccessEvent(
            String authority, String provider, String realm,
            UserAuthenticationToken auth) {
        super(auth);
        Assert.hasText(authority, "authority is required");
        Assert.notNull(provider, "provider is required");
        Assert.notNull(realm, "realm is required");

        this.authority = authority;
        this.provider = provider;
        this.realm = realm;

    }

    public UserAuthenticationToken getAuthenticationToken() {
        return (UserAuthenticationToken) super.getAuthentication();
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public WebAuthenticationDetails getDetails() {
        return getAuthenticationToken().getWebAuthenticationDetails();
    }

}
