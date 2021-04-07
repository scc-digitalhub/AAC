package it.smartcommunitylab.aac.attributes.model;

import java.util.Date;

import it.smartcommunitylab.aac.model.AttributeType;

public class DateAttribute extends AbstractAttribute {

    private Date value;

    public DateAttribute(String key) {
        this.key = key;
    }

    public DateAttribute(String key, Date date) {
        this.key = key;
        this.value = date;
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
