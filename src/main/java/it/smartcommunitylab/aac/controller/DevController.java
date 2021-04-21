package it.smartcommunitylab.aac.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.model.Realm;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
public class DevController {

	@Autowired
	private RealmManager realmManager;
	@Autowired
	private UserManager userManager;
	
	@GetMapping("/console/dev/realms")
	public ResponseEntity<List<Realm>> myRealms() throws NoSuchRealmException {
        // TODO: complete reading of user realms
        UserDetails user = userManager.curUserDetails();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (SystemKeys.REALM_GLOBAL.equals(user.getRealm())) {
    		return ResponseEntity.ok(Collections.singletonList(new Realm(SystemKeys.REALM_GLOBAL, "GLOBAL")));
        }
		return ResponseEntity.ok(Collections.singletonList(realmManager.getRealm(user.getRealm())));
        
	}
	@GetMapping("/console/dev/realms/{slug:.*}")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority('{slug}:REALM_ADMIN') or hasAuthority('{slug}:REALM_DEVELOPER')")
	public ResponseEntity<Realm> getRealm(@PathVariable String slug) throws NoSuchRealmException {
		return ResponseEntity.ok(realmManager.getRealm(slug));
	}
}
