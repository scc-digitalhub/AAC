package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;

/*
 * An attribute type supporting OBJECT in serializable form.
 *
 * We let type resettable but we expect the definition to match the content.
 * Changing the type will render this attribute opaque for service.
 */
public class SerializableAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private AttributeType type;

    private Serializable value;

    public SerializableAttribute(String key) {
        this.key = key;
        this.type = AttributeType.OBJECT;
    }

    public SerializableAttribute(String key, Serializable value) {
        this.key = key;
        this.value = value;
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
