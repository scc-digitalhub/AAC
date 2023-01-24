package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.claims.model.SerializableClaimDefinition;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;

public class EmailsProfileClaim extends AbstractProfileClaim<EmailProfile> {

    public final static String KEY = "emails";
    public final static SerializableClaimDefinition DEFINITION;

    static {
        SerializableClaimDefinition def = new SerializableClaimDefinition(KEY);
        // email is repeatable
        def.setIsMultiple(true);
        DEFINITION = def;
    }

    public EmailsProfileClaim(EmailProfile profile) {
        super(KEY, profile);
    }
}
