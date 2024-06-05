package it.smartcommunitylab.aac.scope.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.smartcommunitylab.aac.scope.Keys;
import it.smartcommunitylab.aac.scope.model.Scope;
import javax.persistence.Transient;

public abstract class AbstractApiScope implements Scope {

    @JsonInclude
    @Transient
    private String authority;

    @JsonInclude
    @Transient
    private String provider;

    protected String realm;

    // TODO i18n
    protected String name;
    protected String description;

    protected AbstractApiScope(String authority, String provider) {
        this.authority = authority;
        this.provider = provider;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractApiScope() {
        this((String) null, (String) null);
    }

    @Override
    public String getType() {
        return Keys.RESOURCE_SCOPE;
    }

    // by default resources are associated to repositories, not providers
    // authorityId and provider are transient: implementations should avoid
    // persisting these attributes
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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
