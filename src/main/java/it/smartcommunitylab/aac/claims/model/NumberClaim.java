package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;

public class NumberClaim extends AbstractClaim {

    private Number value;

    public NumberClaim(String key) {
        this.key = key;
    }

    public NumberClaim(String key, Number value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.NUMBER;
    }

    @Override
    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }
}
