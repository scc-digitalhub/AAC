package it.smartcommunitylab.aac.claims.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.model.AttributeType;

/*
 * A claim type supporting OBJECT in serializable form.
 * Key is optional, to merge with other claims under the namespace set null
 * 
 * We let type resettable but we expect the definition to match the content.
 * Changing the type will render this claim opaque for service.
 */
public class SerializableClaim extends AbstractClaim {

    private AttributeType type;

    private Serializable value;

    public SerializableClaim(String key) {
        this.key = key;
        this.type = AttributeType.OBJECT;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }
}
