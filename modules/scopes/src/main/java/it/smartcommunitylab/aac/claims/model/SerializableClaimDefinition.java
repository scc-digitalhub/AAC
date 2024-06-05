package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.model.AttributeType;

public class SerializableClaimDefinition extends AbstractClaimDefinition {

    public SerializableClaimDefinition(String key) {
        super(key);
    }

    @Override
    public AttributeType getType() {
        return AttributeType.OBJECT;
    }
}
