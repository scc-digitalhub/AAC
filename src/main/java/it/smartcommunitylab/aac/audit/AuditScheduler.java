/*
 * Copyright 2024 the original author or authors
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

import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import java.time.Instant;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditScheduler implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DELAY = 24 * 60 * 60 * 1000; //every day
    public static final int INITIAL_DELAY = 5 * 60 * 1000; //wait 5m for start

    @Value("${audit.retention}")
    private Long auditRetentionInterval;

    @Autowired
    private AuditEventStore eventStore;

    @Scheduled(fixedDelay = DEFAULT_DELAY, initialDelay = INITIAL_DELAY)
    @Transactional
    public void deleteExpiredEvents() {
        //delete only if interval is > 1day
        if (auditRetentionInterval != null && auditRetentionInterval.longValue() * 1000 > DEFAULT_DELAY) {
            try {
                Instant before = Instant.now().minusSeconds(auditRetentionInterval);
                logger.info("remove audit events expired before {}", before);
                eventStore.cleanupAuditEvents(before);
            } catch (RuntimeException e) {
                logger.error("error cleaning audit events", e);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (auditRetentionInterval != null && auditRetentionInterval.longValue() * 1000 < DEFAULT_DELAY) {
            logger.warn("audit retention interval must be greater than 1 day, disabled cleanup");
            auditRetentionInterval = null;
        }
    }
}
