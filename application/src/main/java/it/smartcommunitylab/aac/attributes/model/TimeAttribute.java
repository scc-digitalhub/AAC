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

package it.smartcommunitylab.aac.attributes.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttribute;
import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;

public class TimeAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    @JsonIgnore
    private LocalTime value;

    public TimeAttribute(String key) {
        this.key = key;
    }

    public TimeAttribute(String key, LocalTime time) {
        this.key = key;
        this.value = time;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.TIME;
    }

    @Override
    public LocalTime getValue() {
        return value;
    }

    @Override
    public String exportValue() {
        return value == null ? null : getIsoDateTimeValue();
    }

    public void setValue(LocalTime value) {
        this.value = value;
    }

    @JsonGetter("value")
    public String getIsoDateTimeValue() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
        return formatter.format(value);
    }

    public static LocalTime parseValue(Serializable value) throws ParseException {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        }

        String stringValue = String.valueOf(value);

        Collection<DateTimeFormatter> formatters = getFormatters();
        for (final DateTimeFormatter df : formatters) {
            try {
                LocalTime time = LocalTime.parse(stringValue, df);
                if (time != null) {
                    return time;
                }
            } catch (DateTimeParseException e) {}
        }

        throw new ParseException("Unable to parse the date", 0);
    }

    public static LocalTime parseIsoValue(String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            return LocalTime.parse(value, formatter);
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
