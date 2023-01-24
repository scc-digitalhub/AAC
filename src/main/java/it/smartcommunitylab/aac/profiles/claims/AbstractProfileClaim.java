package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;

public abstract class AbstractProfileClaim<P extends AbstractProfile> extends SerializableClaim {

    public AbstractProfileClaim(String key, P profile) {
        super(key, profile);
    }
}