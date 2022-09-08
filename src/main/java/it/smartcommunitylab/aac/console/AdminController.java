package it.smartcommunitylab.aac.console;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.model.Realm;

@RestController
@Hidden
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')")
public class AdminController {

    @Autowired
    private RealmManager realmManager;

    @GetMapping("/console/admin/realms")
    public Page<Realm> getRealms(@RequestParam(required = false) String q, Pageable pageRequest) {
        return realmManager.searchRealms(q, pageRequest);
    }

    @GetMapping("/console/admin/realms/{slug}")
    public Realm getRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
            throws NoSuchRealmException {
        return realmManager.getRealm(slug);
    }

    @PostMapping("/console/admin/realms")
    public Realm addRealm(@RequestBody @Valid @NotNull Realm realm) {
        return realmManager.addRealm(realm);
    }

    @PutMapping("/console/admin/realms/{slug}")
    public Realm updateRealm(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestBody @Valid @NotNull Realm realm)
            throws NoSuchRealmException {
        return realmManager.updateRealm(slug, realm);
    }

    @DeleteMapping("/console/admin/realms/{slug}")
    public void deleteRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
            throws NoSuchRealmException {
        realmManager.deleteRealm(slug, true);
    }
}
