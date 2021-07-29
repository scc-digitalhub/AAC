package it.smartcommunitylab.aac.controller;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.WebContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AttributeManager;
import it.smartcommunitylab.aac.attributes.DefaultAttributesSet;
import it.smartcommunitylab.aac.audit.AuditManager;
import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
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
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.dto.RealmStatsBean;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SpaceRoles;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.endpoint.OAuth2MetadataEndpoint;
import it.smartcommunitylab.aac.roles.RoleManager;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServiceClaim;
import it.smartcommunitylab.aac.services.ServiceScope;
import it.smartcommunitylab.aac.services.ServicesManager;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
public class DevController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.url}")
    private String applicationUrl;
    
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
    @Autowired
    private AuditManager auditManager;
    @Autowired
    private RoleManager roleManager;
    @Autowired
    private AttributeManager attributeManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private OAuth2MetadataEndpoint oauth2MetadataEndpoint;

    @GetMapping("/dev")
    public ModelAndView developer() {
        UserDetails user = userManager.curUserDetails();
        if (user == null || !user.isRealmDeveloper()) {
            throw new SecurityException();
        }
        return new ModelAndView("index");
    }

    @GetMapping("/console/dev/realms")
    public ResponseEntity<Collection<Realm>> myRealms() throws NoSuchRealmException {
        UserDetails user = userManager.curUserDetails();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Collection<Realm> realms = user.getRealms().stream()
                .map(r -> {
                    try {
                        return realmManager.getRealm(r);
                    } catch (NoSuchRealmException e) {
                        return null;
                    }
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());

        if (user.hasAuthority(Config.R_ADMIN)) {
            // system admin ccan access all realms
            realms = realmManager.listRealms();
        }

        return ResponseEntity.ok(realms);
    }

    @GetMapping("/console/dev/realms/{realm:.*}/well-known/oauth2")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, Object>> getRealmOAuth2Metadata(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        // hack
        // TODO render proper per realm meta
        Map<String, Object> metadata = oauth2MetadataEndpoint.getAuthServerMetadata();
        return ResponseEntity.ok(metadata);

    }
    
    @GetMapping("/console/dev/realms/{realm:.*}/well-known/url")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, String>> getRealmBaseUrl(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            HttpServletRequest request) throws NoSuchRealmException {
        // hack
        
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        URI requestUri = uri.build().toUri();
        builder.scheme(requestUri.getScheme()).host(requestUri.getHost()).port(requestUri.getPort());
        String baseUrl = builder.build().toString();
        
        // TODO render proper per realm meta
        Map<String, String> metadata = new HashMap<>();
        metadata.put("applicationUrl", applicationUrl);
        metadata.put("requestUrl", requestUri.toString());
        metadata.put("baseUrl", baseUrl);

        return ResponseEntity.ok(metadata);

    }

    @GetMapping("/console/dev/realms/{realm:.*}/stats")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<RealmStatsBean> getRealmStats(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        RealmStatsBean bean = new RealmStatsBean();

        Realm realmObj = realmManager.getRealm(realm);
        bean.setRealm(realmObj);

        Long userCount = userManager.countUsers(realm);
        bean.setUsers(userCount);

        Collection<ConfigurableProvider> providers = providerManager
                .listProviders(realm, ConfigurableProvider.TYPE_IDENTITY);
        bean.setProviders(providers.size());

        int activeProviders = (int) providers.stream().filter(p -> providerManager.isProviderRegistered(p)).count();
        bean.setProvidersActive(activeProviders);

        Collection<ClientApp> apps = clientManager.listClientApps(realm);
        bean.setApps(apps.size());

        bean.setServices(serviceManager.listServices(realm).size());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date after = cal.getTime();

        bean.setEvents(auditManager.countRealmEvents(realm, null, after, null));

        bean.setLoginCount(auditManager.countRealmEvents(realm, "USER_AUTHENTICATION_SUCCESS", after, null));
        List<RealmAuditEvent> loginEvents = auditManager
                .findRealmEvents(realm, "USER_AUTHENTICATION_SUCCESS", after, null).stream()
                .limit(5)
                .map(e -> {
                    // clear event details
                    Map<String, Object> d = new HashMap<>(e.getData());
                    d.remove("details");

                    return new RealmAuditEvent(e.getRealm(), e.getTimestamp(), e.getPrincipal(), e.getType(), d);
                })
                .collect(Collectors.toList());

        bean.setLoginEvents(loginEvents);

        bean.setRegistrationCount(auditManager.countRealmEvents(realm, "USER_REGISTRATION", after, null));
        List<RealmAuditEvent> registrationEvents = auditManager
                .findRealmEvents(realm, "USER_REGISTRATION", after, null).stream()
                .limit(5)
                .map(e -> {
                    // clear event details
                    Map<String, Object> d = new HashMap<>(e.getData());
                    d.remove("details");

                    return new RealmAuditEvent(e.getRealm(), e.getTimestamp(), e.getPrincipal(), e.getType(), d);
                })
                .collect(Collectors.toList());
        bean.setRegistrationEvents(registrationEvents);

        return ResponseEntity.ok(bean);
    }

    @GetMapping("/console/dev/realms/{realm:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Realm> getRealm(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(realmManager.getRealm(realm));
    }

    @GetMapping("/console/dev/realms/{realm}/export")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void exportRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            HttpServletResponse res)
            throws NoSuchRealmException, SystemException, IOException {
        Realm r = realmManager.getRealm(realm);

//      String s = yaml.dump(clientApp);
        String s = yamlObjectMapper.writeValueAsString(r);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=realm-" + r.getSlug() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();

    }

    @PostMapping("/console/dev/realms/{realm}/custom")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void previewRealm(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = true) @Valid @NotBlank String template,
            @RequestBody @Valid CustomizationBean cb,
            HttpServletRequest req, HttpServletResponse res)
            throws NoSuchRealmException, SystemException, IOException {

        WebContext ctx = new WebContext(req, res, servletContext, req.getLocale());
        String s = devManager.previewRealmTemplate(realm, template, cb, ctx);

        // write as file
        res.setContentType("text/html");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();

    }

    /*
     * Users
     */
    @GetMapping("/console/dev/realms/{realm:.*}/users")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Page<User>> getRealmUsers(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false) String q, Pageable pageRequest) throws NoSuchRealmException {
        return ResponseEntity.ok(userManager.searchUsers(realm, q, pageRequest));
    }

    @GetMapping("/console/dev/realms/{realm}/users/{subjectId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<User> getRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User user = userManager.getUser(realm, subjectId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/console/dev/realms/{realm}/users/{subjectId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
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
    public ResponseEntity<User> updateRealmRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestBody RolesBean bean) throws NoSuchRealmException, NoSuchUserException {
        userManager.updateRealmAuthorities(realm, subjectId, bean.getRoles());
        return ResponseEntity.ok(userManager.getUser(realm, subjectId));
    }

    @PostMapping("/console/dev/realms/{realm}/users/invite")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Void> inviteUser(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody InvitationBean bean)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException {
        userManager.inviteUser(realm, bean.getUsername(), bean.getSubjectId(), bean.getRoles());
        return ResponseEntity.ok(null);
    }

    /*
     * Providers
     */

    @GetMapping("/console/dev/realms/{realm:.*}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ConfigurableProvider>> getRealmProviders(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {

        List<ConfigurableProvider> providers = providerManager
                .listProviders(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(providers);
    }

    @GetMapping("/console/dev/realms/{realm:.*}/providertemplates")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Collection<ConfigurableProvider>> getRealmProviderTemplates(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {

        List<ConfigurableProvider> providers = providerManager
                .listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(providers);
    }

    @GetMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> getRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        ConfigurableProvider provider = providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY,
                providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        // if registered fetch active configuration
        if (isRegistered) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
            Map<String, Serializable> configMap = idp.getConfiguration().getConfiguration();
            // we replace config instead of merging, when active config can not be
            // modified anyway
            provider.setConfiguration(configMap);
        }

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return ResponseEntity.ok(provider);
    }

    @DeleteMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        providerManager.deleteProvider(realm, providerId);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/console/dev/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> createRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ConfigurableProvider registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        // unpack and build model
        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();

        ConfigurableProvider provider = new ConfigurableProvider(authority, null, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setType(type);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);
        provider.setEvents(events);
        provider.setConfiguration(configuration);

        provider = providerManager.addProvider(realm, provider);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return ResponseEntity.ok(provider);
    }

    @PutMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> updateRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ConfigurableProvider registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        provider.setName(name);
        provider.setDescription(description);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);

        provider.setEvents(events);
        provider.setConfiguration(configuration);
        provider.setHookFunctions(hookFunctions);

        provider = providerManager.updateProvider(realm, providerId, provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return ResponseEntity.ok(provider);
    }

    @PutMapping("/console/dev/realms/{realm}/providers/{providerId}/state")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> updateRealmProviderState(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody ConfigurableProvider registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        boolean enabled = registration.isEnabled();

        if (enabled) {
            provider = providerManager.registerProvider(realm, providerId);
        } else {
            provider = providerManager.unregisterProvider(realm, providerId);
        }

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return ResponseEntity.ok(provider);
    }

    @GetMapping("/console/dev/realms/{realm}/providers/{providerId:.*}/export")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void exportRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            HttpServletResponse res)
            throws NoSuchProviderException, NoSuchRealmException, SystemException, IOException {
        ConfigurableProvider provider = providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY,
                providerId);

//      String s = yaml.dump(clientApp);
        String s = yamlObjectMapper.writeValueAsString(provider);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=idp-" + provider.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();

    }

    @PutMapping("/console/dev/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ConfigurableProvider> importProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_XYAML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            ConfigurableProvider registration = yamlObjectMapper.readValue(file.getInputStream(),
                    ConfigurableProvider.class);

            // unpack and build model
            String id = registration.getProvider();
            String authority = registration.getAuthority();
            String type = registration.getType();
            String name = registration.getName();
            String description = registration.getDescription();
            String persistence = registration.getPersistence();
            String events = registration.getEvents();
            Map<String, Serializable> configuration = registration.getConfiguration();
            Map<String, String> hookFunctions = registration.getHookFunctions();

            ConfigurableProvider provider = new ConfigurableProvider(authority, id, realm);
            provider.setName(name);
            provider.setDescription(description);
            provider.setType(type);
            provider.setEnabled(false);
            provider.setPersistence(persistence);
            provider.setEvents(events);
            provider.setConfiguration(configuration);
            provider.setHookFunctions(hookFunctions);

            provider = providerManager.addProvider(realm, provider);

            // fetch also configuration schema
            JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
            provider.setSchema(schema);

            return ResponseEntity.ok(provider);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    /*
     * ClientApps
     */
    @GetMapping("/console/dev/realms/{realm:.*}/apps")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ClientApp>> getRealmClientApps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(clientManager.listClientApps(realm));
    }

    @GetMapping("/console/dev/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> getRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
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
    public ResponseEntity<ClientApp> createRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
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

    @PutMapping("/console/dev/realms/{realm}/apps")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> importRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_XYAML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            ClientApp app = yamlObjectMapper.readValue(file.getInputStream(), ClientApp.class);
            app.setRealm(realm);

            ClientApp clientApp = clientManager.registerClientApp(realm, app);

            // fetch also configuration schema
            JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
            clientApp.setSchema(schema);

            return ResponseEntity.ok(clientApp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RegistrationException(e.getMessage());
        }

    }

    @PutMapping("/console/dev/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ClientApp> updateRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @Valid @RequestBody ClientApp app)
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
    public ResponseEntity<Void> deleteRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
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

        clientManager.resetClientCredentials(realm, clientId);

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
    public ResponseEntity<OAuth2AccessToken> testRealmClientAppOAuth2(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @PathVariable String grantType)
            throws NoSuchRealmException, NoSuchClientException, SystemException {

        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // check if oauth2
        if (!clientApp.getType().equals(SystemKeys.CLIENT_TYPE_OAUTH2)) {
            throw new IllegalArgumentException("client does not support oauth2");
        }

        OAuth2AccessToken accessToken = devManager.testOAuth2Flow(realm, clientId, grantType);

        return ResponseEntity.ok(accessToken);
    }

    @PostMapping("/console/dev/realms/{realm}/apps/{clientId:.*}/claims")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<FunctionValidationBean> testRealmClientAppClaims(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @Valid @RequestBody FunctionValidationBean function)
            throws NoSuchRealmException, NoSuchClientException, SystemException, NoSuchResourceException,
            InvalidDefinitionException {

        try {
            // TODO expose context personalization in UI
            function = devManager.testClientClaimMapping(realm, clientId, function);

        } catch (InvalidDefinitionException | RuntimeException e) {
            // translate error
            function.addError(e.getMessage());

//            // wrap error
//            Map<String, Serializable> res = new HashMap<>();
//            res.put("error", e.getClass().getName());
//            res.put("message", e.getMessage());
//            return ResponseEntity.badRequest().body(res);
        }

        return ResponseEntity.ok(function);
    }

    @GetMapping("/console/dev/realms/{realm}/apps/{clientId:.*}/export")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public void exportRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            HttpServletResponse res)
            throws NoSuchRealmException, NoSuchClientException, SystemException, IOException {
//        Yaml yaml = YamlUtils.getInstance(true, ClientApp.class);

        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

//        String s = yaml.dump(clientApp);
        String s = yamlObjectMapper.writeValueAsString(clientApp);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=clientapp-" + clientApp.getName() + ".yaml");
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
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Resource> getResource(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String resourceId)
            throws NoSuchResourceException {
        return ResponseEntity.ok(scopeManager.getResource(resourceId));
    }

    @GetMapping("/console/dev/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<List<Service>> listServices(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        return ResponseEntity.ok(serviceManager.listServices(realm));
    }

    @GetMapping("/console/dev/realms/{realm}/services/{serviceId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> getService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException {
        return ResponseEntity.ok(serviceManager.getService(realm, serviceId));
    }

    @GetMapping("/console/dev/realms/{realm}/services/{serviceId}/yaml")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public void exportService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId, HttpServletResponse res)
            throws NoSuchRealmException, NoSuchServiceException, IOException {
//        Yaml yaml = YamlUtils.getInstance(true, Service.class);

        Service service = serviceManager.getService(realm, serviceId);

//        String s = yaml.dump(service);
        String s = yamlObjectMapper.writeValueAsString(service);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=service-" + service.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();
    }

    @PostMapping("/console/dev/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> addService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid Service s)
            throws NoSuchRealmException {
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

    @PutMapping("/console/dev/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> importService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_XYAML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            Service s = yamlObjectMapper.readValue(file.getInputStream(), Service.class);
            s.setRealm(realm);

            Service service = serviceManager.addService(realm, s);

            return ResponseEntity.ok(service);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RegistrationException(e.getMessage());
        }

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
    public ResponseEntity<FunctionValidationBean> validate(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @Valid @RequestBody FunctionValidationBean function)
            throws NoSuchServiceException, NoSuchRealmException, SystemException, InvalidDefinitionException {

        try {
            // TODO expose context personalization in UI
            function = devManager.testServiceClaimMapping(realm, serviceId, function);

        } catch (InvalidDefinitionException | RuntimeException e) {
            // translate error
            function.addError(e.getMessage());
        }

        return ResponseEntity.ok(function);
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
        return ResponseEntity
                .ok(serviceManager.addServiceScopeApproval(realm, serviceId, scope, clientId, duration, approved));
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
        return ResponseEntity.ok(null);
    }

    /*
     * Spaces
     */
    @GetMapping("/console/dev/rolespaces/users")
    public ResponseEntity<Page<SpaceRoles>> getRoleSpaceUsers(
            @RequestParam(required = false) String context,
            @RequestParam(required = false) String space,
            @RequestParam(required = false) String q, Pageable pageRequest)
            throws NoSuchRealmException, NoSuchUserException {
        if (invalidOwner(context, space)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roleManager.getContextRoles(context, space, q, pageRequest));
    }

    @PostMapping("/console/dev/rolespaces/users")
    public ResponseEntity<SpaceRoles> addRoleSpaceRoles(@RequestBody SpaceRoles roles)
            throws NoSuchRealmException, NoSuchUserException {
        if (invalidOwner(roles.getContext(), roles.getSpace())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roleManager.saveContextRoles(roles.getSubject(), roles.getContext(), roles.getSpace(),
                roles.getRoles()));
    }

    private boolean invalidOwner(String context, String space) throws NoSuchUserException, NoSuchRealmException {
        // current user should be owner of the space or of the parent
        Collection<SpaceRole> myRoles = roleManager.curUserRoles();
        SpaceRole spaceOwner = new SpaceRole(context, space, Config.R_PROVIDER);
        SpaceRole parentOwner = context != null ? SpaceRole.ownerOf(context) : null;
        return myRoles.stream().noneMatch(r -> r.equals(spaceOwner) || r.equals(parentOwner));
    }

    /*
     * Audit events
     */

    @GetMapping("/console/dev/realms/{realm}/audit")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Collection<RealmAuditEvent>> findEvents(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, name = "type") Optional<String> type,
            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
            throws NoSuchRealmException {

        return ResponseEntity
                .ok(auditManager.findRealmEvents(realm,
                        type.orElse(null), after.orElse(null), before.orElse(null)));

    }

    /*
     * Attributes sets
     */

    @GetMapping("/console/dev/realms/{realm}/attributeset")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<AttributeSet> listAttributeSets(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        logger.debug("list attribute sets for realm " + String.valueOf(realm));
        return attributeManager.listAttributeSets(realm);
    }

    @GetMapping("/console/dev/realms/{realm}/attributeset/{setId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public AttributeSet getAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId)
            throws NoSuchAttributeSetException, NoSuchRealmException {
        logger.debug("get attribute set " + String.valueOf(setId) + " for realm " + String.valueOf(realm));
        return attributeManager.getAttributeSet(realm, setId);
    }

    @PostMapping("/console/dev/realms/{realm}/attributeset")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public AttributeSet addAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid DefaultAttributesSet s) throws NoSuchRealmException {
        logger.debug("add attribute set for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("attribute set bean " + String.valueOf(s));
        }
        return attributeManager.addAttributeSet(realm, s);
    }

    @PutMapping("/console/dev/realms/{realm}/attributeset/{setId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public AttributeSet updateAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId,
            @RequestBody @Valid DefaultAttributesSet s) throws NoSuchAttributeSetException, NoSuchRealmException {
        logger.debug("update attribute set " + String.valueOf(setId) + " for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("attribute set bean " + String.valueOf(s));
        }
        return attributeManager.updateAttributeSet(realm, setId, s);
    }

    @DeleteMapping("/console/dev/realms/{realm}/attributeset/{setId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void deleteAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId)
            throws NoSuchAttributeSetException {
        logger.debug("delete attribute set " + String.valueOf(setId) + " for realm " + String.valueOf(realm));
        attributeManager.deleteAttributeSet(realm, setId);
    }

    @PutMapping("/console/dev/realms/{realm}/attributeset")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public AttributeSet importAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import attribute set to realm " + String.valueOf(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString()) &&
                        !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            DefaultAttributesSet s = yamlObjectMapper.readValue(file.getInputStream(),
                    DefaultAttributesSet.class);

            if (logger.isTraceEnabled()) {
                logger.trace("attribute set bean: " + String.valueOf(s));
            }

            return attributeManager.addAttributeSet(realm, s);

        } catch (Exception e) {
            logger.error("import attribute set error: " + e.getMessage());
            throw e;
        }

    }

    @GetMapping("/console/dev/realms/{realm}/attributeset/{setId}/yaml")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void exportAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId, HttpServletResponse res)
            throws NoSuchRealmException, NoSuchAttributeSetException, IOException {

        AttributeSet set = attributeManager.getAttributeSet(realm, setId);

//        String s = yaml.dump(service);
        String s = yamlObjectMapper.writeValueAsString(set);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=attributeset-" + set.getIdentifier() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();
    }

    /*
     * REST style exception handling
     */
    @ExceptionHandler({
            NoSuchRealmException.class,
            NoSuchUserException.class,
            NoSuchClientException.class,
            NoSuchServiceException.class,
            NoSuchScopeException.class,
            NoSuchClaimException.class,
            NoSuchProviderException.class
    })
    public ResponseEntity<Object> handleNotFoundException(Exception ex) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.NOT_FOUND;
        Map<String, Object> response = buildResponse(ex, status, null);

        return new ResponseEntity<>(response, headers, status);
    }

    @ExceptionHandler({
            InvalidDefinitionException.class,
            IllegalArgumentException.class,
            RegistrationException.class
    })
    public ResponseEntity<Object> handleBadRequestException(Exception ex) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> response = buildResponse(ex, status, null);

        return new ResponseEntity<>(response, headers, status);
    }

    @ExceptionHandler({
            SystemException.class,
            RuntimeException.class,
            IOException.class
    })
    public ResponseEntity<Object> handleSystemException(Exception ex) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> response = buildResponse(ex, status, null);

        return new ResponseEntity<>(response, headers, status);
    }

    private Map<String, Object> buildResponse(Exception ex, HttpStatus status, Object body) {
        Map<String, Object> response = new HashMap<>();
//        response.put("timestamp", new Date())
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", ex.getMessage());

        if (body != null) {
            response.put("description", body);
        }

        logger.error("error processing request: " + ex.getMessage());
        ex.printStackTrace();

        return response;

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

    public static class InvitationBean {

        private String username, subjectId;

        private List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
        }

    }

    public static class ValidationBean {
        private List<String> scopes;
        private String mapping;
        private String kind;

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

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

    }

//    public static class ValidationResultBean {
//        private String errorMessage;
//        private Map<String, Object> data;
//
//        public String getErrorMessage() {
//            return errorMessage;
//        }
//
//        public void setErrorMessage(String errorMessage) {
//            this.errorMessage = errorMessage;
//        }
//
//        public Map<String, Object> getData() {
//            return data;
//        }
//
//        public void setData(Map<String, Object> data) {
//            this.data = data;
//        }
//    }
}
