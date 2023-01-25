package it.smartcommunitylab.aac.scope.base;

import java.util.Collection;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.scope.model.ApiResource;

public abstract class AbstractApiResource<S extends AbstractApiScope> implements ApiResource {

    @JsonInclude
    @Transient
    private String authority;

    @JsonInclude
    @Transient
    private String provider;

    protected String realm;

    protected String name;

    // TODO i18n
    protected String title;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public abstract Collection<? extends AbstractApiScope> getScopes();

    @Override
    public abstract Collection<? extends AbstractClaimDefinition> getClaims();

}
