package it.smartcommunitylab.aac.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.approval.Approval;
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
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.DevManager;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.dto.ProviderRegistrationBean;
import it.smartcommunitylab.aac.dto.RealmStatsBean;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServiceClaim;
import it.smartcommunitylab.aac.services.ServiceScope;
import it.smartcommunitylab.aac.services.ServicesManager;
import it.smartcommunitylab.aac.utils.YamlUtils;
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
    @Autowired
    private ScopeManager scopeManager;
    @Autowired
    private DevManager devManager;
    @Autowired
    private ServicesManager serviceManager;

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

    @GetMapping("/console/dev/realms/{realm:.*}/stats")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<RealmStatsBean> getRealmStats(@PathVariable String realm) throws NoSuchRealmException {
    	RealmStatsBean bean = new RealmStatsBean();
    	Realm realmObj = realmManager.getRealm(realm);
    	bean.setRealm(realmObj);
    	Long userCount = userManager.countUsers(realm);
    	bean.setUsers(userCount);
        Collection<ConfigurableProvider> providers = providerManager
                .listProviders(realm, ConfigurableProvider.TYPE_IDENTITY);
        bean.setProviders(providers.size());
    	Collection<ClientApp> apps = clientManager.listClientApps(realm);
    	bean.setApps(apps.size());
    	bean.setServices(serviceManager.listServices(realm).size());
        return ResponseEntity.ok(bean);
    }

    @GetMapping("/console/dev/realms/{realm:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Realm> getRealm(@PathVariable String realm) throws NoSuchRealmException {
        return ResponseEntity.ok(realmManager.getRealm(realm));
    }

    /*
     * Users
     */
    @GetMapping("/console/dev/realms/{realm:.*}/users")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Page<User>> getRealmUsers(@PathVariable String realm,
            @RequestParam(required = false) String q, Pageable pageRequest) throws NoSuchRealmException {
        return ResponseEntity.ok(userManager.searchUsers(realm, q, pageRequest));
    }

    @DeleteMapping("/console/dev/realms/{realm}/users/{subjectId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRealmUser(@PathVariable String realm, @PathVariable String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User curUser = userManager.curUser(realm);
        if (curUser.getSubjectId().equals(subjectId)) {
            throw new IllegalArgumentException("Cannot delete current user");
        }
        userManager.removeUser(realm, subjectId);
        return ResponseEntity.ok(null);
    }

    @PutMapping("/console/dev/realms/{realm}/users/{subjectId:.*}/roles")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<User> updateRealmRoles(@PathVariable String realm, @PathVariable String subjectId,
            @RequestBody RolesBean bean) throws NoSuchRealmException, NoSuchUserException {
        userManager.updateRealmAuthorities(realm, subjectId, bean.getRoles());
        return ResponseEntity.ok(userManager.getUser(realm, subjectId));
    }

    /*
     * Providers
     */

    @GetMapping("/console/dev/realms/{realm:.*}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Collection<ProviderRegistrationBean>> getRealmProviders(@PathVariable String realm)
            throws NoSuchRealmException {

        List<ProviderRegistrationBean> providers = providerManager
                .listProviders(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(cp);
                    res.setRegistered(providerManager.isProviderRegistered(cp));
                    return res;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(providers);
    }

    @GetMapping("/console/dev/realms/{realm:.*}/providertemplates")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Collection<ProviderRegistrationBean>> getRealmProviderTemplates(@PathVariable String realm)
            throws NoSuchRealmException {

        List<ProviderRegistrationBean> providers = providerManager
                .listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(cp);
                    res.setRegistered(providerManager.isProviderRegistered(cp));
                    return res;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(providers);
    }
    
    @GetMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ProviderRegistrationBean> getRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        ConfigurableProvider provider = providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY,
                providerId);
        ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        res.setRegistered(isRegistered);

        // if registered fetch active configuration
        if (isRegistered) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
            Map<String, Serializable> configMap = idp.getConfiguration().getConfiguration();
            // we replace config instead of merging, when active config can not be
            // modified anyway
            res.setConfiguration(configMap);
        }

        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRealmProvider(@PathVariable String realm, @PathVariable String providerId)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        providerManager.deleteProvider(realm, providerId);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/console/dev/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ProviderRegistrationBean> createRealmProvider(@PathVariable String realm,
            @Valid @RequestBody ProviderRegistrationBean registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        // unpack and build model
        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        Map<String, Serializable> configuration = registration.getConfiguration();

        ConfigurableProvider provider = new ConfigurableProvider(authority, null, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setType(type);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setConfiguration(configuration);

        provider = providerManager.addProvider(realm, provider);

        return ResponseEntity.ok(ProviderRegistrationBean.fromProvider(provider));
    }

    @PutMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ProviderRegistrationBean> updateRealmProvider(@PathVariable String realm,
            @PathVariable String providerId, @Valid @RequestBody ProviderRegistrationBean registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        boolean enabled = (registration.getEnabled() != null ? registration.getEnabled().booleanValue() : false);
        Map<String, Serializable> configuration = registration.getConfiguration();

        provider.setName(name);
        provider.setDescription(description);
        provider.setPersistence(persistence);
        provider.setConfiguration(configuration);
        provider.setEnabled(enabled);

        provider = providerManager.updateProvider(realm, providerId, provider);
        ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        res.setRegistered(isRegistered);

        return ResponseEntity.ok(res);
    }

    @PutMapping("/console/dev/realms/{realm}/providers/{providerId}/state")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ProviderRegistrationBean> updateRealmProviderState(@PathVariable String realm,
            @PathVariable String providerId, @RequestBody ProviderRegistrationBean registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        boolean enabled = (registration.getEnabled() != null ? registration.getEnabled().booleanValue() : true);

        if (enabled) {
            provider = providerManager.registerProvider(realm, providerId);
        } else {
            provider = providerManager.unregisterProvider(realm, providerId);
        }

        ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        res.setRegistered(isRegistered);

        return ResponseEntity.ok(res);
    }

    /*
     * ClientApps
     */
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

        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);
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

        // fetch also configuration schema
        JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);
    }

    @PutMapping("/console/dev/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> updateRealmClientApp(@PathVariable String realm,
            @PathVariable String clientId, @Valid @RequestBody ClientApp app)
            throws NoSuchRealmException, NoSuchClientException, SystemException {

        ClientApp clientApp = clientManager.updateClientApp(realm, clientId, app);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
        clientApp.setSchema(schema);

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

    @DeleteMapping("/console/dev/realms/{realm}/apps/{clientId:.*}/credentials")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> resetRealmClientAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {

        ClientCredentials credentials = clientManager.resetClientCredentials(realm, clientId);

        // re-read app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);

    }
    
    @GetMapping("/console/dev/realms/{realm}/apps/{clientId:.*}/oauth2/{grantType}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<OAuth2AccessToken> testRealmClientAppOAuth2(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String grantType)
            throws NoSuchRealmException, NoSuchClientException, SystemException {

        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        //check if oauth2
        if(!clientApp.getType().equals(SystemKeys.CLIENT_TYPE_OAUTH2)) {
            throw new IllegalArgumentException("client does not support oauth2");
        }
        
        OAuth2AccessToken accessToken = devManager.testOAuth2Flow(realm, clientId, grantType);

        return ResponseEntity.ok(accessToken);
    }
    
    @PostMapping("/console/dev/realms/{realm}/apps/{clientId:.*}/claims")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, Serializable>> testRealmClientAppClaims(@PathVariable String realm, @PathVariable String clientId,
            @RequestBody String functionCode)
            throws NoSuchRealmException, NoSuchClientException, SystemException, NoSuchResourceException, InvalidDefinitionException {

        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);
        try {
        Map<String, Serializable> result = devManager.testClientClaimMapping(realm, clientId, functionCode);

        return ResponseEntity.ok(result);
        
        } catch (InvalidDefinitionException | RuntimeException e) {
            e.printStackTrace();
            //wrap error
            Map<String, Serializable> res =  new HashMap<>();
            res.put("error",e.getClass().getName());
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/console/dev/realms/{realm}/apps/{clientId:.*}/yaml")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public void getRealmClientApp(@PathVariable String realm, @PathVariable String clientId,
            HttpServletResponse res)
            throws NoSuchRealmException, NoSuchClientException, SystemException, IOException {
        Yaml yaml = YamlUtils.getInstance(true, ClientApp.class);
        
        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        
        String s = yaml.dump(clientApp);
        //write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition","attachment;filename="+clientApp.getName()+".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();
    }
    
    /*
     * Scopes and resources
     */
    @GetMapping("/console/dev/realms/{realm}/scopes")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<Scope>> listScopes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) {
        return ResponseEntity.ok(scopeManager.listScopes());
    }

    @GetMapping("/console/dev/realms/{realm}/scopes/{scope:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Scope> getScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope) throws NoSuchScopeException {
        return ResponseEntity.ok(scopeManager.getScope(scope));
    }

    @GetMapping("/console/dev/realms/{realm}/resources")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<Resource>> listResources(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) {
        return ResponseEntity.ok(scopeManager.listResources());
    }

    @GetMapping("/console/dev/realms/{realm}/resources/{resourceId:.*}")
    public ResponseEntity<Resource> getResource(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String resourceId)
            throws NoSuchResourceException {
        return ResponseEntity.ok(scopeManager.getResource(resourceId));
    }

    @GetMapping("/console/dev/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<List<Service>> listServices(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
    	return ResponseEntity.ok(serviceManager.listServices(realm));
    }
    @GetMapping("/console/dev/realms/{realm}/services/{serviceId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> getService(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm, @PathVariable String serviceId) throws NoSuchRealmException, NoSuchServiceException {
    	return ResponseEntity.ok(serviceManager.getService(realm, serviceId));
    }
    @GetMapping("/console/dev/realms/{realm}/services/{serviceId}/yaml")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public void exportService(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm, @PathVariable String serviceId, HttpServletResponse res) throws NoSuchRealmException, NoSuchServiceException, IOException {
    	Yaml yaml = YamlUtils.getInstance(true, Service.class);
        
    	Service service = serviceManager.getService(realm, serviceId);
    	
    	String s = yaml.dump(service);
        //write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition","attachment;filename="+service.getName()+".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();
    }

    @PostMapping("/console/dev/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> addService(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm, @RequestBody @Valid Service s) throws NoSuchRealmException {
    	return ResponseEntity.ok(serviceManager.addService(realm, s));
    }

    @PutMapping("/console/dev/realms/{realm}/services/{serviceId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> updateService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid Service s) throws NoSuchServiceException, NoSuchRealmException {
        return ResponseEntity.ok(serviceManager.updateService(realm, serviceId, s));
    }

    @DeleteMapping("/console/dev/realms/{realm}/services/{serviceId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException {
        serviceManager.deleteService(realm, serviceId);
        return ResponseEntity.ok(null);
    }
    @GetMapping("/console/dev/services/nsexists")
    public ResponseEntity<Boolean> checkNamespace(@RequestParam String ns) throws NoSuchRealmException {
    	return ResponseEntity.ok(serviceManager.checkServiceNamespace(ns));
    }
    
    @PostMapping("/console/dev/realms/{realm}/services/{serviceId}/claims")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceClaim> addServiceClaim(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid ServiceClaim s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.addServiceClaim(realm, serviceId, s));
    }

    @PutMapping("/console/dev/realms/{realm}/services/{serviceId}/claims/{key}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceClaim> updateServiceClaim(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.KEY_PATTERN) String key,
            @RequestBody @Valid ServiceClaim s)
            throws NoSuchClaimException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.updateServiceClaim(realm, serviceId, key, s));
    }

    @DeleteMapping("/console/dev/realms/{realm}/services/{serviceId}/claims/{key}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteServiceClaim(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.KEY_PATTERN) String key)
            throws NoSuchClaimException, NoSuchServiceException {
        serviceManager.deleteServiceClaim(realm, serviceId, key);
        return ResponseEntity.ok(null);
    }


    @PostMapping("/console/dev/realms/{realm}/services/{serviceId}/claims/validate")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ValidationResultBean> validate(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId, @RequestBody ValidationBean bean) throws NoSuchServiceException, NoSuchRealmException {
    	Service service = serviceManager.getService(realm, serviceId);
    	ValidationResultBean res = new ValidationResultBean();
    	res.data = new HashMap<>();
    	// TODO
    	return ResponseEntity.ok(res);
    }
    
    @PostMapping("/console/dev/realms/{realm}/services/{serviceId}/scopes")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceScope> addServiceScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid ServiceScope s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.addServiceScope(realm, serviceId, s));
    }

    @PutMapping("/console/dev/realms/{realm}/services/{serviceId}/scopes/{scope}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceScope> updateServiceScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestBody @Valid ServiceScope s)
            throws NoSuchScopeException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.updateServiceScope(realm, serviceId, scope, s));
    }

    @DeleteMapping("/console/dev/realms/{realm}/services/{serviceId}/scopes/{scope}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteServiceScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchScopeException, NoSuchServiceException {
        serviceManager.deleteServiceScope(realm, serviceId, scope);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/console/dev/realms/{realm}/services/{serviceId}/scopes/{scope}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<Approval>> getServiceScopeApprovals(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        return ResponseEntity.ok(serviceManager.getServiceScopeApprovals(realm, serviceId, scope));
    }
    @GetMapping("/console/dev/realms/{realm}/services/{serviceId}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<Approval>> getServiceApprovals(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        return ResponseEntity.ok(serviceManager.getServiceApprovals(realm, serviceId));
    }

    @PostMapping("/console/dev/realms/{realm}/services/{serviceId}/scopes/{scope}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Approval> addServiceScopeApproval(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestParam String clientId,
            @RequestParam(required = false, defaultValue = "true") boolean approved)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        int duration = SystemKeys.DEFAULT_APPROVAL_VALIDITY;
        return ResponseEntity.ok(serviceManager.addServiceScopeApproval(realm, serviceId, scope, clientId, duration, approved));
    }

    @DeleteMapping("/console/dev/realms/{realm}/services/{serviceId}/scopes/{scope}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteServiceScopeApproval(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestParam String clientId)
            throws NoSuchRealmException, NoSuchScopeException, NoSuchServiceException {
        serviceManager.revokeServiceScopeApproval(realm, serviceId, scope, clientId);
        return  ResponseEntity.ok(null);
    }
    
    /*
     * DTO
     */
    public static class RolesBean {

        private List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

    }
    public static class ValidationBean {
    	private List<String> scopes;
    	private String mapping;
		public List<String> getScopes() {
			return scopes;
		}
		public void setScopes(List<String> scopes) {
			this.scopes = scopes;
		}
		public String getMapping() {
			return mapping;
		}
		public void setMapping(String mapping) {
			this.mapping = mapping;
		}
    }
    public static class ValidationResultBean {
    	private String errorMessage;
    	private Map<String, Object> data;
		public String getErrorMessage() {
			return errorMessage;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		public Map<String, Object> getData() {
			return data;
		}
		public void setData(Map<String, Object> data) {
			this.data = data;
		}
    }
}
