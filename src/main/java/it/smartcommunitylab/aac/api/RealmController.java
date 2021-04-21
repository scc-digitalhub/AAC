package it.smartcommunitylab.aac.api;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public Page<Realm> getRealms(@RequestParam(required=false) String q, Pageable pageRequest) {
        return realmManager.searchRealms(q, pageRequest);
    }

    @GetMapping("{slug}")
    public Realm getRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug) throws NoSuchRealmException {
        return realmManager.getRealm(slug);
    }

    @PostMapping
    public Realm addRealm(
            @RequestBody @Valid Realm r) {
        return realmManager.addRealm(r);
    }

    @PutMapping("{slug}")
    public Realm updateRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestBody @Valid Realm r) throws NoSuchRealmException {
        return realmManager.updateRealm(slug, r);
    }

    @DeleteMapping("{slug}")
    public void deleteRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
            @RequestParam(required = false, defaultValue = "false") boolean cleanup) throws NoSuchRealmException {
        realmManager.deleteRealm(slug, cleanup);
    }

}
