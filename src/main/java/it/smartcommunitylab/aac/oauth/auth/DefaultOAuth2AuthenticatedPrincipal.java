package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.Assert;

public class DefaultOAuth2AuthenticatedPrincipal implements OAuth2AuthenticatedPrincipal, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    protected final String name;

    protected final String realm;

    protected final Map<String, Object> attributes;

    private final Collection<GrantedAuthority> authorities;

    public DefaultOAuth2AuthenticatedPrincipal(
        String realm,
        String name,
        Map<String, Object> attributes,
        Collection<GrantedAuthority> authorities
    ) {
        Assert.notEmpty(attributes, "attributes cannot be empty");

        this.realm = (realm != null ? realm : (String) attributes.get("realm"));
        this.name = (name != null ? name : (String) attributes.get("sub"));

        this.attributes = Collections.unmodifiableMap(attributes);
        this.authorities = Collections.unmodifiableCollection(authorities);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getRealm() {
        return realm;
    }
}
