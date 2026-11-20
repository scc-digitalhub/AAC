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

package it.smartcommunitylab.aac.audit.listeners;

import it.smartcommunitylab.aac.events.UserDeleteEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

/*
 * Audit listener for user lifecycle events: translates the event into an
 * audit event, with the deleted user as principal.
 */
public class UserEventListener implements ApplicationListener<UserDeleteEvent>, ApplicationEventPublisherAware {

    public static final String USER_DELETE = "USER_DELETE";

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onApplicationEvent(UserDeleteEvent event) {
        String principal = event.getUserId();

        // null values are accepted here and skipped by the store
        Map<String, Object> data = new HashMap<>();
        data.put("realm", event.getRealm());
        data.put("actor", event.getActor());

        // build audit
        AuditEvent audit = new AuditEvent(Instant.ofEpochMilli(event.getTimestamp()), principal, USER_DELETE, data);

        // publish as event, listener will persist to store
        if (publisher != null) {
            publisher.publishEvent(new AuditApplicationEvent(audit));
        }
    }
}
