package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.model.AttributeType;

public class BooleanClaim extends AbstractClaim {

    private Boolean value;

    public BooleanClaim(String key) {
        this.key = key;
    }

    public BooleanClaim(String key, Boolean value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getAttributeType() {
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
