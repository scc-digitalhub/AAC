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

import java.util.Map;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.oauth2.core.ClaimAccessor;

public interface ApplicationAuditEvent<E extends ApplicationEvent> extends ClaimAccessor {
    public static final String EVENT_KEY = "event";

    default E getEvent() {
        E event = null;

        if (getClaims() != null && getClaims().containsKey(EVENT_KEY)) {
            //try cast and handle error
            try {
                @SuppressWarnings("unchecked")
                E e = (E) getClaims().get(EVENT_KEY);
                event = e;
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("invalid event");
            }
        }

        return event;
    }

    default String getClazz() {
        Map<String, Object> map = this.getClaimAsMap(EVENT_KEY);
        if (map == null || !map.containsKey("@class")) {
            return null;
        }

        //try cast and handle error
        String value = null;
        try {
            String c = (String) map.get("@class");
            value = c;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("invalid value for clazz");
        }

        return value;
    }
    // public static <T extends ApplicationEvent> ApplicationAuditEvent<T> from(AuditEvent audit, Class<?> clazz) {
    //     ApplicationAuditEvent<T> ea = new ApplicationAuditEvent<>(
    //         audit.getTimestamp(),
    //         audit.getPrincipal(),
    //         audit.getType(),
    //         audit.getData()
    //     );

    //     try {
    //         if (ea.getClazz() != null && clazz.isAssignableFrom(Class.forName(ea.getClazz()))) {
    //             return ea;
    //         }
    //     } catch (ClassNotFoundException e) {}

    //     throw new IllegalArgumentException("invalid or missing event");
    // }
}
