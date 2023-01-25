package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.model.AttributeType;

public class NumberClaimDefinition extends AbstractClaimDefinition {

    public NumberClaimDefinition(String key) {
        super(key);
    }

    @Override
    public AttributeType getType() {
        return AttributeType.NUMBER;
    }

}
