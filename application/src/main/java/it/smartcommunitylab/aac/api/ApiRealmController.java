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

package it.smartcommunitylab.aac.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AdminRealmsScope;
import it.smartcommunitylab.aac.api.scopes.ApiRealmScope;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.RealmManager;
import java.util.Collection;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiSecurityTag(AdminRealmsScope.SCOPE)
@Tag(name = "Realms", description = "Manage realms and their configuration")
@ApiRequestMapping
public class ApiRealmController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RealmManager realmManager;

    @GetMapping("/realms")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')  and hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE + "')")
    @Operation(summary = "list realm with optional keywords")
    public Collection<Realm> getRealms(@RequestParam(required = false) Optional<String> q) {
        if (q.isPresent()) {
            String query = StringUtils.trimAllWhitespace(q.get());
            logger.debug("search realms for query {}", String.valueOf(query));

            return realmManager.searchRealms(q.get());
        } else {
            logger.debug("list realms");

            return realmManager.listRealms();
        }
    }

    @PostMapping("/realms")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')  and hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE + "')")
    @Operation(summary = "add a new realm")
    public Realm addRealm(@RequestBody @NotNull @Valid Realm r) throws RegistrationException {
        logger.debug("add realm");

        if (logger.isTraceEnabled()) {
            logger.trace("realm bean: {}", StringUtils.trimAllWhitespace(r.toString()));
        }
        return realmManager.addRealm(r);
    }

    @GetMapping("/realms/{slug}")
    @PreAuthorize(
        "(hasAuthority('" +
        Config.R_ADMIN +
        "') or hasAuthority(#realm+':ROLE_ADMIN')) and (hasAuthority('SCOPE_" +
        AdminRealmsScope.SCOPE +
        "') or hasAuthority('SCOPE_" +
        ApiRealmScope.SCOPE +
        "'))"
    )
    @Operation(summary = "get a given realm")
    public Realm getRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
        throws NoSuchRealmException {
        logger.debug("get realm {}", StringUtils.trimAllWhitespace(slug));

        return realmManager.getRealm(slug);
    }

    @PutMapping("/realms/{slug}")
    @PreAuthorize(
        "(hasAuthority('" +
        Config.R_ADMIN +
        "') or hasAuthority(#realm+':ROLE_ADMIN')) and (hasAuthority('SCOPE_" +
        AdminRealmsScope.SCOPE +
        "') or hasAuthority('SCOPE_" +
        ApiRealmScope.SCOPE +
        "'))"
    )
    @Operation(summary = "update a given realm")
    public Realm updateRealm(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
        @RequestBody @Valid @NotNull Realm r
    ) throws NoSuchRealmException, RegistrationException {
        logger.debug("update realm {}", StringUtils.trimAllWhitespace(slug));

        if (logger.isTraceEnabled()) {
            logger.trace("realm bean: {}", StringUtils.trimAllWhitespace(r.toString()));
        }
        return realmManager.updateRealm(slug, r);
    }

    @DeleteMapping("/realms/{slug}")
    @PreAuthorize(
        "(hasAuthority('" +
        Config.R_ADMIN +
        "') or hasAuthority(#realm+':ROLE_ADMIN')) and (hasAuthority('SCOPE_" +
        AdminRealmsScope.SCOPE +
        "') or hasAuthority('SCOPE_" +
        ApiRealmScope.SCOPE +
        "'))"
    )
    @Operation(summary = "delete a given realm")
    public void deleteRealm(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
        @RequestParam(required = false, defaultValue = "false") boolean cleanup
    ) throws NoSuchRealmException {
        logger.debug("delete realm {}", StringUtils.trimAllWhitespace(slug));

        realmManager.deleteRealm(slug, cleanup);
    }
}
