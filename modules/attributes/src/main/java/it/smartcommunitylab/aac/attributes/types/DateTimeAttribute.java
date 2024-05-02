/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.attributes.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttribute;
import it.smartcommunitylab.aac.attributes.model.AttributeType;
import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;

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

    @Override
    public String exportValue() {
        return value == null ? null : getIsoDateTimeValue();
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
            } catch (DateTimeParseException e) {}
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
