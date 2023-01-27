package it.smartcommunitylab.aac.claims.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.model.AttributeType;

public class InstantClaim extends AbstractClaim {

    private Instant value;

    public InstantClaim(String key) {
        this.key = key;
    }

    public InstantClaim(String key, Instant value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.INSTANT;
    }

    @Override
    public Instant getValue() {
        return value;
    }

    public void setValue(Instant value) {
        this.value = value;
    }

    @Override
    public Serializable exportValue() {
        if (value == null) {
            return null;
        }

        return DateTimeFormatter.ISO_INSTANT.format(value);
    }

}
