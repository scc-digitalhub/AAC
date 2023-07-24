package it.smartcommunitylab.aac.core.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ClientDetails;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public abstract class ClientAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // clientId is principal
    protected final String principal;

    // keep realm separated to support clients authentication in different realms
    protected String realm;

    public ClientAuthentication(String clientId) {
        super(null);
        this.principal = clientId;
        setAuthenticated(false);
    }

    public ClientAuthentication(String clientId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = clientId;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public String getCredentials() {
        return null;
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    @Override
    public String getName() {
        return principal;
    }

    public String getClientId() {
        return this.principal;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"
            );
        }

        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }

    public abstract ClientDetails getClient();

    public abstract void setClient(ClientDetails details);

    public abstract String getAuthenticationMethod();

    public abstract WebAuthenticationDetails getWebAuthenticationDetails();
}
