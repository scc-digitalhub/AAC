package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServiceClaim;
import it.smartcommunitylab.aac.services.ServiceClient;
import it.smartcommunitylab.aac.services.ServiceScope;
import it.smartcommunitylab.aac.services.ServicesManager;

/*
 * Base controller for custom services
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseServicesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ServicesManager serviceManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Services
     * 
     * When provided with a fully populated service model, related entities will be
     * updated accordingly.
     */

    @GetMapping("/service/{realm}")
    public Collection<Service> listServices(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list services for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServices(realm);
    }

    @GetMapping("/service/{realm}/{serviceId}")
    public Service getService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("get service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.getService(realm, serviceId);
    }

    @PostMapping("/service/{realm}")
    public Service addService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull Service s) throws NoSuchRealmException {
        logger.debug("add service for realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("service bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.addService(realm, s);
    }

    @PutMapping("/service/{realm}/{serviceId}")
    public Service updateService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull Service s) throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("update service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("service bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.updateService(realm, serviceId, s);
    }

    @DeleteMapping("/service/{realm}/{serviceId}")
    public void deleteService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException {
        logger.debug("delete service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        serviceManager.deleteService(realm, serviceId);
    }

    @PutMapping("/service/{realm}")
    public Service importService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import service to realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() == null) {
            throw new IllegalArgumentException("invalid file");
        }

        if (!SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType())
                && !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType())) {
            throw new IllegalArgumentException("invalid file");
        }

        try {
            Service s = yamlObjectMapper.readValue(file.getInputStream(),
                    Service.class);

            if (logger.isTraceEnabled()) {
                logger.trace("service bean: " + StringUtils.trimAllWhitespace(s.toString()));
            }

            return serviceManager.addService(realm, s);

        } catch (Exception e) {
            logger.error("import service error: " + e.getMessage());
            throw e;
        }

    }

    /*
     * Service scopes
     */

    @GetMapping("/service/{realm}/{serviceId}/scopes")
    public Collection<ServiceScope> listServiceScopes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException {
        logger.debug("list scopes from service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServiceScopes(realm, serviceId);
    }

    @GetMapping("/service/{realm}/{serviceId}/scopes/{scope}")
    public ServiceScope getServiceScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchScopeException, NoSuchRealmException, NoSuchServiceException {
        logger.debug("get scope {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(scope), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        return serviceManager.getServiceScope(realm, serviceId, scope);
    }

    @PostMapping("/service/{realm}/{serviceId}/scopes")
    public ServiceScope addServiceScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull ServiceScope s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add scope to service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("scope bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.addServiceScope(realm, serviceId, s);
    }

    @PutMapping("/service/{realm}/{serviceId}/scopes/{scope}")
    public ServiceScope updateServiceScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestBody @Valid @NotNull ServiceScope s)
            throws NoSuchScopeException, NoSuchServiceException, RegistrationException {
        logger.debug("update scope {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(scope), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("scope bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.updateServiceScope(realm, serviceId, scope, s);
    }

    @DeleteMapping("/service/{realm}/{serviceId}/scopes/{scope}")
    public void deleteServiceScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchScopeException, NoSuchServiceException {
        logger.debug("delete scope {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(scope), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        serviceManager.deleteServiceScope(realm, serviceId, scope);
    }

    /*
     * Service claims
     */
    @GetMapping("/service/{realm}/{serviceId}/claims")
    public Collection<ServiceClaim> listServiceClaims(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException {
        logger.debug("list claims from service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServiceClaims(realm, serviceId);
    }

    @GetMapping("/service/{realm}/{serviceId}/claims/{key}")
    public ServiceClaim getServiceClaim(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.KEY_PATTERN) String key)
            throws NoSuchClaimException, NoSuchRealmException, NoSuchServiceException {
        logger.debug("get claim {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(key), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        return serviceManager.getServiceClaim(realm, serviceId, key);
    }

    @PostMapping("/service/{realm}/{serviceId}/claims")
    public ServiceClaim addServiceClaim(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull ServiceClaim s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add claim to service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("claim bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.addServiceClaim(realm, serviceId, s);
    }

    @PutMapping("/service/{realm}/{serviceId}/claims/{key}")
    public ServiceClaim updateServiceClaim(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.KEY_PATTERN) String key,
            @RequestBody @Valid @NotNull ServiceClaim s)
            throws NoSuchClaimException, NoSuchServiceException, RegistrationException {
        logger.debug("update claim {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(key), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("claim bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.updateServiceClaim(realm, serviceId, key, s);
    }

    @DeleteMapping("/service/{realm}/{serviceId}/claims/{key}")
    public void deleteServiceClaim(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.KEY_PATTERN) String key)
            throws NoSuchClaimException, NoSuchServiceException {
        logger.debug("delete claim {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(key), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        serviceManager.deleteServiceClaim(realm, serviceId, key);
    }

    /*
     * Service scope approvals
     */

    @GetMapping("/service/{realm}/{serviceId}/scopes/{scope}/approvals")
    public Collection<Approval> getServiceScopeApprovals(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        logger.debug("list approvals from service {}for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.getServiceScopeApprovals(realm, serviceId, scope);
    }

    @PostMapping("/service/{realm}/{serviceId}/scopes/{scope}/approvals")
    public Approval addServiceScopeApproval(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestParam @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestParam(required = false, defaultValue = "true") boolean approved)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        logger.debug("add approval for scope {} client {} to service {} for realm {}",
                StringUtils.trimAllWhitespace(scope), StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        int duration = SystemKeys.DEFAULT_APPROVAL_VALIDITY;
        return serviceManager.addServiceScopeApproval(realm, serviceId, scope, clientId, duration, approved);
    }

    @DeleteMapping("/service/{realm}/{serviceId}/scopes/{scope}/approvals")
    public void deleteServiceScopeApproval(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestParam @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchScopeException, NoSuchServiceException {
        logger.debug("revoke approval for scope {} client {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(scope), StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        serviceManager.revokeServiceScopeApproval(realm, serviceId, scope, clientId);
    }

    /*
     * Service client
     */
    @GetMapping("/service/{realm}/{serviceId}/clients")
    public Collection<ServiceClient> listServiceClients(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("list clients from service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServiceClients(realm, serviceId);
    }

    @GetMapping("/service/{realm}/{serviceId}/clients/{clientId}")
    public ServiceClient getServiceClients(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchServiceException, NoSuchRealmException, NoSuchClientException {
        logger.debug("get service client {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        return serviceManager.getServiceClient(realm, serviceId, clientId);
    }

    @PostMapping("/service/{realm}/{serviceId}/clients")
    public ServiceClient addServiceClient(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull ServiceClient s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add client to service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("client bean " + StringUtils.trimAllWhitespace(s.toString()));
        }
        String type = s.getType();
        return serviceManager.addServiceClient(realm, serviceId, type);
    }

    @DeleteMapping("/service/{realm}/{serviceId}/clients/{clientId}")
    public void deleteServiceClient(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchServiceException, NoSuchRealmException, NoSuchClientException {
        logger.debug("delete service client {} from service {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        serviceManager.deleteServiceClient(realm, serviceId, clientId);
    }

}
