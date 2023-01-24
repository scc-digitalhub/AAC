package it.smartcommunitylab.aac.profiles.claims;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.claims.model.ClaimDefinition;
import it.smartcommunitylab.aac.claims.model.SerializableClaimDefinition;
import it.smartcommunitylab.aac.profiles.model.CustomProfile;

public class CustomProfileClaim extends AbstractProfileClaim<CustomProfile> {

    private final SerializableClaimDefinition definition;

    public CustomProfileClaim(CustomProfile profile) {
        super(profile.getIdentifier(), profile);
        Assert.notNull(profile, "profile can not be null");
        Assert.hasText(profile.getIdentifier(), "identifier can not be null or empty");

        this.definition = DEFINITION(key);
    }

    public ClaimDefinition definition() {
        return definition;
    }

    public static SerializableClaimDefinition DEFINITION(String identifier) {
        SerializableClaimDefinition def = new SerializableClaimDefinition(identifier);
        // profile is repeatable
        def.setIsMultiple(true);
        return def;
    }
}
