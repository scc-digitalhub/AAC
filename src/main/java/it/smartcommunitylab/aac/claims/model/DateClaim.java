package it.smartcommunitylab.aac.claims.model;

import java.time.LocalDate;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.model.AttributeType;

public class DateClaim extends AbstractClaim {

    private LocalDate value;

    public DateClaim(String key) {
        this.key = key;
    }

    public DateClaim(String key, LocalDate value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.DATE;
    }

    @Override
    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate value) {
        this.value = value;
    }

}
