package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.model.AttributeType;

public class BooleanClaimDefinition extends AbstractClaimDefinition {

    public BooleanClaimDefinition(String key) {
        super(key);
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.BOOLEAN;
    }

}
