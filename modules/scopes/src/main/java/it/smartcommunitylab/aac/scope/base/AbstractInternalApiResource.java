package it.smartcommunitylab.aac.scope.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.util.Assert;

public abstract class AbstractInternalApiResource<S extends AbstractInternalApiScope, C extends AbstractClaimDefinition>
    extends AbstractApiResource<S> {

    private final String uri;
    private final String resourceId;
    private final String id;

    private Set<S> scopes;
    private Set<C> claims;

    //    public AbstractInternalApiResource(String realm, String uri, String resourceId) {
    //        this(SystemKeys.AUTHORITY_INTERNAL, realm, uri, resourceId);
    //    }

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
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String getResource() {
        // use URI + id as static schema for indicator
        return uri + SystemKeys.URN_SEPARATOR + id;
    }

    @Override
    public String getNamespace() {
        // internal api are not namespaced
        return null;
    }

    @Override
    public Collection<S> getScopes() {
        return Collections.unmodifiableSet(scopes);
    }

    public void setScopes(Collection<S> scopes) {
        Assert.notNull(scopes, "scopes can not be null or empty");
        this.scopes = Collections.unmodifiableSet(new TreeSet<>(scopes));
    }

    @SuppressWarnings("unchecked")
    public void setScopes(S... scopes) {
        setScopes(Arrays.asList(scopes));
    }

    public Set<C> getClaims() {
        return claims;
    }

    public void setClaims(Collection<C> claims) {
        Assert.notNull(claims, "claims can not be null or empty");
        this.claims = Collections.unmodifiableSet(new TreeSet<>(claims));
    }

    @SuppressWarnings("unchecked")
    public void setClaims(C... claims) {
        setClaims(Arrays.asList(claims));
    }

    // i18n: use id as key for language files
    @Override
    public String getName() {
        return resourceId;
    }

    @Override
    public String getTitle() {
        return "resources." + resourceId + ".title";
    }

    @Override
    public String getDescription() {
        return "resources." + resourceId + ".description";
    }
}
