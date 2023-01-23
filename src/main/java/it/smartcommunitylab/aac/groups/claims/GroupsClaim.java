package it.smartcommunitylab.aac.groups.claims;

import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.claims.model.StringClaimDefinition;

public class GroupsClaim extends StringClaim {

    public final static String KEY = "groups";
    public final static StringClaimDefinition DEFINITION;

    static {
        StringClaimDefinition def = new StringClaimDefinition(KEY);
        // groups is repeatable, one per membership
        def.setIsMultiple(true);
        DEFINITION = def;
    }

    public GroupsClaim(String group) {
        super(KEY, group);
    }

}
