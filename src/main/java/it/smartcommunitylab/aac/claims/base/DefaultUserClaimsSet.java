package it.smartcommunitylab.aac.claims.base;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.claims.model.Claim;
import it.smartcommunitylab.aac.core.model.UserResource;

public class DefaultUserClaimsSet extends AbstractClaimsSet implements UserResource {

    private String userId;
    private Map<String, AbstractClaim> claims;

    public DefaultUserClaimsSet(String authority, String provider) {
        this(authority, provider, UUID.randomUUID().toString());
    }

    public DefaultUserClaimsSet(String authority, String provider, String id) {
        super(authority, provider, id);
        claims = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getSubjectId() {
        return userId;
    }

    @Override
    public Collection<Claim> getClaims() {
        return Collections.unmodifiableCollection(claims.values());
    }

    public void addClaim(AbstractClaim claim) {
        if (claim != null) {
            if (claim.getId() == null) {
                // generate an id
                claim.setId(UUID.randomUUID().toString());
            }

            claims.put(claim.getId(), claim);
        }
    }

    public AbstractClaim removeClaim(String id) {
        AbstractClaim c = claims.get(id);
        if (c != null) {
            claims.remove(id);
        }

        return c;
    }

    public void setClaims(Collection<AbstractClaim> claims) {
        if (claims != null) {
            this.claims = claims.stream()
                    .map(c -> {
                        if (c.getId() == null) {
                            // generate an id
                            c.setId(UUID.randomUUID().toString());
                        }
                        return c;
                    })
                    .collect(Collectors.toMap(c -> c.getId(), c -> c));
        }
    }

    @Override
    public String getUuid() {
        return id;
    }
}
