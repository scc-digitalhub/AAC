package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.AttributeType;

public class DateTimeAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    @JsonIgnore
    private LocalDateTime value;

    public DateTimeAttribute(String key) {
        this.key = key;
    }

    public DateTimeAttribute(String key, LocalDateTime date) {
        this.key = key;
        this.value = date;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.DATETIME;
    }

    @Override
    public LocalDateTime getValue() {
        return value;
    }

    public void setValue(LocalDateTime value) {
        this.value = value;
    }

    @JsonGetter("value")
    public String getIsoDateTimeValue() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return formatter.format(value);
    }

    public static LocalDateTime parseValue(Serializable value) throws ParseException {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }

        String stringValue = String.valueOf(value);

        Collection<DateTimeFormatter> formatters = getFormatters();
        for (final DateTimeFormatter df : formatters) {
            try {
                LocalDateTime date = LocalDateTime.parse(stringValue, df);
                if (date != null) {
                    return date;
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
        formatters.add(DateTimeFormatter.ISO_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_INSTANT);

        formatters.add(DateTimeFormatter.RFC_1123_DATE_TIME);

        // fallback to date only
        formatters.add(DateTimeFormatter.ISO_DATE);
        formatters.add(DateTimeFormatter.BASIC_ISO_DATE);
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE);
        formatters.add(DateTimeFormatter.ofPattern("dd-MM-YYYY"));
        return formatters;
    }

}
