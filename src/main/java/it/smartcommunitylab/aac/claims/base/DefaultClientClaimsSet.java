package it.smartcommunitylab.aac.claims.base;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.claims.model.Claim;
import it.smartcommunitylab.aac.core.model.ClientResource;

public class DefaultClientClaimsSet extends AbstractClaimsSet implements ClientResource {

    private String clientId;
    private Map<String, AbstractClaim> claims;

    public DefaultClientClaimsSet(String authority, String provider) {
        this(authority, provider, UUID.randomUUID().toString());
    }

    public DefaultClientClaimsSet(String authority, String provider, String id) {
        super(authority, provider, id);
        claims = new HashMap<>();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getSubjectId() {
        return clientId;
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

    public void setClaims(Collection<? extends AbstractClaim> claims) {
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

}
