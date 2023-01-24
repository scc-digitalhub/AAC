package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.claims.model.SerializableClaimDefinition;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class BasicProfileClaim extends AbstractProfileClaim<BasicProfile> {

    public final static String KEY = "profile";
    public final static SerializableClaimDefinition DEFINITION;

    static {
        SerializableClaimDefinition def = new SerializableClaimDefinition(KEY);
        def.setIsMultiple(false);
        DEFINITION = def;
    }

    public BasicProfileClaim(BasicProfile profile) {
        super(KEY, profile);
    }
}
