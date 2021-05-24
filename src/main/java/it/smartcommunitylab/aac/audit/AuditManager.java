package it.smartcommunitylab.aac.audit;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.audit.store.AuditEventStore;

@Service
public class AuditManager {

    @Autowired
    private AuditEventStore auditStore;

    public long countRealmEvents(String realm, String type, Date after, Date before) {
        Instant a = after == null ? null : after.toInstant();
        Instant b = before == null ? null : before.toInstant();

        return auditStore.countByRealm(realm, a, b, type);
    }

    public long countPrincipalEvents(String principal, String type, Date after, Date before) {
        Instant a = after == null ? null : after.toInstant();
        Instant b = before == null ? null : before.toInstant();
        return auditStore.countByPrincipal(principal, a, b, type);
    }

    public List<RealmAuditEvent> findRealmEvents(String realm, String type, Date after, Date before) {
        Instant a = after == null ? null : after.toInstant();
        Instant b = before == null ? null : before.toInstant();

        return auditStore.findByRealm(realm, a, b, type);
    }

    public List<AuditEvent> findPrincipalEvents(String principal, String type, Date after, Date before) {
        Instant a = after == null ? null : after.toInstant();
        Instant b = before == null ? null : before.toInstant();
        return auditStore.findByPrincipal(principal, a, b, type);
    }
}
