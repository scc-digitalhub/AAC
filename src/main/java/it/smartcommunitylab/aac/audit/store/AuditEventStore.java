package it.smartcommunitylab.aac.audit.store;

import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;

public interface AuditEventStore extends AuditEventRepository {
    public long countByRealm(String realm, Instant after, Instant before, String type);

    public long countByPrincipal(String principal, Instant after, Instant before, String type);

    public List<RealmAuditEvent> findByRealm(String realm, Instant after, Instant before, String type);

    public List<AuditEvent> findByPrincipal(String principal, Instant after, Instant before, String type);
}
