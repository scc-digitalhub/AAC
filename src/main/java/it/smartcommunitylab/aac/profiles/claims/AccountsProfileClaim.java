package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.claims.model.SerializableClaimDefinition;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;

public class AccountsProfileClaim extends AbstractProfileClaim<AccountProfile> {

    public final static String KEY = "accounts";
    public final static SerializableClaimDefinition DEFINITION;

    static {
        SerializableClaimDefinition def = new SerializableClaimDefinition(KEY);
        // accounts is repeatable
        def.setIsMultiple(true);
        DEFINITION = def;
    }

    public AccountsProfileClaim(AccountProfile profile) {
        super(KEY, profile);
    }
}
