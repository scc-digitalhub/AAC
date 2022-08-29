package it.smartcommunitylab.aac.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.model.Realm;

@RestController
@Hidden
public class AdminController {

    @Autowired
    private RealmManager realmManager;

    @GetMapping("/console/admin/realms")
    @PreAuthorize("hasAuthority(\"" + Config.R_ADMIN + "\")")
    public ResponseEntity<Page<Realm>> getRealms(@RequestParam(required = false) String q, Pageable pageRequest) {
        return ResponseEntity.ok(realmManager.searchRealms(q, pageRequest));
    }

    @GetMapping("/console/admin/realms/{slug:.*}")
    @PreAuthorize("hasAuthority(\"" + Config.R_ADMIN + "\")")
    public ResponseEntity<Realm> getRealm(@PathVariable String slug) throws NoSuchRealmException {
        return ResponseEntity.ok(realmManager.getRealm(slug));
    }

    @PostMapping("/console/admin/realms")
    @PreAuthorize("hasAuthority(\"" + Config.R_ADMIN + "\")")
    public ResponseEntity<Realm> addRealm(@RequestBody Realm realm) {
        return ResponseEntity.ok(realmManager.addRealm(realm));
    }

    @PutMapping("/console/admin/realms/{slug:.*}")
    @PreAuthorize("hasAuthority(\"" + Config.R_ADMIN + "\")")
    public ResponseEntity<Realm> updateRealm(@PathVariable String slug, @RequestBody Realm realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(realmManager.updateRealm(slug, realm));
    }

    @DeleteMapping("/console/admin/realms/{slug:.*}")
    @PreAuthorize("hasAuthority(\"" + Config.R_ADMIN + "\")")
    public ResponseEntity<Void> deleteRealm(@PathVariable String slug) throws NoSuchRealmException {
        realmManager.deleteRealm(slug, true);
        return ResponseEntity.ok(null);
    }
}
