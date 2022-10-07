package it.smartcommunitylab.aac.services;

import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;

/*
 * Base controller for custom services
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseServicesController implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ServicesManager serviceManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(serviceManager, "service manager is required");
    }

    @Autowired
    public void setServiceManager(ServicesManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Services
     * 
     * When provided with a fully populated service model, related entities will be
     * updated accordingly.
     */

    @GetMapping("/services/{realm}")
    @Operation(summary = "list services from realm")
    public Collection<Service> listServices(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list services for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServices(realm);
    }

    @PostMapping("/services/{realm}")
    @Operation(summary = "add a new service to realm")
    public Service addService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull Service s) throws NoSuchRealmException {
        logger.debug("add service for realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("service bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.addService(realm, s);
    }

    @GetMapping("/services/{realm}/{serviceId}")
    @Operation(summary = "get a specific service from realm")
    public Service getService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("get service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.getService(realm, serviceId);
    }

    @PutMapping("/services/{realm}/{serviceId}")
    @Operation(summary = "update a specific service in realm")
    public Service updateService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull Service s) throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("update service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("service bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.updateService(realm, serviceId, s);
    }

    @DeleteMapping("/services/{realm}/{serviceId}")
    @Operation(summary = "delete a specific service from realm")
    public void deleteService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException {
        logger.debug("delete service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        serviceManager.deleteService(realm, serviceId);
    }

    /*
     * Approvals
     */
    @GetMapping("/services/{realm}/{serviceId}/approvals")
    @Operation(summary = "get approvals for a given service in realm")
    public Collection<Approval> getRealmServiceApprovals(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        logger.debug("get approvals for service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.getServiceApprovals(realm, serviceId);
    }

    /*
     * Service scopes
     */

    @GetMapping("/services/{realm}/{serviceId}/scopes")
    @Operation(summary = "get scopes for a given service in realm")
    public Collection<ServiceScope> listServiceScopes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException {
        logger.debug("list scopes from service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServiceScopes(realm, serviceId);
    }

    @PostMapping("/services/{realm}/{serviceId}/scopes")
    @Operation(summary = "add a new scope to a given service in realm")
    public ServiceScope addServiceScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull ServiceScope s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add scope to service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("scope bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.addServiceScope(realm, serviceId, s);
    }

    @GetMapping("/services/{realm}/{serviceId}/scopes/{scope}")
    @Operation(summary = "get a specific scope from a given service in realm")
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

    @PutMapping("/services/{realm}/{serviceId}/scopes/{scope}")
    @Operation(summary = "update a specific scope from a given service in realm")
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
            logger.trace("scope bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.updateServiceScope(realm, serviceId, scope, s);
    }

    @DeleteMapping("/services/{realm}/{serviceId}/scopes/{scope}")
    @Operation(summary = "delete a specific scope from a given service in realm")
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
    @GetMapping("/services/{realm}/{serviceId}/claims")
    @Operation(summary = "get claims for a given service in realm")
    public Collection<ServiceClaim> listServiceClaims(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchRealmException, NoSuchServiceException {
        logger.debug("list claims from service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServiceClaims(realm, serviceId);
    }

    @PostMapping("/services/{realm}/{serviceId}/claims")
    @Operation(summary = "add a new claim to a given service in realm")
    public ServiceClaim addServiceClaim(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull ServiceClaim s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add claim to service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("claim bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.addServiceClaim(realm, serviceId, s);
    }

    @GetMapping("/services/{realm}/{serviceId}/claims/{key}")
    @Operation(summary = "get a specific claim from a given service in realm")
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

    @PutMapping("/services/{realm}/{serviceId}/claims/{key}")
    @Operation(summary = "update a specific claim from a given service in realm")
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
            logger.trace("claim bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return serviceManager.updateServiceClaim(realm, serviceId, key, s);
    }

    @DeleteMapping("/services/{realm}/{serviceId}/claims/{key}")
    @Operation(summary = "delete a specific claim from a given service in realm")
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

    @GetMapping("/services/{realm}/{serviceId}/scopes/{scope}/approvals")
    @Operation(summary = "get approvals for a given scope from a given service in realm")
    public Collection<Approval> getServiceScopeApprovals(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchRealmException, NoSuchServiceException, NoSuchScopeException {
        logger.debug("list approvals from service {}for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.getServiceScopeApprovals(realm, serviceId, scope);
    }

    @PostMapping("/services/{realm}/{serviceId}/scopes/{scope}/approvals")
    @Operation(summary = "add a new approval for a given scope in a given service in realm")
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

    @DeleteMapping("/services/{realm}/{serviceId}/scopes/{scope}/approvals")
    @Operation(summary = "remove an approval for a given scope in a given service in realm")
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
    @GetMapping("/services/{realm}/{serviceId}/clients")
    @Operation(summary = "list client for a given service in realm")
    public Collection<ServiceClient> listServiceClients(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId)
            throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("list clients from service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        return serviceManager.listServiceClients(realm, serviceId);
    }

    @PostMapping("/services/{realm}/{serviceId}/clients")
    @Operation(summary = "add a new client for a given service in realm")
    public ServiceClient addServiceClient(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull ServiceClient s)
            throws NoSuchRealmException, NoSuchServiceException, RegistrationException {
        logger.debug("add client to service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("client bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        String type = s.getType();
        ServiceClient client = serviceManager.addServiceClient(realm, serviceId, type);

        return client;
    }

    @GetMapping("/services/{realm}/{serviceId}/clients/{clientId}")
    @Operation(summary = "get a specific client for a given service in realm")
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

    @DeleteMapping("/services/{realm}/{serviceId}/clients/{clientId}")
    @Operation(summary = "delete a specific client for a given service in realm")
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
