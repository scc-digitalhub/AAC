package it.smartcommunitylab.aac.claims.base;

import java.util.HashSet;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.scope.base.AbstractApiResource;

public abstract class AbstractResourceClaimsSetExtractor<R extends AbstractApiResource<?>>
        extends AbstractClaimsSetExtractor {

    protected final R res;

    public AbstractResourceClaimsSetExtractor(R res) {
        super(res.getAuthority(), res.getProvider(), res.getRealm(), res.getResource());
        Assert.notNull(res, "resource can not be null");
        this.res = res;

        if (res.getClaims() != null) {
            this.setDefinitions(new HashSet<>(res.getClaims()));
        }
    }

}
