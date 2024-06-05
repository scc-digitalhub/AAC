package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.model.AttributeType;
import java.time.LocalDateTime;

public class DateTimeClaim extends AbstractClaim {

    private LocalDateTime value;

    public DateTimeClaim(String key) {
        this.key = key;
    }

    public DateTimeClaim(String key, LocalDateTime value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.DATETIME;
    }

    @Override
    public LocalDateTime getValue() {
        return value;
    }

    public void setValue(LocalDateTime value) {
        this.value = value;
    }
}
