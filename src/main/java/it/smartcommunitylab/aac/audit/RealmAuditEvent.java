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

package it.smartcommunitylab.aac.audit;

import it.smartcommunitylab.aac.SystemKeys;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.util.Assert;

public class RealmAuditEvent extends AuditEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String realm;

    public RealmAuditEvent(String realm, Instant timestamp, String principal, String type, Map<String, Object> data) {
        super(timestamp, principal, type, data);
        Assert.notNull(realm, "realm can not be null");
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return (
            "RealmAuditEvent [realm=" +
            realm +
            ", timestamp=" +
            getTimestamp() +
            ", principal=" +
            getPrincipal() +
            ", type=" +
            getType() +
            ", data=" +
            getData() +
            "]"
        );
    }

    public String getId() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTimestamp());
        if (getPrincipal() != null) {
            sb.append("-").append(getPrincipal());
        }
        return sb.toString();
    }

    public long getTime() {
        return getTimestamp() != null ? getTimestamp().getEpochSecond() * 1000 : -1;
    }
}
