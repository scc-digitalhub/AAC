package it.smartcommunitylab.aac.api;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.AuditManager;
import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import it.smartcommunitylab.aac.common.NoSuchRealmException;

@RestController
@RequestMapping("api")
public class AuditController {
    @Autowired
    private AuditManager auditManager;

    @GetMapping("/audit/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<RealmAuditEvent> findEvents(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, name = "type") Optional<String> type,
            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
            throws NoSuchRealmException {

        return auditManager.listRealmEvents(realm, type.orElse(null), after.orElse(null), before.orElse(null));

    }

}
