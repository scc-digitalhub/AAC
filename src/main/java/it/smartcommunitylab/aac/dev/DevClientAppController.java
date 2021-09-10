package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
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

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.model.ClientApp;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
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
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * ClientApps
     */
    @GetMapping("/realms/{realm}/apps")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ClientApp>> getRealmClientApps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(clientManager.listClientApps(realm));
    }

    @GetMapping("/realms/{realm}/apps/search")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Page<ClientApp>> searchRealmClientApps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false) String q, Pageable pageRequest)
            throws NoSuchRealmException {
        return ResponseEntity.ok(clientManager.searchClientApps(realm, q, pageRequest));
    }

    @GetMapping("/realms/{realm}/apps/{clientId:.*}")
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

    @PostMapping("/realms/{realm}/apps")
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

    @PutMapping("/realms/{realm}/apps")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ClientApp>> importRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
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

//        Path path = null;

        try {
            List<ClientApp> apps = new ArrayList<>();
            boolean multiple = false;

//            // save as temp
//            try (InputStream is = file.getInputStream()) {
//                path = Files.createTempFile(temp, "client-", ".yaml");
//                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
//            }

            // read as raw yaml to check if collection
            Yaml yaml = new Yaml();
//            try (InputStream is = Files.newInputStream(path)) {
            Map<String, Object> obj = yaml.load(file.getInputStream());
            multiple = obj.containsKey("clients");
//            }

//            try (InputStream is = Files.newInputStream(path)) {
            if (multiple) {
                Map<String, List<ClientApp>> list = yamlObjectMapper.readValue(file.getInputStream(), typeRef);
                for (ClientApp app : list.get("clients")) {
                    app.setRealm(realm);
                    if (reset) {
                        // reset clientId
                        app.setClientId(null);
                    }

                    ClientApp clientApp = clientManager.registerClientApp(realm, app);

                    // fetch also configuration schema
                    JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
                    clientApp.setSchema(schema);

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

                // fetch also configuration schema
                JsonSchema schema = clientManager.getConfigurationSchema(clientApp.getType());
                clientApp.setSchema(schema);

                apps.add(clientApp);
            }
//            }

            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }
            throw new RegistrationException(e.getMessage());
//        } finally {
//            if (path != null) {
//                // cleanup temp
//                try {
//                    Files.delete(path);
//                } catch (Exception e) {
//                    logger.error("Error removing temp file " + path.toAbsolutePath());
//                }
//            }
        }

    }

    @PutMapping("/realms/{realm}/apps/{clientId:.*}")
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

    @DeleteMapping("/realms/{realm}/apps/{clientId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteRealmClientApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchClientException, SystemException {
        clientManager.deleteClientApp(realm, clientId);
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/realms/{realm}/apps/{clientId:.*}/credentials")
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

    @GetMapping("/realms/{realm}/apps/{clientId:.*}/oauth2/{grantType}")
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

    @PostMapping("/realms/{realm}/apps/{clientId:.*}/claims")
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

    @GetMapping("/realms/{realm}/apps/{clientId:.*}/export")
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

//    @Override
//    public void afterPropertiesSet() throws Exception {
//        // make sure temp folder is ready
//        ApplicationTemp at = new ApplicationTemp(ClientApp.class);
//        temp = Paths.get(at.getDir().getAbsolutePath());
//
//    }

}
