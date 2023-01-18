package it.smartcommunitylab.aac.claims.base;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.model.ClaimsSet;

public abstract class AbstractClaimsSet implements ClaimsSet {

    @JsonInclude
    @Transient
    private String authority;

    @JsonInclude
    @Transient
    private String provider;

    protected final String id;

    protected String realm;
    protected String resource;
    protected String namespace;

    protected AbstractClaimsSet(String authority, String provider, String id) {
        this.authority = authority;
        this.provider = provider;
        this.id = id;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractClaimsSet() {
        this((String) null, (String) null, (String) null);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CLAIMS_SET;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getClaimsSetId() {
        return id;
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

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
