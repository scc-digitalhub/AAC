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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AttributeProviderManager;
import it.smartcommunitylab.aac.clients.ClientManager;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.SubjectManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.dto.UserSubject;
import it.smartcommunitylab.aac.identity.IdentityProviderManager;
import it.smartcommunitylab.aac.model.Developer;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.realms.RealmManager;
import it.smartcommunitylab.aac.services.ServicesManager;
import it.smartcommunitylab.aac.templates.TemplatesManager;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.service.LanguageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Hidden
@RequestMapping("/console/dev")
public class DevRealmController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private IdentityProviderManager identityProviderManager;

    @Autowired
    private AttributeProviderManager attributeProviderManager;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private ServicesManager serviceManager;

    @Autowired
    private SubjectManager subjectManager;

    @Autowired
    private TemplatesManager templatesManager;

    //    @Autowired
    //    private AuditManager auditManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @GetMapping("/realms")
    public Collection<Realm> myRealms(@NotNull UserAuthentication userAuth) throws NoSuchRealmException {
        UserDetails user = userAuth.getUser();
        Collection<Realm> realms = user
            .getRealms()
            .stream()
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
            // system admin can access all realms
            realms = realmManager.listRealms();
        }

        return realms;
    }

    @GetMapping("/realms/{realm}")
    @PreAuthorize(
        "hasAuthority('" +
        Config.R_ADMIN +
        "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')"
    )
    public Realm getRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
        throws NoSuchRealmException {
        return realmManager.getRealm(realm);
    }

    @PutMapping("/realms/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Realm updateRealm(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull Realm r
    ) throws NoSuchRealmException, RegistrationException {
        return realmManager.updateRealm(realm, r);
    }

    @DeleteMapping("/realms/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void deleteRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
        throws NoSuchRealmException {
        realmManager.deleteRealm(realm, true);
    }

    @GetMapping("/realms/{realm}/export")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void exportRealm(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false, defaultValue = "false") boolean config,
        HttpServletResponse res
    ) throws NoSuchRealmException, SystemException, IOException {
        Realm r = realmManager.getRealm(realm);
        Object export = r;
        String key = r.getSlug();

        if (config) {
            key = r.getSlug() + "-config";
            RealmConfig rc = realmManager.getRealmConfig(realm);
            export = rc;
        }

        String s = yamlObjectMapper.writeValueAsString(export);

        // write as file
        res.setContentType(SystemKeys.MEDIA_TYPE_APPLICATION_YAML_VALUE);
        res.setHeader("Content-Disposition", "attachment;filename=realm-" + key + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

    //    /*
    //     * Audit events
    //     */
    //
    //    @GetMapping("/realms/{realm}/audit")
    //    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    //    public Collection<RealmAuditEvent>> findEvents(
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
    //            @RequestParam(required = false, name = "type") Optional<String> type,
    //            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
    //            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
    //            throws NoSuchRealmException {
    //
    //        return ResponseEntity
    //                .ok(auditManager.findRealmEvents(realm,
    //                        type.orElse(null), after.orElse(null), before.orElse(null)));
    //
    //    }

    /*
     * Dev console users
     */
    @GetMapping("/realms/{realm}/developers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<Developer> getDevelopers(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchRealmException {
        return realmManager.getDevelopers(realm);
    }

    @PostMapping("/realms/{realm}/developers")
    public Developer inviteDeveloper(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull UserSubject bean
    ) throws NoSuchRealmException, NoSuchUserException, RegistrationException {
        return realmManager.inviteDeveloper(realm, bean.getSubjectId(), bean.getEmail());
    }

    @PutMapping("/realms/{realm}/developers/{subjectId}")
    public Developer updateDeveloper(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
        @RequestBody @Valid @NotNull Collection<String> roles
    ) throws NoSuchRealmException, NoSuchUserException {
        return realmManager.updateDeveloper(realm, subjectId, roles);
    }

    @DeleteMapping("/realms/{realm}/developers/{subjectId}")
    public void removeDeveloper(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchUserException {
        realmManager.removeDeveloper(realm, subjectId);
    }

    /*
     * Realm subjects
     */
    @GetMapping("/realms/{realm}/subjects")
    public Collection<Subject> getRealmSubjects(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String t
    ) throws NoSuchRealmException {
        Set<String> types = StringUtils.hasText(t) ? StringUtils.commaDelimitedListToSet(t) : null;
        return subjectManager.searchSubjects(realm, q, types);
    }

    @GetMapping("/realms/{realm}/subjects/{subjectId}")
    public Subject getRealmSubject(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchSubjectException {
        return subjectManager.getSubject(realm, subjectId);
    }

    /*
     * Realm templates / i18n config
     */
    @GetMapping("/realms/{realm}/languages")
    public Collection<String> getLanguages(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchProviderException, NoSuchRealmException {
        // return languages available system-wide
        return Arrays.asList(LanguageService.LANGUAGES);
    }

    @GetMapping("/realms/{realm}/templates/conf")
    public ConfigurableTemplateProvider getTemplateProviderConfig(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchProviderException, NoSuchRealmException, RegistrationException {
        // single config per realm
        return templatesManager.getProviderByRealm(realm);
    }

    @PutMapping("/realms/{realm}/templates/conf")
    public ConfigurableTemplateProvider setTemplateProviderConfig(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull ConfigurableTemplateProvider config
    ) throws NoSuchProviderException, NoSuchRealmException, RegistrationException, NoSuchAuthorityException {
        // single config per realm, so either add as new or update
        ConfigurableTemplateProvider cp = templatesManager.findProviderByRealm(realm);
        if (cp == null) {
            // add as new
            return templatesManager.addProvider(realm, config);
        } else {
            config.setProvider(cp.getProvider());
            return templatesManager.updateProvider(realm, cp.getProvider(), config);
        }
    }
}
