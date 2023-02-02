package it.smartcommunitylab.aac.audit;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

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
        return "RealmAuditEvent [realm=" + realm + ", timestamp=" + getTimestamp() + ", principal="
                + getPrincipal() + ", type=" + getType() + ", data=" + getData() + "]";
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
        return getTimestamp() != null ? getTimestamp().getEpochSecond() : -1;
    }

}
