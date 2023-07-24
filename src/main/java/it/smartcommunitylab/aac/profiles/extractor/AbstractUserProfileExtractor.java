package it.smartcommunitylab.aac.profiles.extractor;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.DateTimeAttribute;
import it.smartcommunitylab.aac.attributes.model.NumberAttribute;
import it.smartcommunitylab.aac.attributes.model.TimeAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.model.AttributeType;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractUserProfileExtractor implements UserProfileExtractor {

    // lookup an attribute in multiple sets, return first match
    protected Attribute getAttribute(Collection<UserAttributes> attributes, String key, String... identifier) {
        return getAttribute(attributes, key, Arrays.asList(identifier));
    }

    protected Attribute getAttribute(Collection<UserAttributes> attributes, String key, Collection<String> identifier) {
        Set<UserAttributes> sets = attributes
            .stream()
            .filter(a -> identifier.contains(a.getIdentifier()))
            .collect(Collectors.toSet());

        for (UserAttributes uattr : sets) {
            Optional<Attribute> attr = uattr.getAttributes().stream().filter(a -> a.getKey().equals(key)).findFirst();
            if (attr.isPresent()) {
                return attr.get();
            }
        }

        return null;
    }

    protected String getStringAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.STRING == attr.getType()) {
            return (String) attr.getValue();
        }

        return String.valueOf(attr.getValue());
    }

    protected Boolean getBooleanAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.BOOLEAN == attr.getType()) {
            return (Boolean) attr.getValue();
        }

        try {
            return BooleanAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected Number getNumberAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.NUMBER == attr.getType()) {
            return (Number) attr.getValue();
        }

        try {
            return NumberAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected LocalDate getDateAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.DATE == attr.getType()) {
            return (LocalDate) attr.getValue();
        }

        try {
            return DateAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected LocalDateTime getDateTimeAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.DATETIME == attr.getType()) {
            return (LocalDateTime) attr.getValue();
        }

        try {
            return DateTimeAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected LocalTime getTimeAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.TIME == attr.getType()) {
            return (LocalTime) attr.getValue();
        }

        try {
            return TimeAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }
}
