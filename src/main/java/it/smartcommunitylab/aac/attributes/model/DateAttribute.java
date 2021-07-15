package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import it.smartcommunitylab.aac.model.AttributeType;

public class DateAttribute extends AbstractAttribute {

    private LocalDate value;

    public DateAttribute(String key) {
        this.key = key;
    }

    public DateAttribute(String key, LocalDate date) {
        this.key = key;
        this.value = date;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.DATE;
    }

    @Override
    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate value) {
        this.value = value;
    }

    public static LocalDate parseValue(Serializable value) throws ParseException {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }

        String stringValue = String.valueOf(value);

        Collection<DateTimeFormatter> formatters = getFormatters();
        for (final DateTimeFormatter df : formatters) {
            try {
                LocalDate date = LocalDate.parse(stringValue, df);
                if (date != null) {
                    return date;
                }
            } catch (DateTimeParseException e) {
            }
        }

        throw new ParseException("Unable to parse the date", 0);
    }

    public static LocalDate parseIsoValue(String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDate parseInstantValue(String value) {
        try {
            return LocalDate.from(Instant.parse(value));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Collection<DateTimeFormatter> getFormatters() {
        Collection<DateTimeFormatter> formatters = new ArrayList<>();
        formatters.add(DateTimeFormatter.ISO_DATE);
        formatters.add(DateTimeFormatter.BASIC_ISO_DATE);
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE);
        formatters.add(DateTimeFormatter.ofPattern("dd-MM-YYYY"));
        formatters.add(DateTimeFormatter.ISO_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_INSTANT);

        formatters.add(DateTimeFormatter.RFC_1123_DATE_TIME);
        return formatters;
    }

}
