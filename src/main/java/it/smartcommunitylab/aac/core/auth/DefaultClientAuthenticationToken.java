package it.smartcommunitylab.aac.core.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ClientDetails;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public abstract class DefaultClientAuthenticationToken extends ClientAuthentication {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // client details
    protected ClientDetails clientDetails;

    // web authentication details
    protected WebAuthenticationDetails webAuthenticationDetails;

    protected String authenticationMethod;

    public DefaultClientAuthenticationToken(String clientId) {
        super(clientId);
    }

    public DefaultClientAuthenticationToken(String clientId, Collection<? extends GrantedAuthority> authorities) {
        super(clientId, authorities);
    }

    @Override
    public Object getDetails() {
        return clientDetails;
    }

    public ClientDetails getClient() {
        return clientDetails;
    }

    public void setClient(ClientDetails clientDetails) {
        this.clientDetails = clientDetails;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public WebAuthenticationDetails getWebAuthenticationDetails() {
        return webAuthenticationDetails;
    }

    public void setWebAuthenticationDetails(WebAuthenticationDetails webAuthenticationDetails) {
        this.webAuthenticationDetails = webAuthenticationDetails;
    }

    public void setDetails(ClientDetails details) {
        this.clientDetails = details;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }
}
