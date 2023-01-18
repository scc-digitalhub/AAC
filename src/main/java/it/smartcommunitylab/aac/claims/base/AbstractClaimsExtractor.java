package it.smartcommunitylab.aac.claims.base;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.claims.model.ClaimsSet;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.model.User;

public abstract class AbstractClaimsExtractor extends AbstractProvider<AbstractClaimsSet>
        implements ClaimsExtractor<AbstractClaimsSet> {

    private final String resource;
    protected Map<String, AbstractClaimDefinition> definitions;
    protected String namespace;

    public AbstractClaimsExtractor(String authority, String provider, String realm, String resource) {
        super(authority, provider, realm);
        Assert.hasText(resource, "resource can not be null");

        this.resource = resource;
        this.definitions = Collections.emptyMap();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CLAIMS_SET;

    }

    protected void setDefinitions(Collection<AbstractClaimDefinition> definitions) {
        if (definitions != null) {
            this.definitions = definitions.stream().collect(Collectors.toMap(c -> c.getKey(), c -> c));
        }
    }

    public String getNamespace() {
        return namespace;
    }

    protected void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getResource() {
        return resource;
    }

    public void validate(ClaimsSet set) {
        // validate claimset against definition
        if (set == null) {
            throw new IllegalArgumentException("null set");
        }

        // match owner
        if (getAuthority().equals(set.getAuthority()) || getProvider().equals(set.getProvider())) {
            throw new IllegalArgumentException("invalid provider");
        }

        // match realm
        if (getRealm().equals(set.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // match resource
        if (getResource().equals(set.getResource())) {
            throw new IllegalArgumentException("resource-mismatch");
        }

        // namespace
        if (getNamespace() != null && !getNamespace().equals(set.getNamespace())) {
            throw new IllegalArgumentException("invalid-namespace");
        }

        // validate claims
        if (set.getClaims() != null) {
            set.getClaims().forEach(c -> {
                AbstractClaimDefinition def = definitions.get(c.getKey());
                if (def == null) {
                    throw new IllegalArgumentException("invalid-claim");
                }

                if (def.getAttributeType() != c.getAttributeType()) {
                    throw new IllegalArgumentException("invalid-claim");
                }
            });

            // check multiple
            Map<String, Long> keys = set.getClaims().stream()
                    .collect(Collectors.groupingBy(c -> c.getKey(), Collectors.counting()));

            keys.forEach((k, c) -> {
                AbstractClaimDefinition def = definitions.get(k);
                if (def == null) {
                    throw new IllegalArgumentException("invalid-claim");
                }

                if (c > 1 && !def.isMultiple()) {
                    throw new IllegalArgumentException("invalid-claim");
                }
            });
        }
    }

    @Override
    public DefaultUserClaimsSet extract(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        Collection<AbstractClaim> claims = extractUserClaims(user, client, scopes, extensions);
        if (claims == null) {
            // nothing to return
            return null;
        }

        // build a claimSet
        DefaultUserClaimsSet claimsSet = new DefaultUserClaimsSet(getAuthority(), getProvider());
        claimsSet.setUserId(user.getSubjectId());
        claimsSet.setResource(getResource());
        claimsSet.setNamespace(getNamespace());
        claimsSet.setRealm(getRealm());
        claimsSet.setClaims(claims);

        // validate
        validate(claimsSet);

        return claimsSet;
    }

    @Override
    public DefaultClientClaimsSet extract(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        Collection<AbstractClaim> claims = extractClientClaims(client, scopes, extensions);
        if (claims == null) {
            // nothing to return
            return null;
        }

        // build a claimSet
        DefaultClientClaimsSet claimsSet = new DefaultClientClaimsSet(getAuthority(), getProvider());
        claimsSet.setClientId(client.getClientId());
        claimsSet.setResource(getResource());
        claimsSet.setNamespace(getNamespace());
        claimsSet.setRealm(getRealm());
        claimsSet.setClaims(claims);

        // validate
        validate(claimsSet);

        return claimsSet;
    }

    protected abstract Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client,
            Collection<String> scopes, Map<String, Serializable> extensions);

    protected abstract Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions);

}
