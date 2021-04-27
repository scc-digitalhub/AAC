package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.dto.ProviderRegistrationBean;
import it.smartcommunitylab.aac.model.ClientApp;
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
	@Autowired
	private ProviderManager providerManager;
	@Autowired
	private ClientManager clientManager;
	
    @RequestMapping("/dev")
    public ModelAndView developer() {
        UserDetails user = userManager.curUserDetails();
        if (user == null || !user.isRealmDeveloper()) {
            throw new SecurityException();
        }
        return new ModelAndView("index");
    }
	
	
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
	@GetMapping("/console/dev/realms/{realm:.*}")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
	public ResponseEntity<Realm> getRealm(@PathVariable String realm) throws NoSuchRealmException {
		return ResponseEntity.ok(realmManager.getRealm(realm));
	}
	
	@GetMapping("/console/dev/realms/{realm:.*}/users")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<Page<User>> getRealmUsers(@PathVariable String realm, @RequestParam(required=false) String q, Pageable pageRequest) throws NoSuchRealmException {
		return ResponseEntity.ok(userManager.searchUsers(realm, q, pageRequest));
	}
	
	@DeleteMapping("/console/dev/realms/{realm}/users/{subjectId:.*}")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<Void> deleteRealmUser(@PathVariable String realm, @PathVariable String subjectId) throws NoSuchRealmException, NoSuchUserException {
		User curUser = userManager.curUser(realm);
		if (curUser.getSubjectId().equals(subjectId)) {
			throw new IllegalArgumentException("Cannot delete current user");
		}
		userManager.removeUser(realm, subjectId);
		return ResponseEntity.ok(null);
	}
	
	
	@PutMapping("/console/dev/realms/{realm}/users/{subjectId:.*}/roles")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<User> updateRealmRoles(@PathVariable String realm, @PathVariable String subjectId, @RequestBody RolesBean bean) throws NoSuchRealmException, NoSuchUserException {
		userManager.updateRealmAuthorities(realm, subjectId, bean.getRoles());
		return ResponseEntity.ok(userManager.getUser(realm, subjectId));
	}

	@GetMapping("/console/dev/realms/{realm:.*}/providers")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<Collection<ConfigurableProvider>> getRealmProviders(@PathVariable String realm) throws NoSuchRealmException {
		return ResponseEntity.ok(providerManager.listProviders(realm));
	}
	
	@DeleteMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<Void> deleteRealmProvider(@PathVariable String realm, @PathVariable String providerId) throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
		providerManager.deleteProvider(realm, providerId);
		return ResponseEntity.ok(null);
	}
	
	@PostMapping("/console/dev/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<ConfigurableProvider> createRealmProvider(@PathVariable String realm, @Valid @RequestBody ProviderRegistrationBean registration) throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        Map<String, Object> configuration = registration.getConfiguration();

        ConfigurableProvider provider = providerManager.addProvider(realm, authority, type, name, configuration);
        return ResponseEntity.ok(provider);
	}
	
	@PutMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<ConfigurableProvider> updateRealmProvider(@PathVariable String realm, @PathVariable String providerId, @Valid @RequestBody ProviderRegistrationBean registration) throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // we update only configuration
        String name = registration.getName();
        Map<String, Object> configuration = registration.getConfiguration();
        boolean enabled = registration.isEnabled();
        
        provider.setName(name);
        provider.setConfiguration(configuration);
        provider.setEnabled(enabled);
        
        return ResponseEntity.ok(providerManager.updateProvider(realm, providerId, provider));
	}
	
	@PutMapping("/console/dev/realms/{realm}/providers/{providerId}/state")
    @PreAuthorize("hasAuthority('"+Config.R_ADMIN+"') or hasAuthority(#realm+':ROLE_ADMIN')")
	public ResponseEntity<ConfigurableProvider> updateRealmProviderState(@PathVariable String realm, @PathVariable String providerId, @RequestBody ProviderRegistrationBean registration) throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        boolean enabled = registration.isEnabled();
        if (enabled) {
        	provider = providerManager.registerProvider(realm, providerId);
        } else {
        	provider = providerManager.unregisterProvider(realm, providerId);
        }
        return ResponseEntity.ok(provider);
	}
	
	
    @GetMapping("/console/dev/realms/{realm:.*}/apps")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ClientApp>> getRealmClientApps(@PathVariable String realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(clientManager.listClientApps(realm));
    }
    
    @GetMapping("/console/dev/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> getRealmClientApp(@PathVariable String realm, @PathVariable String clientId)
            throws NoSuchRealmException, NoSuchClientException, SystemException {
        return ResponseEntity.ok(clientManager.getClientApp(realm, clientId));
    }

    @PostMapping("/console/dev/realms/{realm}/apps")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> createRealmClientApp(@PathVariable String realm,
            @Valid @RequestBody ClientApp app)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        // enforce realm match
        app.setRealm(realm);

        ClientApp clientApp = clientManager.registerClientApp(realm, app);
        return ResponseEntity.ok(clientApp);
    }

    @PutMapping("/console/dev/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> updateRealmClientApp(@PathVariable String realm,
            @PathVariable String clientId, @Valid @RequestBody ClientApp app)
            throws NoSuchRealmException, NoSuchClientException, SystemException {

        ClientApp clientApp = clientManager.updateClientApp(realm, clientId, app);
        return ResponseEntity.ok(clientApp);
    }

    @DeleteMapping("/console/dev/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteRealmClientApp(@PathVariable String realm, @PathVariable String clientId)
            throws NoSuchRealmException, NoSuchClientException, SystemException {
        clientManager.deleteClientApp(realm, clientId);
        return ResponseEntity.ok(null);
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
