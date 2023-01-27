package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.AttributeType;

public class InstantAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    @JsonIgnore
    private Instant value;

    public InstantAttribute(String key) {
        this.key = key;
    }

    public InstantAttribute(String key, Instant date) {
        this.key = key;
        this.value = date;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.INSTANT;
    }

    @Override
    public Instant getValue() {
        return value;
    }

    @Override
    public String exportValue() {
        return value == null ? null : getIsoDateTimeValue();
    }

    public void setValue(Instant value) {
        this.value = value;
    }

    @JsonGetter("value")
    public String getIsoDateTimeValue() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return formatter.format(value);
    }

    public static Instant parseValue(Serializable value) throws ParseException {
        if (value instanceof Instant) {
            return (Instant) value;
        }

        String stringValue = String.valueOf(value);

        Collection<DateTimeFormatter> formatters = getFormatters();
        for (final DateTimeFormatter df : formatters) {
            try {
                ZonedDateTime date = ZonedDateTime.parse(stringValue, df);
                if (date != null) {
                    return date.toInstant();
                }
            } catch (DateTimeParseException e) {
            }
        }

        throw new ParseException("Unable to parse the date", 0);
    }

    public static LocalDateTime parseIsoValue(String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDateTime parseInstantValue(String value) {
        try {
            return LocalDateTime.from(Instant.parse(value));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Collection<DateTimeFormatter> getFormatters() {
        Collection<DateTimeFormatter> formatters = new ArrayList<>();
        formatters.add(DateTimeFormatter.ISO_INSTANT);
        formatters.add(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        formatters.add(DateTimeFormatter.RFC_1123_DATE_TIME);

        // fallback to date only
        formatters.add(DateTimeFormatter.ISO_DATE);
        formatters.add(DateTimeFormatter.BASIC_ISO_DATE);
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE);
        formatters.add(DateTimeFormatter.ofPattern("dd-MM-YYYY"));
        return formatters;
    }

}
