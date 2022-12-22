package it.smartcommunitylab.aac.scope.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

public abstract class AbstractInternalApiResource extends AbstractApiResource {

    private final String uri;
    private final String resourceId;
    private final String id;

    private Set<AbstractInternalApiScope> scopes;

    public AbstractInternalApiResource(String realm, String uri, String resourceId) {
        this(SystemKeys.AUTHORITY_INTERNAL, realm, uri, resourceId);
    }

    public AbstractInternalApiResource(String authority, String realm, String uri, String resourceId) {
        // realm is used to build a unique providerId
        super(authority, resourceId + SystemKeys.URN_SEPARATOR + realm);
        Assert.hasText(realm, "realm can not be null or empty");
        Assert.hasText(uri, "uri can not be null or empty");
        Assert.hasText(resourceId, "resourceId can not be null or empty");

        this.realm = realm;
        this.uri = uri;
        this.resourceId = resourceId;

        // same schema for unique id
        this.id = resourceId + SystemKeys.ID_SEPARATOR + realm;

        this.scopes = Collections.emptySet();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getApiResourceId() {
        return resourceId;

    }

    @Override
    public String getResource() {
        // use URI + id as static schema
        return uri + SystemKeys.URN_SEPARATOR + id;
    }

    @Override
    public String getNamespace() {
        // internal api are not namespaced
        return null;
    }

    @Override
    public Collection<AbstractInternalApiScope> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<AbstractInternalApiScope> scopes) {
        Assert.notNull(scopes, "scopes can not be null or empty");
        this.scopes = Collections.unmodifiableSet(new TreeSet<>(scopes));
    }

    public void setScopes(AbstractInternalApiScope... scopes) {
        setScopes(Arrays.asList(scopes));
    }

}