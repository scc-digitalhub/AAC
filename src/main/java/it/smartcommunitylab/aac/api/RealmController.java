package it.smartcommunitylab.aac.api;

import java.util.Collection;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AdminRealmsScope;
import it.smartcommunitylab.aac.api.scopes.ApiRealmScope;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.model.Realm;

@RestController
@RequestMapping("api/realm")
public class RealmController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RealmManager realmManager;

    @GetMapping("")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')  and hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE + "')")
    public Collection<Realm> getRealms(@RequestParam(required = false) Optional<String> q) {
        if (q.isPresent()) {
            String query = StringUtils.trimAllWhitespace(q.get());
            logger.debug("search realms for query {}",
                    String.valueOf(query));

            return realmManager.searchRealms(q.get());
        } else {
            logger.debug("list realms");

            return realmManager.listRealms();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')  and hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE + "')")
    public Realm addRealm(
            @RequestBody @NotNull @Valid Realm r) {
        logger.debug("add realm");

        if (logger.isTraceEnabled()) {
            logger.trace("realm bean: " + StringUtils.trimAllWhitespace(r.toString()));
        }
        return realmManager.addRealm(r);
    }

    @GetMapping("{slug}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and (hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE
            + "') or hasAuthority('SCOPE_" + ApiRealmScope.SCOPE + "'))")
    public Realm getRealm(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
            throws NoSuchRealmException {
        logger.debug("get realm {}",
                StringUtils.trimAllWhitespace(slug));

        return realmManager.getRealm(slug);
    }

    @PutMapping("{slug}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and (hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE
            + "') or hasAuthority('SCOPE_" + ApiRealmScope.SCOPE + "'))")
    public Realm updateRealm(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestBody @Valid @NotNull Realm r) throws NoSuchRealmException {
        logger.debug("update realm {}",
                StringUtils.trimAllWhitespace(slug));

        if (logger.isTraceEnabled()) {
            logger.trace("realm bean: " + StringUtils.trimAllWhitespace(r.toString()));
        }
        return realmManager.updateRealm(slug, r);
    }

    @DeleteMapping("{slug}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and (hasAuthority('SCOPE_" + AdminRealmsScope.SCOPE
            + "') or hasAuthority('SCOPE_" + ApiRealmScope.SCOPE + "'))")
    public void deleteRealm(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestParam(required = false, defaultValue = "false") boolean cleanup) throws NoSuchRealmException {
        logger.debug("delete realm {}",
                StringUtils.trimAllWhitespace(slug));

        realmManager.deleteRealm(slug, cleanup);
    }

}
