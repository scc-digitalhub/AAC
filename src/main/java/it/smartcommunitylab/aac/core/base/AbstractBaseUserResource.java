package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.smartcommunitylab.aac.core.model.UserResource;
import java.io.Serializable;
import javax.persistence.Transient;

public abstract class AbstractBaseUserResource implements UserResource, Serializable {

    @JsonInclude
    @Transient
    private String authority;

    @JsonInclude
    @Transient
    private String provider;

    protected AbstractBaseUserResource(String authority, String provider) {
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
    private AbstractBaseUserResource() {
        this((String) null, (String) null);
    }

    public abstract void setUserId(String userId);

    public abstract void setRealm(String realm);

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
}
