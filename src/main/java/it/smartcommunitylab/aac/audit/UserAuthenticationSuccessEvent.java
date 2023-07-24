package it.smartcommunitylab.aac.audit;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.util.Assert;

//TODO add custom serializer
//TODO add subtype inference

public class UserAuthenticationSuccessEvent extends AuthenticationSuccessEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String provider;
    private final String realm;

    public UserAuthenticationSuccessEvent(String authority, String provider, String realm, UserAuthentication auth) {
        super(auth);
        Assert.hasText(authority, "authority is required");
        Assert.notNull(provider, "provider is required");
        Assert.notNull(realm, "realm is required");

        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
    }

    public UserAuthentication getAuthenticationToken() {
        return (UserAuthentication) super.getAuthentication();
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
