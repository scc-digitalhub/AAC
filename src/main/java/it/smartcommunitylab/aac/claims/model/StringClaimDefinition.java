package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.model.AttributeType;

public class StringClaimDefinition extends AbstractClaimDefinition {

    public StringClaimDefinition(String key) {
        super(key);
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.STRING;
    }

}
