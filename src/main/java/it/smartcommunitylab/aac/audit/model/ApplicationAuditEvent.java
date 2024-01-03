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

package it.smartcommunitylab.aac.audit.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ApplicationAuditEvent<E extends ApplicationEvent> extends ExtendedAuditEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    private static final String EVENT_KEY = "event";
    private static final String EVENT_CLASS = "clazz";

    private static final String ID_KEY = "id";
    public static final String[] KEYS = { ID_KEY, EVENT_KEY, EVENT_CLASS };

    public ApplicationAuditEvent(
        @Nullable String id,
        Instant timestamp,
        String principal,
        String type,
        E event,
        Map<String, Object> attributes
    ) {
        super(timestamp, principal, type, buildData(id, event, attributes));
    }

    public ApplicationAuditEvent(Instant timestamp, String principal, String type, Map<String, Object> data) {
        super(timestamp, principal, type, data);
    }

    static Map<String, Object> buildData(@Nullable String id, ApplicationEvent event, Map<String, Object> attributes) {
        Assert.notNull(event, "event can not be null");

        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }

        return buildData(attributes, KEYS, new Object[] { id, event, event.getClass().getName() });
    }

    public String getId() {
        return getAsString(ID_KEY);
    }

    public E getEvent() {
        E event = null;

        if (getData() != null && getData().containsKey(EVENT_KEY)) {
            //try cast and handle error
            try {
                @SuppressWarnings("unchecked")
                E e = (E) getData().get(EVENT_KEY);
                event = e;
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("invalid event");
            }
        }

        return event;
    }

    public String getClazz() {
        return getAsString(EVENT_CLASS);
    }

    public long getTime() {
        return getTimestamp() != null ? getTimestamp().getEpochSecond() * 1000 : -1;
    }

    @Override
    protected Collection<String> getKeys() {
        return Arrays.asList(KEYS);
    }

    @Override
    public String toString() {
        return (
            "ApplicationAuditEvent [id=" +
            String.valueOf(getId()) +
            ", type=" +
            getType() +
            ", timestamp=" +
            getTimestamp() +
            ", principal=" +
            getPrincipal() +
            "]"
        );
    }

    public static <T extends ApplicationEvent> ApplicationAuditEvent<T> from(AuditEvent audit, Class<?> clazz) {
        ApplicationAuditEvent<T> ea = new ApplicationAuditEvent<>(
            audit.getTimestamp(),
            audit.getPrincipal(),
            audit.getType(),
            audit.getData()
        );

        try {
            if (ea.getClazz() != null && clazz.isAssignableFrom(Class.forName(ea.getClazz()))) {
                return ea;
            }
        } catch (ClassNotFoundException e) {}

        throw new IllegalArgumentException("invalid or missing event");
    }
}
