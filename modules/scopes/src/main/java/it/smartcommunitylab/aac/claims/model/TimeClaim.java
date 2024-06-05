package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.model.AttributeType;
import java.time.LocalTime;

public class TimeClaim extends AbstractClaim {

    private LocalTime value;

    public TimeClaim(String key) {
        this.key = key;
    }

    public TimeClaim(String key, LocalTime value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.TIME;
    }

    @Override
    public LocalTime getValue() {
        return value;
    }

    public void setValue(LocalTime value) {
        this.value = value;
    }
}
