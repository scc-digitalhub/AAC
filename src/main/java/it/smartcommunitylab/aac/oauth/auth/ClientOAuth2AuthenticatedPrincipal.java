package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

import it.smartcommunitylab.aac.SystemKeys;

public class ClientOAuth2AuthenticatedPrincipal extends DefaultOAuth2AuthenticatedPrincipal {
    
    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public ClientOAuth2AuthenticatedPrincipal(String realm, String name, Map<String, Object> attributes,
            Collection<GrantedAuthority> authorities) {
        super(realm, name, attributes, authorities);
    }

    public String getClientId() {
        return name;
    }
}
