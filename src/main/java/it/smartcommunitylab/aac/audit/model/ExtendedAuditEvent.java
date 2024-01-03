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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.audit.AuditEvent;

public abstract class ExtendedAuditEvent extends AuditEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected ExtendedAuditEvent(Instant timestamp, String principal, String type, Map<String, Object> data) {
        super(timestamp, principal, type, data);
    }

    protected Collection<String> getKeys() {
        return Collections.emptyList();
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

    public Map<String, Object> getAttributes() {
        return super.getData() == null
            ? null
            : super
                .getData()
                .entrySet()
                .stream()
                .filter(e -> !getKeys().contains(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    protected String getAsString(String key) {
        String value = null;

        if (getData() != null && getData().containsKey(key)) {
            //try cast and handle error
            try {
                String c = (String) getData().get(key);
                value = c;
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("invalid value for " + key);
            }
        }

        return value;
    }
}
