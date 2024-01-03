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

package it.smartcommunitylab.aac.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.AuditManager;
import it.smartcommunitylab.aac.audit.model.RealmAuditEvent;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
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
    @Operation(summary = "find audit events from a given realm")
    public Collection<RealmAuditEvent> findEvents(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false, name = "type") Optional<String> type,
        @RequestParam(required = false, name = "after") @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) Optional<Date> after,
        @RequestParam(required = false, name = "before") @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) Optional<Date> before
    ) throws NoSuchRealmException {
        logger.debug("find audit events for realm [}", StringUtils.trimAllWhitespace(realm));

        return auditManager.findRealmEvents(realm, type.orElse(null), after.orElse(null), before.orElse(null));
    }
}
