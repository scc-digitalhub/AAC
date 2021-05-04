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
    private String description;

    // providers enabled
    private Collection<String> providers;

    // scopes enabled
    private Collection<String> scopes;
    private Collection<String> resourceIds;

    // hook functions
    private Map<String, String> hookFunctions;
    private Map<String, String> hookWebUrls;
    private String hookUniqueSpaces;

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

    public Collection<String> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<String> scopes) {
        this.scopes = scopes;
    }

    public Collection<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Collection<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public Map<String, String> getHookWebUrls() {
        return hookWebUrls;
    }

    public void setHookWebUrls(Map<String, String> hookWebUrls) {
        this.hookWebUrls = hookWebUrls;
    }

    public String getHookUniqueSpaces() {
        return hookUniqueSpaces;
    }

    public void setHookUniqueSpaces(String hookUniqueSpaces) {
        this.hookUniqueSpaces = hookUniqueSpaces;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
