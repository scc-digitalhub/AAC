package it.smartcommunitylab.aac.attributes.mapper;

import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.DateTimeAttribute;
import it.smartcommunitylab.aac.attributes.model.NumberAttribute;
import it.smartcommunitylab.aac.attributes.model.SerializableAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.model.TimeAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.model.AttributeType;

public class ExactAttributesMapper extends BaseAttributesMapper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ExactAttributesMapper(AttributeSet attributeSet) {
        super(attributeSet);

    }

    @Override
    protected Attribute getAttribute(Attribute attribute, Map<String, Serializable> attributes) {
        // search in map for keys exact matching
        String key = attribute.getKey();
        AttributeType type = attribute.getType();

        Serializable value = null;

        // exact match first
        if (attributes.containsKey(key)) {
            value = attributes.get(key);
        }

        if (value != null) {
            try {
                if (type == AttributeType.BOOLEAN) {
                    Boolean b = BooleanAttribute.parseValue(value);
                    return new BooleanAttribute(key, b);
                }
                if (type == AttributeType.DATE) {
                    LocalDate d = DateAttribute.parseValue(value);
                    return new DateAttribute(key, d);
                }
                if (type == AttributeType.DATETIME) {
                    LocalDateTime dt = DateTimeAttribute.parseValue(value);
                    return new DateTimeAttribute(key, dt);
                }
                if (type == AttributeType.NUMBER) {
                    Number n = NumberAttribute.parseValue(value);
                    return new NumberAttribute(key, n);
                }
                if (type == AttributeType.STRING) {
                    String s = StringAttribute.parseValue(value);
                    return new StringAttribute(key, s);
                }
                if (type == AttributeType.TIME) {
                    LocalTime t = TimeAttribute.parseValue(value);
                    return new TimeAttribute(key, t);
                }
                if (type == AttributeType.OBJECT) {
                    return new SerializableAttribute(key, value);
                }
            } catch (ParseException e) {
                logger.debug("parse error for field " + key + " " + e.getMessage());
            }
        }

        return null;
    }

}
