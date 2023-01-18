package it.smartcommunitylab.aac.roles.claims;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.claims.model.StringClaimDefinition;

public class RolesClaim extends StringClaim {

    public final static String KEY = "roles";
    public final static AbstractClaimDefinition DEFINITION;

    static {
        StringClaimDefinition def = new StringClaimDefinition(KEY);
        // roles is repeatable, one per membership
        def.setIsMultiple(true);
        DEFINITION = def;
    }

    public RolesClaim(String role) {
        super(KEY, role);
    }

}
