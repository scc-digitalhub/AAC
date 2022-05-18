package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevClientAppController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<ClientApp>>> typeRef = new TypeReference<Map<String, List<ClientApp>>>() {
    };

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private DevManager devManager;

    @Autowired
    private RealmRoleManager roleManager;

    @Autowired
    private SpaceRoleManager spaceRoleManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * ClientApps
     */
    @GetMapping("/realms/{realm}/apps")
    public ResponseEntity<Collection<ClientApp>> getRealmClientApps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(clientManager.listClientApps(realm));
    }

    @GetMapping("/realms/{realm}/apps/search")
    public ResponseEntity<Page<ClientApp>> searchRealmClientApps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false) String q, Pageable pageRequest)
            throws NoSuchRealmException {
        return ResponseEntity.ok(clientManager.searchClientApps(realm, q, pageRequest));
    }

    @GetMapping("/realms/{realm}/apps/{clientId}")
    public ResponseEntity<ClientApp> getRealmClientApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException, SystemException {

        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientId);
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);
    }

    @PostMapping("/realms/{realm}/apps")
    public ResponseEntity<ClientApp> createRealmClientApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull ClientApp app)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        // enforce realm match
        app.setRealm(realm);

        ClientApp clientApp = clientManager.registerClientApp(realm, app);

        try {
            // fetch also configuration schema
            JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientApp.getClientId());
            clientApp.setSchema(schema);
        } catch (NoSuchClientException e) {
        }

        return ResponseEntity.ok(clientApp);
    }

    @PutMapping("/realms/{realm}/apps")
    public ResponseEntity<Collection<ClientApp>> importRealmClientApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() == null) {
            throw new IllegalArgumentException("invalid file");
        }

        if (!SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType())
                && !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType())
                && !SystemKeys.MEDIA_TYPE_XYAML.toString().equals(file.getContentType())) {
            throw new IllegalArgumentException("invalid file");
        }

        try {
            List<ClientApp> apps = new ArrayList<>();
            boolean multiple = false;

            // read as raw yaml to check if collection
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(file.getInputStream());
            multiple = obj.containsKey("clients");

            if (multiple) {
                Map<String, List<ClientApp>> list = yamlObjectMapper.readValue(file.getInputStream(), typeRef);
                for (ClientApp app : list.get("clients")) {
                    app.setRealm(realm);
                    if (reset) {
                        // reset clientId
                        app.setClientId(null);
                    }

                    ClientApp clientApp = clientManager.registerClientApp(realm, app);

                    try {
                        // fetch also configuration schema
                        JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientApp.getClientId());
                        clientApp.setSchema(schema);
                    } catch (NoSuchClientException e) {
                    }

                    apps.add(clientApp);
                }

            } else {
                // try single element
                ClientApp app = yamlObjectMapper.readValue(file.getInputStream(), ClientApp.class);
                app.setRealm(realm);
                if (reset) {
                    // reset clientId
                    app.setClientId(null);
                }

                ClientApp clientApp = clientManager.registerClientApp(realm, app);

                try {
                    // fetch also configuration schema
                    JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientApp.getClientId());
                    clientApp.setSchema(schema);
                } catch (NoSuchClientException e) {
                }

                apps.add(clientApp);
            }

            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }
            throw new RegistrationException(e.getMessage());
        }
    }

    @PutMapping("/realms/{realm}/apps/{clientId}")
    public ResponseEntity<ClientApp> updateRealmClientApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid @NotNull ClientApp app)
            throws NoSuchRealmException, NoSuchClientException, SystemException {

        ClientApp clientApp = clientManager.updateClientApp(realm, clientId, app);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientId);
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);
    }

    @DeleteMapping("/realms/{realm}/apps/{clientId}")
    public ResponseEntity<Void> deleteRealmClientApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException, SystemException {
        clientManager.deleteClientApp(realm, clientId);
        return ResponseEntity.ok(null);
    }

    @PutMapping("/realms/{realm}/apps/{clientId}/credentials/{credentialsId}")
    public ResponseEntity<ClientApp> resetRealmClientAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {

        clientManager.resetClientCredentials(realm, clientId, credentialsId);

        // re-read app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientId);
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);
    }

    @DeleteMapping("/realms/{realm}/apps/{clientId}/credentials/{credentialsId}")
    public ResponseEntity<ClientApp> removeRealmClientAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {

        clientManager.removeClientCredentials(realm, clientId, credentialsId);

        // re-read app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // fetch also configuration schema
        JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientId);
        clientApp.setSchema(schema);

        return ResponseEntity.ok(clientApp);
    }

    @GetMapping("/realms/{realm}/apps/{clientId}/oauth2/{grantType}")
    public ResponseEntity<OAuth2AccessToken> testRealmClientAppOAuth2(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
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

    @PostMapping("/realms/{realm}/apps/{clientId}/claims")
    public ResponseEntity<FunctionValidationBean> testRealmClientAppClaims(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid @NotNull FunctionValidationBean function)
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

    @GetMapping("/realms/{realm}/apps/{clientId}/export")
    public void exportRealmClientApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
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
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

    @GetMapping("/realms/{realm}/apps/{clientId}/schema")
    public ResponseEntity<JsonSchema> getAppConfigurationSchema(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {

        return ResponseEntity.ok(clientManager.getClientConfigurationSchema(realm, clientId));
    }

    @GetMapping("/realms/{realm}/apps/{clientId}/providers")
    public ResponseEntity<Collection<ConfigurableIdentityProvider>> getAppProviders(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {

        // get client app
//        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // TODO replace with registration bean when implemented
        return ResponseEntity.ok(clientManager.listIdentityProviders(realm));
    }

    @GetMapping("/realms/{realm}/apps/{clientId}/authorities")
    public ResponseEntity<Collection<GrantedAuthority>> getRealmAppAuthorities(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException {
        Collection<GrantedAuthority> authorities = clientManager.getAuthorities(realm, clientId);
        return ResponseEntity.ok(authorities);
    }

    @PutMapping("/realms/{realm}/apps/{clientId}/authorities")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
            + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
    public ResponseEntity<Collection<GrantedAuthority>> updateRealmAppAuthorities(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid @NotNull Collection<RealmGrantedAuthority> roles)
            throws NoSuchRealmException, NoSuchClientException {
        // filter roles, make sure they belong to the current realm
        Set<String> values = roles.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .map(a -> a.getRole())
                .collect(Collectors.toSet());

        Collection<GrantedAuthority> authorities = clientManager.setAuthorities(realm, clientId, values);

        return ResponseEntity.ok(authorities);
    }

    /*
     * Roles
     */

    @GetMapping("/realms/{realm}/apps/{clientId}/roles")
    public ResponseEntity<Collection<RealmRole>> getRealmClientAppRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException {
        try {
            Collection<RealmRole> roles = roleManager.getSubjectRoles(realm, clientId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }

    }

    @PutMapping("/realms/{realm}/apps/{clientId}/roles")
    public ResponseEntity<Collection<RealmRole>> updateRealmClientAppRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid @NotNull Collection<RealmRole> roles)
            throws NoSuchRealmException, NoSuchClientException {
        // filter roles, make sure they belong to the current realm
        Set<RealmRole> values = roles.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .collect(Collectors.toSet());

        try {
            Collection<RealmRole> result = roleManager.setSubjectRoles(realm, clientId,
                    values);
            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    @GetMapping("/realms/{realm}/apps/{clientId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> getRealmClientAppSpaceRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException {
        try {
            Collection<SpaceRole> roles = spaceRoleManager.getRoles(clientId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    @PutMapping("/realms/{realm}/apps/{clientId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> updateRealmClientAppSpaceRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid @NotNull Collection<String> roles)
            throws NoSuchRealmException, NoSuchClientException {
        try {
            Set<SpaceRole> spaceRoles = roles.stream().map(r -> SpaceRole.parse(r)).collect(Collectors.toSet());
            Collection<SpaceRole> result = spaceRoleManager.setRoles(clientId, spaceRoles);

            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    /*
     * Service approvals (permissions)
     */

    @GetMapping("/realms/{realm}/apps/{clientId}/approvals")
    public ResponseEntity<Collection<Approval>> getRealmUserApprovals(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException {
        Collection<Approval> approvals = clientManager.getApprovals(realm, clientId);
        return ResponseEntity.ok(approvals);
    }

    /*
     * Audit
     */
    @GetMapping("/realms/{realm}/apps/{clientId}/audit")
    public ResponseEntity<Collection<AuditEvent>> getRealmUserAudit(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
            throws NoSuchRealmException, NoSuchClientException {
        Collection<AuditEvent> result = clientManager.getAudit(realm, clientId, after.orElse(null),
                before.orElse(null));
        return ResponseEntity.ok(result);
    }

}
