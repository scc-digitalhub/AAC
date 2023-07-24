package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;
import java.util.Date;

public class DateClaim extends AbstractClaim {

    private Date value;

    public DateClaim(String key) {
        this.key = key;
    }

    public DateClaim(String key, Date value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.DATE;
    }

    @Override
    public Date getValue() {
        return value;
    }

    public void setValue(Date value) {
        this.value = value;
    }
}
