package it.smartcommunitylab.aac.services;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.services.model.ApiService;
import it.smartcommunitylab.aac.services.model.ApiServiceClaimDefinition;
import it.smartcommunitylab.aac.services.model.ApiServiceScope;
import it.smartcommunitylab.aac.services.provider.ApiServiceResourceProviderConfig;
import it.smartcommunitylab.aac.services.service.ServicesService;

/*
 * Manage services and their integration.
 * Also handles permissions, by comparing roles in realm
 */

@Component
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_DEVELOPER + "')")
public class ServicesManager implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ServicesService servicesService;

    @Autowired
    private RealmService realmService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ApiServiceResourceAuthority serviceAuthority;

    @Autowired
    private ProviderConfigRepository<ApiServiceResourceProviderConfig> registrationRepository;

    @Autowired
    private SearchableApprovalStore approvalStore;

    public ServicesManager() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(servicesService, "services service is required");
        Assert.notNull(serviceAuthority, "services authority is required");
        Assert.notNull(registrationRepository, "registration repository is required");

        init();
    }

    public void init() {
        // export all resource providers to repository
        // TODO move to bootstrap
        List<ApiService> services = servicesService.listServices();
        for (ApiService service : services) {
            String serviceId = service.getServiceId();

            // we load from db so version is zero, let's check
            // note: we load only missing configs to avoid a cascade effect on
            // multi-instance startup
            if (registrationRepository.findByProviderId(serviceId) != null) {
                // skip loading
                continue;
            }

            // build config
            ApiServiceResourceProviderConfig providerConfig = new ApiServiceResourceProviderConfig(service);

            // register
            registrationRepository.addRegistration(providerConfig);
        }
    }

    /*
     * Services
     * 
     */
    public ApiService findService(String realm, String serviceId) {
        logger.debug("find service {} for realm {}", StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // not supported
            return null;
        }

        ApiService service = servicesService.findService(serviceId);

        if (service == null) {
            return null;
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // note: find returns a bare model
        return service;
    }

    public ApiService getService(String realm, String serviceId) throws NoSuchServiceException, NoSuchRealmException {
        logger.debug("get service {} for realm {}", StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // not supported
            return null;
        }

        Realm re = realmService.getRealm(realm);

        ApiService service = servicesService.getService(serviceId);
        if (!re.getSlug().equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        return service;
    }

    public List<ApiService> listServices(String realm) throws NoSuchRealmException {
        logger.debug("list services for realm {}", StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // not supported
            return Collections.emptyList();
        }

        Realm re = realmService.getRealm(realm);
        return servicesService.listServices(re.getSlug());
    }

    public ApiService addService(String realm, ApiService reg) throws NoSuchRealmException, RegistrationException {
        logger.debug("add service to realm {}", StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("service bean: {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        // fetch realm
        Realm re = realmService.getRealm(realm);

        // fetch and validate id if provided
        String serviceId = reg.getServiceId();

        if (serviceId != null && !serviceId.matches(SystemKeys.SLUG_PATTERN)) {
            throw new RegistrationException("invalid service id");
        }

        // add, will process related
        ApiService service = servicesService.addService(re.getSlug(), serviceId, reg);

        // always register the resource
        // build provider config
        ApiServiceResourceProviderConfig providerConfig = new ApiServiceResourceProviderConfig(service);

        // register and force reload
        serviceAuthority.registerProvider(providerConfig);

        return service;
    }

    public ApiService updateService(String realm, String serviceId, ApiService reg)
            throws NoSuchServiceException, NoSuchRealmException, RegistrationException {
        logger.debug("update service {} for realm {}", StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("service bean: {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        // fetch realm
        Realm re = realmService.getRealm(realm);

        // load bare
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!re.getSlug().equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // update service, will process related
        service = servicesService.updateService(serviceId, reg);

        // refresh service
        refreshService(serviceId);

        return service;
    }

    public void deleteService(String realm, String serviceId) throws NoSuchServiceException {
        logger.debug("delete service {} for realm {}", StringUtils.trimAllWhitespace(serviceId),
                StringUtils.trimAllWhitespace(realm));

        ApiService service = servicesService.getService(serviceId);
        if (service != null) {
            if (!realm.equals(service.getRealm())) {
                throw new IllegalArgumentException("realm-mismatch");
            }

            // always unregister
            serviceAuthority.unregisterProvider(serviceId);

            // TODO remove tokens with this audience?
            // TODO invalidate tokens with this scopes?

            // remove, will cleanup related entities
            servicesService.deleteService(serviceId);
        }
    }

    private void refreshService(String serviceId) throws NoSuchServiceException {
        try {
            // refresh service
            ApiService service = servicesService.getService(serviceId);

            // always register the resource
            // build provider config
            ApiServiceResourceProviderConfig providerConfig = new ApiServiceResourceProviderConfig(service);

            // register and force reload
            serviceAuthority.registerProvider(providerConfig);
        } catch (RegistrationException e) {
            logger.error("error refreshing service {}: {}", serviceId, e.getMessage());
        }
    }

    /*
     * Scopes
     */
    public List<ApiServiceScope> listServiceScopes(String realm, String serviceId) throws NoSuchServiceException {
        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        return servicesService.listScopes(serviceId);
    }

    public ApiServiceScope getServiceScope(String realm, String serviceId, String scopeId)
            throws NoSuchServiceException, NoSuchScopeException {
        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        ApiServiceScope scope = servicesService.getScope(scopeId);
        if (!realm.equals(scope.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }
        if (!serviceId.equals(scope.getServiceId())) {
            throw new IllegalArgumentException("service-mismatch");
        }

        return scope;
    }

    public ApiServiceScope addServiceScope(String realm, String serviceId, ApiServiceScope reg)
            throws NoSuchServiceException, RegistrationException {

        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // fetch and validate id if provided
        String scopeId = reg.getScopeId();
        if (scopeId != null && !scopeId.matches(SystemKeys.SLUG_PATTERN)) {
            throw new RegistrationException("invalid scope id");
        }

        // add
        ApiServiceScope scope = servicesService.addScope(serviceId, scopeId, reg);

        // refresh service
        refreshService(serviceId);

        return scope;
    }

    public ApiServiceScope updateServiceScope(String realm, String serviceId, String scopeId, ApiServiceScope reg)
            throws NoSuchServiceException, NoSuchScopeException, RegistrationException {

        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        ApiServiceScope scope = servicesService.getScope(scopeId);
        if (!realm.equals(scope.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }
        if (!serviceId.equals(scope.getServiceId())) {
            throw new IllegalArgumentException("service-mismatch");
        }

        // update
        scope = servicesService.updateScope(scopeId, reg);

        // refresh service
        refreshService(serviceId);

        return scope;
    }

    public void deleteServiceScope(String realm, String serviceId, String scopeId)
            throws NoSuchServiceException, NoSuchScopeException {

        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        ApiServiceScope scope = servicesService.getScope(scopeId);
        if (!realm.equals(scope.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }
        if (!serviceId.equals(scope.getServiceId())) {
            throw new IllegalArgumentException("service-mismatch");
        }

        // TODO invalidate tokens with this scope?

        // remove approvals
        try {
            Collection<Approval> approvals = approvalStore.findScopeApprovals(scope.getScope());
            approvalStore.revokeApprovals(approvals);
        } catch (Exception e) {
        }

        // remove
        servicesService.deleteScope(scopeId);

        // refresh service
        refreshService(serviceId);
    }

    /*
     * Claims
     */

    public List<ApiServiceClaimDefinition> listServiceClaims(String realm, String serviceId)
            throws NoSuchServiceException {
        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        return servicesService.listClaims(serviceId);
    }

    public ApiServiceClaimDefinition getServiceClaim(String realm, String serviceId, String claimId)
            throws NoSuchServiceException, NoSuchClaimException {
        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        ApiServiceClaimDefinition claim = servicesService.getClaim(claimId);
        if (!realm.equals(claim.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }
        if (!serviceId.equals(claim.getServiceId())) {
            throw new IllegalArgumentException("service-mismatch");
        }

        return claim;
    }

    public ApiServiceClaimDefinition addServiceClaim(String realm, String serviceId, ApiServiceClaimDefinition reg)
            throws NoSuchServiceException, RegistrationException {
        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // fetch and validate id if provided
        String claimId = reg.getClaimId();
        if (claimId != null && !claimId.matches(SystemKeys.SLUG_PATTERN)) {
            throw new RegistrationException("invalid claim id");
        }

        // add
        ApiServiceClaimDefinition claim = servicesService.addClaim(serviceId, claimId, reg);

        // refresh service
        refreshService(serviceId);

        return claim;
    }

    public ApiServiceClaimDefinition updateServiceClaim(String realm, String serviceId, String claimId,
            ApiServiceClaimDefinition reg) throws NoSuchServiceException, NoSuchClaimException, RegistrationException {

        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        ApiServiceClaimDefinition claim = servicesService.getClaim(claimId);
        if (!realm.equals(claim.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }
        if (!serviceId.equals(claim.getServiceId())) {
            throw new IllegalArgumentException("service-mismatch");
        }

        // update
        claim = servicesService.updateClaim(claimId, reg);

        // refresh service
        refreshService(serviceId);

        return claim;
    }

    public void deleteServiceClaim(String realm, String serviceId, String claimId)
            throws NoSuchServiceException, NoSuchClaimException {

        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        ApiServiceClaimDefinition claim = servicesService.getClaim(claimId);
        if (!realm.equals(claim.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }
        if (!serviceId.equals(claim.getServiceId())) {
            throw new IllegalArgumentException("service-mismatch");
        }

        // we leave current tokens with the claim populated
        // remove only entity
        servicesService.deleteClaim(claimId);

        // refresh service
        refreshService(serviceId);
    }

    /*
     * Scope Approvals
     */

    public Collection<Approval> getServiceScopeApprovals(String realm, String serviceId, String scopeId)
            throws NoSuchServiceException, NoSuchScopeException {
        ApiServiceScope sc = getServiceScope(realm, serviceId, scopeId);

        // we use serviceId as id
        String resourceId = serviceId;

        // fetch approvals from store, serviceId is stored in userId
        return approvalStore.findUserScopeApprovals(resourceId, sc.getScope());

    }

    public Collection<Approval> getServiceApprovals(String realm, String serviceId) throws NoSuchServiceException {
        // use finder to avoid loading all related
        ApiService service = servicesService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // we use serviceId as id
        String resourceId = serviceId;
        return approvalStore.findUserApprovals(resourceId);
    }

    public Approval addServiceScopeApproval(String realm, String serviceId, String scopeId, String subjectId,
            int duration, boolean approved)
            throws NoSuchServiceException, NoSuchScopeException {

        ApiServiceScope sc = getServiceScope(realm, serviceId, scopeId);
        String scope = sc.getScope();

        // check if subject exists
        Subject sub = subjectService.findSubject(subjectId);
        if (sub == null) {
            throw new IllegalArgumentException("invalid subjectId");
        }

        // add approval to store, will refresh if present
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, duration);
        Date expiresAt = calendar.getTime();
        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        // we use serviceId as id
        String resourceId = serviceId;

        Approval approval = new Approval(resourceId, subjectId, scope, expiresAt, approvalStatus);
        approvalStore.addApprovals(Collections.singleton(approval));

        return approvalStore.findApproval(serviceId, subjectId, scope);

    }

    public void revokeServiceScopeApproval(String realm, String serviceId, String scopeId, String subjectId)
            throws NoSuchServiceException, NoSuchScopeException {

        ApiServiceScope sc = getServiceScope(realm, serviceId, scopeId);

        // we use serviceId as id
        String resourceId = serviceId;

        Approval approval = approvalStore.findApproval(resourceId, subjectId, sc.getScope());
        if (approval != null) {
            approvalStore.revokeApprovals(Collections.singleton(approval));
        }
    }

    public Boolean checkServiceNamespace(String realm, String serviceNamespace) {
        return servicesService.findServiceByNamespace(realm, serviceNamespace.toLowerCase()) != null;
    }

    public Boolean checkServiceResource(String realm, String resource) {
        return servicesService.findServiceByResource(realm, resource) != null;
    }
}
