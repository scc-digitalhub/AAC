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

package it.smartcommunitylab.aac.audit.store;

import java.time.Instant;
import java.util.List;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.data.domain.Pageable;

public interface AuditEventStore extends AuditEventRepository {
    public long countByRealm(String realm, Instant after, Instant before, String type);

    public long countByTx(String tx, String type);

    public long countByPrincipal(String principal, Instant after, Instant before, String type);

    public List<AuditEvent> findByRealm(String realm, Instant after, Instant before, String type);

    public List<AuditEvent> searchByRealm(String realm, Instant after, Instant before, String type, Pageable pageable);

    public List<AuditEvent> findByTx(String realm, String type);

    public List<AuditEvent> findByPrincipal(String principal, Instant after, Instant before, String type);
}
