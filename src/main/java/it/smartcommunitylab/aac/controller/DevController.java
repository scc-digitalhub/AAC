package it.smartcommunitylab.aac.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
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
	
	@GetMapping("/console/dev/realms/{slug:.*}/users")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority('{slug}:REALM_ADMIN') or hasAuthority('{slug}:REALM_DEVELOPER')")
	public ResponseEntity<Page<User>> getRealmUsers(@PathVariable String slug, @RequestParam(required=false) String q, Pageable pageRequest) throws NoSuchRealmException {
		return ResponseEntity.ok(userManager.searchUsers(slug, q, pageRequest));
	}
	
	@DeleteMapping("/console/dev/realms/{slug}/users/{subjectId:.*}")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority('{slug}:REALM_ADMIN') or hasAuthority('{slug}:REALM_DEVELOPER')")
	public ResponseEntity<Void> deleteRealmUser(@PathVariable String slug, @PathVariable String subjectId) throws NoSuchRealmException, NoSuchUserException {
		User curUser = userManager.curUser(slug);
		if (curUser.getSubjectId().equals(subjectId)) {
			throw new IllegalArgumentException("Cannot delete current user");
		}
		userManager.removeUser(slug, subjectId);
		return ResponseEntity.ok(null);
	}
	
	@PutMapping("/console/dev/realms/{slug}/users/{subjectId:.*}/roles")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority('{slug}:REALM_ADMIN') or hasAuthority('{slug}:REALM_DEVELOPER')")
	public ResponseEntity<User> updateRealmRoles(@PathVariable String slug, @PathVariable String subjectId, @RequestBody RolesBean bean) throws NoSuchRealmException, NoSuchUserException {
		userManager.updateRealmAuthorities(slug, subjectId, bean.getRoles());
		return ResponseEntity.ok(userManager.getUser(slug, subjectId));
	}

	public static class RolesBean {
		
		private List<String> roles;
		public List<String> getRoles() {
			return roles;
		}
		public void setRoles(List<String> roles) {
			this.roles = roles;
		}
		
	}
	
}
