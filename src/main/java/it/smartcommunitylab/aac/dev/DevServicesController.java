package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServiceClaim;
import it.smartcommunitylab.aac.services.ServiceClient;
import it.smartcommunitylab.aac.services.ServiceScope;
import it.smartcommunitylab.aac.services.ServicesManager;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevServicesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ServicesManager serviceManager;

    @Autowired
    private DevManager devManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @GetMapping("/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<List<Service>> listRealmServices(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        return ResponseEntity.ok(serviceManager.listServices(realm));
    }

    @GetMapping("/realms/{realm}/services/{serviceId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> getRealmService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException {
        return ResponseEntity.ok(serviceManager.getService(realm, serviceId));
    }

    @GetMapping("/realms/{realm}/services/{serviceId}/yaml")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public void exportRealmService(
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

    @PostMapping("/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> addRealmService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid Service s)
            throws NoSuchRealmException {
        return ResponseEntity.ok(serviceManager.addService(realm, s));
    }

    @PutMapping("/realms/{realm}/services/{serviceId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> updateService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid Service s) throws NoSuchServiceException, NoSuchRealmException {
        return ResponseEntity.ok(serviceManager.updateService(realm, serviceId, s));
    }

    @DeleteMapping("/realms/{realm}/services/{serviceId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteRealmService(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException {
        serviceManager.deleteService(realm, serviceId);
        return ResponseEntity.ok(null);
    }

    @PutMapping("/realms/{realm}/services")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Service> importRealmService(
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

    @GetMapping("/realms/{realm}/nsexists")
    public ResponseEntity<Boolean> checkRealmServiceNamespace(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam String ns) throws NoSuchRealmException {
        return ResponseEntity.ok(serviceManager.checkServiceNamespace(realm, ns));
    }

    /*
     * Claims
     */
    @GetMapping("/realms/{realm}/services/{serviceId}/claims")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ServiceClaim>> getRealmServiceClaims(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.listServiceClaims(realm, serviceId));
    }

    @PostMapping("/realms/{realm}/services/{serviceId}/claims")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceClaim> addRealmServiceClaim(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid ServiceClaim s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.addServiceClaim(realm, serviceId, s));
    }

    @PutMapping("/realms/{realm}/services/{serviceId}/claims/{key}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceClaim> updateRealmServiceClaim(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.KEY_PATTERN) String key,
            @RequestBody @Valid ServiceClaim s)
            throws NoSuchClaimException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.updateServiceClaim(realm, serviceId, key, s));
    }

    @DeleteMapping("/realms/{realm}/services/{serviceId}/claims/{key}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteRealmServiceClaim(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.KEY_PATTERN) String key)
            throws NoSuchClaimException, NoSuchServiceException {
        serviceManager.deleteServiceClaim(realm, serviceId, key);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/realms/{realm}/services/{serviceId}/claims/validate")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<FunctionValidationBean> validateRealmServiceClaim(
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

    /*
     * Scopes
     */
    @GetMapping("/realms/{realm}/services/{serviceId}/scopes")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ServiceScope>> getRealmServiceScopes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.listServiceScopes(realm, serviceId));
    }

    @PostMapping("/realms/{realm}/services/{serviceId}/scopes")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceScope> addRealmServiceScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid ServiceScope s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.addServiceScope(realm, serviceId, s));
    }

    @PutMapping("/realms/{realm}/services/{serviceId}/scopes/{scope}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceScope> updateRealmServiceScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestBody @Valid ServiceScope s)
            throws NoSuchScopeException, NoSuchServiceException, RegistrationException {
        return ResponseEntity.ok(serviceManager.updateServiceScope(realm, serviceId, scope, s));
    }

    @DeleteMapping("/realms/{realm}/services/{serviceId}/scopes/{scope}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteRealmServiceScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchScopeException, NoSuchServiceException {
        serviceManager.deleteServiceScope(realm, serviceId, scope);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/realms/{realm}/services/{serviceId}/scopes/{scope}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<Approval>> getRealmServiceScopeApprovals(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        return ResponseEntity.ok(serviceManager.getServiceScopeApprovals(realm, serviceId, scope));
    }

    @PostMapping("/realms/{realm}/services/{serviceId}/scopes/{scope}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Approval> addRealmServiceScopeApproval(
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

    @DeleteMapping("/realms/{realm}/services/{serviceId}/scopes/{scope}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteRealmServiceScopeApproval(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestParam String clientId)
            throws NoSuchRealmException, NoSuchScopeException, NoSuchServiceException {
        serviceManager.revokeServiceScopeApproval(realm, serviceId, scope, clientId);
        return ResponseEntity.ok(null);
    }

    /*
     * Approvals
     */
    @GetMapping("/realms/{realm}/services/{serviceId}/approvals")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<Approval>> getRealmServiceApprovals(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        return ResponseEntity.ok(serviceManager.getServiceApprovals(realm, serviceId));
    }

    /*
     * Service client
     */

    @GetMapping("/realms/{realm}/services/{serviceId}/clients")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ServiceClient>> listServiceClients(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("list service clients " + String.valueOf(serviceId) + " for realm " + String.valueOf(realm));
        Collection<ServiceClient> clients = serviceManager.listServiceClients(realm, serviceId);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/realms/{realm}/services/{serviceId}/clients/{clientId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceClient> getServiceClients(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchServiceException, NoSuchRealmException, NoSuchClientException {
        logger.debug("list service clients " + String.valueOf(serviceId) + " for realm " + String.valueOf(realm));
        ServiceClient client = serviceManager.getServiceClient(realm, serviceId, clientId);
        return ResponseEntity.ok(client);
    }

    @PostMapping("/realms/{realm}/services/{serviceId}/clients")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ServiceClient> addServiceClient(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid ServiceClient s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add client to service " + String.valueOf(serviceId) + " for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("client bean " + String.valueOf(s));
        }
        String type = s.getType();
        ServiceClient client = serviceManager.addServiceClient(realm, serviceId, type);
        return ResponseEntity.ok(client);
    }

    @DeleteMapping("/realms/{realm}/services/{serviceId}/clients/{clientId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Void> deleteServiceClient(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchServiceException, NoSuchRealmException, NoSuchClientException {
        logger.debug("delete service client " + String.valueOf(serviceId) + " for realm " + String.valueOf(realm));
        serviceManager.deleteServiceClient(realm, serviceId, clientId);
        return ResponseEntity.ok(null);
    }
}
