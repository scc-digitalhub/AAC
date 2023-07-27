/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.console;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.controller.BaseClientAppController;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;
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
import org.springframework.util.StringUtils;
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

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevClientAppController extends BaseClientAppController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<ClientApp>>> typeRef =
        new TypeReference<Map<String, List<ClientApp>>>() {};
    private final String LIST_KEY = "clients";

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

    @GetMapping("/apps/{realm}/search")
    public Page<ClientApp> searchClientApps(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false) String q,
        Pageable pageRequest
    ) throws NoSuchRealmException {
        return clientManager.searchClientApps(realm, q, pageRequest);
    }

    @PutMapping("/apps/{realm}")
    public ResponseEntity<Collection<ClientApp>> importClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false, defaultValue = "false") boolean reset,
        @RequestPart(name = "yaml", required = false) @Valid String yaml,
        @RequestPart(name = "file", required = false) @Valid MultipartFile file
    ) throws NoSuchRealmException, RegistrationException {
        logger.debug("import client(s) to realm {}", StringUtils.trimAllWhitespace(realm));

        if (!StringUtils.hasText(yaml) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("empty file or yaml");
        }

        try {
            // read string, fallback to yaml
            if (!StringUtils.hasText(yaml)) {
                if (file.getContentType() == null) {
                    throw new IllegalArgumentException("invalid file");
                }

                if (
                    !SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType()) &&
                    !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType()) &&
                    !SystemKeys.MEDIA_TYPE_XYAML.toString().equals(file.getContentType())
                ) {
                    throw new IllegalArgumentException("invalid file");
                }

                // read whole file as string
                yaml = new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            List<ClientApp> apps = new ArrayList<>();
            List<ClientApp> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<ClientApp>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (ClientApp app : list.get(LIST_KEY)) {
                    regs.add(app);
                }
            } else {
                // try single element
                ClientApp app = yamlObjectMapper.readValue(yaml, ClientApp.class);
                regs.add(app);
            }

            // register all parsed apps
            for (ClientApp reg : regs) {
                reg.setRealm(realm);
                if (reset) {
                    // reset clientId
                    reg.setClientId(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("client bean: {}", String.valueOf(reg));
                }

                ClientApp clientApp = clientManager.registerClientApp(realm, reg);
                //                try {
                //                    // fetch also configuration schema
                //                    JsonSchema schema = clientManager.getClientConfigurationSchema(realm, clientApp.getClientId());
                //                    clientApp.setSchema(schema);
                //                } catch (NoSuchClientException | NoSuchRealmException e) {
                //                    // skip
                //                }

                apps.add(clientApp);
            }

            return ResponseEntity.ok(apps);
        } catch (RuntimeException | IOException e) {
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

            throw new RegistrationException(e.getMessage());
        }
    }

    @GetMapping("/apps/{realm}/{clientId}/export")
    public void exportClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        HttpServletResponse res
    ) throws NoSuchRealmException, NoSuchClientException, SystemException, IOException {
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

    /*
     * Test app
     */

    @GetMapping("/apps/{realm}/{clientId}/oauth2/{grantType}")
    public ResponseEntity<OAuth2AccessToken> testClientAppOAuth2(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @PathVariable String grantType
    ) throws NoSuchRealmException, NoSuchClientException, SystemException {
        // get client app
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // check if oauth2
        if (!clientApp.getType().equals(SystemKeys.CLIENT_TYPE_OAUTH2)) {
            throw new IllegalArgumentException("client does not support oauth2");
        }

        OAuth2AccessToken accessToken = devManager.testOAuth2Flow(realm, clientId, grantType);

        return ResponseEntity.ok(accessToken);
    }

    @PostMapping("/apps/{realm}/{clientId}/claims")
    public ResponseEntity<FunctionValidationBean> testClientAppClaims(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestBody @Valid @NotNull FunctionValidationBean function
    )
        throws NoSuchRealmException, NoSuchClientException, SystemException, NoSuchResourceException, InvalidDefinitionException {
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

    /*
     * Providers
     */

    @GetMapping("/apps/{realm}/{clientId}/providers")
    public ResponseEntity<Collection<ConfigurableIdentityProvider>> getAppProviders(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchClientException, NoSuchRealmException {
        // get client app
        //        ClientApp clientApp = clientManager.getClientApp(realm, clientId);

        // TODO replace with registration bean when implemented
        return ResponseEntity.ok(clientManager.listIdentityProviders(realm));
    }

    /*
     * Authorities
     */

    @GetMapping("/apps/{realm}/{clientId}/authorities")
    public ResponseEntity<Collection<RealmGrantedAuthority>> getRealmAppAuthorities(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchRealmException, NoSuchClientException {
        Collection<RealmGrantedAuthority> authorities = clientManager.getAuthorities(realm, clientId);
        return ResponseEntity.ok(authorities);
    }

    @PutMapping("/apps/{realm}/{clientId}/authorities")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
    public ResponseEntity<Collection<GrantedAuthority>> updateRealmAppAuthorities(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestBody @Valid @NotNull Collection<RealmGrantedAuthority> roles
    ) throws NoSuchRealmException, NoSuchClientException {
        // filter roles, make sure they belong to the current realm
        Set<String> values = roles
            .stream()
            .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
            .map(a -> a.getRole())
            .collect(Collectors.toSet());

        Collection<GrantedAuthority> authorities = clientManager.setAuthorities(realm, clientId, values);

        return ResponseEntity.ok(authorities);
    }

    /*
     * Roles
     */

    @GetMapping("/apps/{realm}/{clientId}/roles")
    public ResponseEntity<Collection<RealmRole>> getClientAppRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchRealmException, NoSuchClientException {
        try {
            Collection<RealmRole> roles = roleManager.getSubjectRoles(realm, clientId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    @PutMapping("/apps/{realm}/{clientId}/roles")
    public ResponseEntity<Collection<RealmRole>> updateClientAppRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestBody @Valid @NotNull Collection<RealmRole> roles
    ) throws NoSuchRealmException, NoSuchClientException {
        // filter roles, make sure they belong to the current realm
        Set<RealmRole> values = roles
            .stream()
            .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
            .collect(Collectors.toSet());

        try {
            Collection<RealmRole> result = roleManager.setSubjectRoles(realm, clientId, values);
            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    @GetMapping("/apps/{realm}/{clientId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> getClientAppSpaceRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchRealmException, NoSuchClientException {
        try {
            Collection<SpaceRole> roles = spaceRoleManager.getRoles(clientId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    @PutMapping("/apps/{realm}/{clientId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> updateClientAppSpaceRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestBody @Valid @NotNull Collection<String> roles
    ) throws NoSuchRealmException, NoSuchClientException {
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

    @GetMapping("/apps/{realm}/{clientId}/approvals")
    public ResponseEntity<Collection<Approval>> getClientAppApprovals(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchRealmException, NoSuchClientException {
        Collection<Approval> approvals = clientManager.getApprovals(realm, clientId);
        return ResponseEntity.ok(approvals);
    }

    /*
     * Audit
     */
    @GetMapping("/apps/{realm}/{clientId}/audit")
    public ResponseEntity<Collection<AuditEvent>> getClientAppAudit(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestParam(required = false, name = "after") @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) Optional<Date> after,
        @RequestParam(required = false, name = "before") @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) Optional<Date> before
    ) throws NoSuchRealmException, NoSuchClientException {
        Collection<AuditEvent> result = clientManager.getAudit(
            realm,
            clientId,
            after.orElse(null),
            before.orElse(null)
        );
        return ResponseEntity.ok(result);
    }
}
