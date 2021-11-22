package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.WebContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.AuditManager;
import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.MyUserManager;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.RealmStatsBean;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SpaceRoles;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.endpoint.OAuth2MetadataEndpoint;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.services.ServicesManager;

@RestController
@Hidden
public class DevController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.url}")
    private String applicationUrl;

    @Autowired
    private MyUserManager myUserManager;

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
    private SpaceRoleManager roleManager;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private OAuth2MetadataEndpoint oauth2MetadataEndpoint;

    @GetMapping("/dev")
    public ModelAndView developer() {
        UserDetails user = myUserManager.curUserDetails();
        if (user == null || (!user.isRealmDeveloper() && !user.isSystemDeveloper())) {
            throw new SecurityException();
        }

        return new ModelAndView("index");
    }

    @GetMapping("/console/dev/realms/{realm}/well-known/oauth2")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, Object>> getRealmOAuth2Metadata(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        // hack
        // TODO render proper per realm meta
        Map<String, Object> metadata = oauth2MetadataEndpoint.getAuthServerMetadata();
        return ResponseEntity.ok(metadata);

    }

    @GetMapping("/console/dev/realms/{realm}/well-known/url")
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

    @GetMapping("/console/dev/realms/{realm}/stats")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<RealmStatsBean> getRealmStats(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        RealmStatsBean bean = new RealmStatsBean();

        Realm realmObj = realmManager.getRealm(realm);
        bean.setRealm(realmObj);

        Long userCount = userManager.countUsers(realm);
        bean.setUsers(userCount);

        Collection<ConfigurableProvider> providers = providerManager.listProviders(realm);
        bean.setProviders(providers.size());

        int activeProviders = (int) providers.stream().filter(p -> providerManager.isProviderRegistered(realm, p))
                .count();
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

    /*
     * Subjects
     * 
     * TODO evaluate permission model
     */

    @GetMapping("/console/dev/subjects/{subjectId}")
    public ResponseEntity<Subject> getSubject(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchSubjectException {
        return ResponseEntity.ok(subjectService.getSubject(subjectId));
    }

    /*
     * Spaces
     */

    @GetMapping("/console/dev/rolespaces")
    public ResponseEntity<Collection<SpaceRole>> getMyRoleSpacesContexts()
            throws NoSuchRealmException, NoSuchUserException {
        return ResponseEntity.ok(roleManager.curContexts());
    }

    @GetMapping("/console/dev/rolespaces/users")
    public ResponseEntity<Page<SpaceRoles>> getRoleSpaceUsers(
            @RequestParam(required = false) String context,
            @RequestParam(required = false) String space,
            @RequestParam(required = false) String q, Pageable pageRequest)
            throws NoSuchRealmException, NoSuchUserException {
        if (invalidOwner(context, space)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roleManager.searchRoles(context, space, q, pageRequest));
    }

    @PostMapping("/console/dev/rolespaces/users")
    public ResponseEntity<SpaceRoles> addRoleSpaceRoles(@RequestBody SpaceRoles roles)
            throws NoSuchRealmException, NoSuchSubjectException {
        if (invalidOwner(roles.getContext(), roles.getSpace())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Collection<SpaceRole> spaceRoles = roles.getRoles().stream()
                .map(r -> new SpaceRole(roles.getContext(), roles.getSpace(), r))
                .collect(Collectors.toList());
        Collection<SpaceRole> result = roleManager.setRoles(roles.getSubject(), roles.getContext(), roles.getSpace(),
                spaceRoles);
        roles.setRoles(result.stream().map(r -> r.getRole()).collect(Collectors.toList()));

        return ResponseEntity.ok(roles);
    }

    private boolean invalidOwner(String context, String space) throws NoSuchRealmException {
        // current user should be owner of the space or of the parent
        Collection<SpaceRole> myRoles = roleManager.curRoles();
        SpaceRole spaceOwner = new SpaceRole(context, space, Config.R_PROVIDER);
        SpaceRole parentOwner = context != null ? SpaceRole.ownerOf(context) : null;
        return myRoles.stream().noneMatch(r -> r.equals(spaceOwner) || r.equals(parentOwner));
    }

//    /*
//     * REST style exception handling
//     */
//    @ExceptionHandler({
//            NoSuchRealmException.class,
//            NoSuchUserException.class,
//            NoSuchClientException.class,
//            NoSuchServiceException.class,
//            NoSuchScopeException.class,
//            NoSuchClaimException.class,
//            NoSuchProviderException.class
//    })
//    public ResponseEntity<Object> handleNotFoundException(Exception ex) {
//        HttpHeaders headers = new HttpHeaders();
//        HttpStatus status = HttpStatus.NOT_FOUND;
//        Map<String, Object> response = buildResponse(ex, status, null);
//
//        return new ResponseEntity<>(response, headers, status);
//    }
//
//    @ExceptionHandler({
//            InvalidDefinitionException.class,
//            IllegalArgumentException.class,
//            RegistrationException.class
//    })
//    public ResponseEntity<Object> handleBadRequestException(Exception ex) {
//        HttpHeaders headers = new HttpHeaders();
//        HttpStatus status = HttpStatus.BAD_REQUEST;
//        Map<String, Object> response = buildResponse(ex, status, null);
//
//        return new ResponseEntity<>(response, headers, status);
//    }
//
//    @ExceptionHandler({
//            SystemException.class,
//            RuntimeException.class,
//            IOException.class
//    })
//    public ResponseEntity<Object> handleSystemException(Exception ex) {
//        HttpHeaders headers = new HttpHeaders();
//        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
//        Map<String, Object> response = buildResponse(ex, status, null);
//
//        return new ResponseEntity<>(response, headers, status);
//    }
//
//    private Map<String, Object> buildResponse(Exception ex, HttpStatus status, Object body) {
//        Map<String, Object> response = new HashMap<>();
////        response.put("timestamp", new Date())
//        response.put("status", status.value());
//        response.put("error", status.getReasonPhrase());
//        response.put("message", ex.getMessage());
//
//        if (body != null) {
//            response.put("description", body);
//        }
//
//        logger.error("error processing request: " + ex.getMessage());
//        ex.printStackTrace();
//
//        return response;
//
//    }

}
