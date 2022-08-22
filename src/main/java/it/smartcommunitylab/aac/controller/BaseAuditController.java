package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.AuditManager;
import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import it.smartcommunitylab.aac.common.NoSuchRealmException;

/*
 * Base controller for audit
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseAuditController implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected AuditManager auditManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(auditManager, "audit manager is required");
    }

    @Autowired
    public void setAuditManager(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    @GetMapping("/audit/{realm}")
    public Collection<RealmAuditEvent> findEvents(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, name = "type") Optional<String> type,
            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
            throws NoSuchRealmException {
        logger.debug("find audit events for realm [}",
                StringUtils.trimAllWhitespace(realm));

        return auditManager.findRealmEvents(realm, type.orElse(null), after.orElse(null), before.orElse(null));

    }

}
