package it.smartcommunitylab.aac.roles.claims;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.claims.model.StringClaimDefinition;

@Deprecated
public class SpacesClaim extends StringClaim {

    public final static String KEY = "spaces";
    public final static AbstractClaimDefinition DEFINITION;

    static {
        StringClaimDefinition def = new StringClaimDefinition(KEY);
        // roles is repeatable, one per membership
        def.setIsMultiple(true);
        DEFINITION = def;
    }

    public SpacesClaim(String role) {
        super(KEY, role);
    }

}
