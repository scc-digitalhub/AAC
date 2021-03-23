package it.smartcommunitylab.aac.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.ClientAttributes;

public class ClientDetails {

    // base attributes
    private final String clientId;
    private final String realm;
    private final String type;

    private String name;

    // providers enabled
    private Collection<String> providers;

    // hook functions
    private Map<String, String> hookFunctions;

    // attributes related to client
    // sets are bound to realm, stored with addressable keys
    // TODO
    private Map<String, ClientAttributes> attributes;

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    // TODO remove, should be left in token, we keep for interface compatibiilty
    private final Collection<GrantedAuthority> authorities;

    // we don't support account enabled/disabled
    private final boolean enabled;

    public ClientDetails(
            String clientId, String realm,
            String type,
            Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(clientId, "clientId can not be null");
        Assert.notNull(realm, "realm can not be null");
        Assert.notNull(type, "type is required");

        this.clientId = clientId;
        this.realm = realm;
        this.type = type;

        this.authorities = Collections.unmodifiableCollection(authorities);

        this.enabled = true;

    }

    public Collection<String> getProviders() {
        return providers;
    }

    public void setProviders(Collection<String> providers) {
        this.providers = providers;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public Map<String, ClientAttributes> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, ClientAttributes> attributes) {
        this.attributes = attributes;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRealm() {
        return realm;
    }

    public String getType() {
        return type;
    }

    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
