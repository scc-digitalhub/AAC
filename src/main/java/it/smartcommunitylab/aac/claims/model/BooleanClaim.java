package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;

public class BooleanClaim extends AbstractClaim {

    private Boolean value;

    public BooleanClaim(String key) {
        this.key = key;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.BOOLEAN;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

}
