package it.smartcommunitylab.aac.scope.base;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.model.ApiResource;

public abstract class AbstractApiResource implements ApiResource {

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

    protected AbstractApiResource(String authority, String provider) {
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
    private AbstractApiResource() {
        this((String) null, (String) null);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_API_RESOURCE;
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
