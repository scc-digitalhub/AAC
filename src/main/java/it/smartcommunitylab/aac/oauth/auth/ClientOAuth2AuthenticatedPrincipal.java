package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

public class ClientOAuth2AuthenticatedPrincipal extends DefaultOAuth2AuthenticatedPrincipal {

    public ClientOAuth2AuthenticatedPrincipal(String realm, String name, Map<String, Object> attributes,
            Collection<GrantedAuthority> authorities) {
        super(realm, name, attributes, authorities);
    }

    public String getClientId() {
        return name;
    }
}
