package it.smartcommunitylab.aac.api;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.model.Realm;

@RestController
@RequestMapping("api/realm")
public class RealmController {

    @Autowired
    private RealmManager realmManager;

    @GetMapping("")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')")
    public Collection<Realm> getRealms(@RequestParam(required = false) String q) {
        return realmManager.searchRealms(q);
    }

    @GetMapping("{slug}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Realm getRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug) throws NoSuchRealmException {
        return realmManager.getRealm(slug);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')")
    public Realm addRealm(
            @RequestBody @Valid Realm r) {
        return realmManager.addRealm(r);
    }

    @PutMapping("{slug}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Realm updateRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestBody @Valid Realm r) throws NoSuchRealmException {
        return realmManager.updateRealm(slug, r);
    }

    @DeleteMapping("{slug}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void deleteRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestParam(required = false, defaultValue = "false") boolean cleanup) throws NoSuchRealmException {
        realmManager.deleteRealm(slug, cleanup);
    }

}
