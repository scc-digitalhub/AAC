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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.ApplicationEvent;

public class ExtendedAuditEvent<E extends ApplicationEvent>
    extends AuditEvent
    implements ApplicationAuditEvent<E>, TxAuditEvent, RealmAuditEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected ExtendedAuditEvent(Instant timestamp, String principal, String type, Map<String, Object> data) {
        super(timestamp, principal, type, data);
    }

    static Map<String, Object> buildData(Map<String, Object> initialData, String[] keys, Object[] values) {
        Map<String, Object> data = new HashMap<>();
        if (initialData != null) {
            data.putAll(initialData);
        }

        if (keys != null && values != null) {
            if (keys.length != values.length) {
                throw new IllegalArgumentException("invalid number of parameters");
            }

            for (int i = 0; i < keys.length; i++) {
                data.put(keys[i], values[i]);
            }
        }

        return data;
    }

    @Override
    public Map<String, Object> getClaims() {
        return getData();
    }

    public String getId() {
        String id = getClaimAsString("id");
        return id != null ? id : getTimestamp().getEpochSecond() + "-" + getPrincipal();
    }

    public Date getTime() {
        if (getTimestamp() == null) {
            return null;
        }

        return Date.from(getTimestamp());
    }

    public static <E extends ApplicationEvent> ExtendedAuditEvent<E> from(AuditEvent event) {
        return new ExtendedAuditEvent<>(event.getTimestamp(), event.getPrincipal(), event.getType(), event.getData());
    }
}
