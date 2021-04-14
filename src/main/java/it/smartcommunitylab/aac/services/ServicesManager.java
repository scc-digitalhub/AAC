package it.smartcommunitylab.aac.services;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.approval.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

/*
 * Manage services and their integration.
 * Also handles permissions, by comparing roles in realm
 */

@Component
public class ServicesManager implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ServicesService serviceService;

    @Autowired
    private RealmService realmService;

//    @Autowired
//    private ScopeRegistry scopeRegistry;
//
//    @Autowired
//    private ExtractorsRegistry extractorsRegistry;

    @Autowired
    private SearchableApprovalStore approvalStore;

    private ScriptServiceClaimExtractor scriptClaimExtractor;

    public ServicesManager() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // build and register the claims extractor
        scriptClaimExtractor = new ScriptServiceClaimExtractor(serviceService);
//        extractorsRegistry.registerExtractor(scriptClaimExtractor);
    }

    /*
     * Services
     * 
     */
    public Service getService(String realm, String serviceId) throws NoSuchServiceException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);

        Service service = serviceService.getService(serviceId);
        if (!re.getSlug().equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        return service;
    }

//    public Service getServiceByNamespace(String realm, String namespace) throws NoSuchServiceException {
//        return serviceService.getServiceByNamespace(namespace);
//    }

    public List<Service> listServices(String realm) throws NoSuchRealmException {
        Realm re = realmService.getRealm(realm);

        return serviceService.listServices(re.getSlug());
    }

    public Service addService(String realm, Service service) throws NoSuchRealmException {
        // fetch realm
        Realm re = realmService.getRealm(realm);

        // explode
        String namespace = service.getNamespace();
        String name = StringUtils.hasText(service.getName()) ? service.getName() : namespace;
        String description = service.getDescription();

        // add
        Service s = serviceService.addService(re.getSlug(), namespace, name, description);
        String serviceId = s.getServiceId();

        try {
            // optional
            Map<String, String> claimMapping = service.getClaimMapping();
            if (claimMapping != null && !claimMapping.isEmpty()) {

                s = serviceService.updateService(serviceId, name, description, claimMapping);

            }

            // related
            Collection<ServiceScope> scopes = service.getScopes();
            Set<ServiceScope> serviceScopes = new HashSet<>();

            if (scopes != null && !scopes.isEmpty()) {
                for (ServiceScope sc : scopes) {
                    sc.setServiceId(serviceId);
                    ServiceScope ss = addServiceScope(realm, serviceId, sc);
                    serviceScopes.add(ss);
                }
            }
            s.setScopes(serviceScopes);

            Collection<ServiceClaim> claims = service.getClaims();
            Set<ServiceClaim> serviceClaims = new HashSet<>();
            if (claims != null && !claims.isEmpty()) {
                for (ServiceClaim sc : claims) {
                    sc.setServiceId(serviceId);
                    ServiceClaim ss = addServiceClaim(realm, serviceId, sc);
                    serviceClaims.add(ss);
                }
            }
            s.setClaims(serviceClaims);

        } catch (NoSuchServiceException e) {
            // something broken
            throw new SystemException();
        }

        return s;
    }

    public Service updateService(String realm, String serviceId, Service service)
            throws NoSuchServiceException, NoSuchRealmException {
        // fetch realm
        Realm re = realmService.getRealm(realm);

        // load full
        Service ss = serviceService.getService(serviceId);

        if (!re.getSlug().equals(ss.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        if (StringUtils.hasText(service.getServiceId()) && !serviceId.equals(service.getServiceId())) {
            throw new IllegalArgumentException("serviceId does not match service");
        }

        // explode
        String namespace = service.getNamespace();
        String name = StringUtils.hasText(service.getName()) ? service.getName() : namespace;
        String description = service.getDescription();
        Map<String, String> claimMapping = service.getClaimMapping();

        // update
        Service result = serviceService.updateService(serviceId, name, description, claimMapping);
        // inflate with old data
        result.setScopes(ss.getScopes());
        result.setClaims(ss.getClaims());

        try {

            // related
            Collection<ServiceScope> scopes = service.getScopes();
            Set<ServiceScope> serviceScopes = new HashSet<>();

            if (scopes != null && !scopes.isEmpty()) {
                for (ServiceScope sc : scopes) {
                    sc.setServiceId(serviceId);

                    // check if exists or new
                    ServiceScope ssc = null;
                    if (ss.getScopes().contains(sc)) {

                        try {
                            ssc = updateServiceScope(realm, serviceId, sc.getScope(), sc);
                        } catch (RegistrationException e) {
                            // ignore
                        } catch (NoSuchScopeException e) {
                            // removed, use add
                            ssc = addServiceScope(realm, serviceId, sc);
                        }
                    } else {
                        ssc = addServiceScope(realm, serviceId, sc);
                    }

                    if (ssc != null) {
                        serviceScopes.add(ssc);
                    }
                }
            }

            // reduce and get removed scopes
            Set<ServiceScope> removedScopes = ss.getScopes().stream().filter(s -> !serviceScopes.contains(s))
                    .collect(Collectors.toSet());
            for (ServiceScope sc : removedScopes) {

                try {
                    deleteServiceScope(realm, serviceId, sc.getScope());
                } catch (NoSuchScopeException e) {
                    // already removed
                }
            }

            result.setScopes(serviceScopes);

            Collection<ServiceClaim> claims = service.getClaims();
            Set<ServiceClaim> serviceClaims = new HashSet<>();
            if (claims != null && !claims.isEmpty()) {
                for (ServiceClaim sc : claims) {
                    sc.setServiceId(serviceId);

                    // check if exists or new
                    ServiceClaim ssc = null;
                    if (ss.getClaims().contains(sc)) {
                        try {
                            ssc = updateServiceClaim(realm, serviceId, sc.getKey(), sc);
                        } catch (RegistrationException e) {
                            // ignore
                        } catch (NoSuchClaimException e) {
                            // removed, use add
                            ssc = addServiceClaim(realm, serviceId, sc);
                        }
                    } else {
                        ssc = addServiceClaim(realm, serviceId, sc);
                    }

                    if (ssc != null) {
                        serviceClaims.add(ssc);
                    }

                }
            }

            // reduce and get removed claims
            Set<ServiceClaim> removedClaims = ss.getClaims().stream().filter(s -> !serviceClaims.contains(s))
                    .collect(Collectors.toSet());
            for (ServiceClaim sc : removedClaims) {
                try {
                    deleteServiceClaim(realm, serviceId, sc.getKey());
                } catch (NoSuchClaimException e) {
                    // already removed
                }
            }

            result.setClaims(serviceClaims);

        } catch (NoSuchServiceException e) {
            // something broken
            throw new SystemException();
        }

        return result;
    }

    public void deleteService(String realm, String serviceId) throws NoSuchServiceException {
        Service service = serviceService.getService(serviceId);
        if (service != null) {
            if (!realm.equals(service.getRealm())) {
                throw new IllegalArgumentException("service does not match realm");
            }

            // TODO remove tokens with this audience?

            // cleanup scopes
            for (ServiceScope sc : service.getScopes()) {
                // unregister
//                scopeRegistry.unregisterScope(sc);

                // TODO invalidate tokens with this scope?

                // remove approvals
                try {
                    Collection<Approval> approvals = approvalStore.findScopeApprovals(sc.getScope());
                    approvalStore.revokeApprovals(approvals);
                } catch (Exception e) {
                }
            }

            // TODO unregister custom extractors when implemented

            // remove, will cleanup related entities
            serviceService.deleteService(serviceId);
        }
    }

    /*
     * Scopes
     */
    public List<ServiceScope> listServiceScopes(String realm, String serviceId) throws NoSuchServiceException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        return serviceService.listScopes(serviceId);
    }

    public ServiceScope getServiceScope(String realm, String serviceId, String scope)
            throws NoSuchServiceException, NoSuchScopeException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        return serviceService.getScope(serviceId, scope);
    }

    public ServiceScope addServiceScope(String realm, String serviceId, ServiceScope sc)
            throws NoSuchServiceException, RegistrationException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        // explode
        String scope = sc.getScope();
        String name = StringUtils.hasText(sc.getName()) ? sc.getName() : scope;
        String description = sc.getDescription();
        ScopeType type = sc.getType() != null ? sc.getType() : ScopeType.GENERIC;
        Set<String> claims = sc.getClaims();
        Set<String> roles = sc.getRoles();
        boolean approvalRequired = sc.isApprovalRequired();

        ServiceScope s = serviceService.addScope(serviceId, scope,
                name, description, type,
                claims, roles,
                approvalRequired);

        // register
//        scopeRegistry.registerScope(s);

        return s;
    }

    public ServiceScope updateServiceScope(String realm, String serviceId, String scope, ServiceScope sc)
            throws NoSuchServiceException, NoSuchScopeException {

        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        if (StringUtils.hasText(sc.getServiceId()) && !serviceId.equals(sc.getServiceId())) {
            throw new IllegalArgumentException("scope does not match service");
        }

        if (StringUtils.hasText(sc.getScope()) && !scope.equals(sc.getScope())) {
            throw new IllegalArgumentException("scope does not match");
        }

        // explode
        String name = StringUtils.hasText(sc.getName()) ? sc.getName() : scope;
        String description = sc.getDescription();
        ScopeType type = sc.getType() != null ? sc.getType() : ScopeType.GENERIC;
        Set<String> claims = sc.getClaims();
        Set<String> roles = sc.getRoles();
        boolean approvalRequired = sc.isApprovalRequired();

        ServiceScope s = serviceService.updateScope(serviceId, scope,
                name, description, type,
                claims, roles,
                approvalRequired);

        // register, will override previous registration if present
//        scopeRegistry.registerScope(s);

        return s;
    }

    public void deleteServiceScope(String realm, String serviceId, String scope)
            throws NoSuchServiceException, NoSuchScopeException {

        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        ServiceScope sc = serviceService.getScope(serviceId, scope);
        if (sc != null) {
            // unregister
//            scopeRegistry.unregisterScope(sc);

            // TODO invalidate tokens with this scope?

            // remove approvals
            try {
                Collection<Approval> approvals = approvalStore.findScopeApprovals(scope);
                approvalStore.revokeApprovals(approvals);
            } catch (Exception e) {
            }

            // remove
            serviceService.deleteScope(serviceId, scope);

        }
    }

    /*
     * Claims
     */

    public List<ServiceClaim> listServiceClaims(String realm, String serviceId) throws NoSuchServiceException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        return serviceService.listClaims(serviceId);
    }

    public ServiceClaim getServiceClaim(String realm, String serviceId, String key)
            throws NoSuchServiceException, NoSuchClaimException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        return serviceService.getClaim(serviceId, key);
    }

    public ServiceClaim addServiceClaim(String realm, String serviceId, ServiceClaim claim)
            throws NoSuchServiceException, RegistrationException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        // explode
        String key = claim.getKey();
        String name = StringUtils.hasText(claim.getName()) ? claim.getName() : key;
        String description = claim.getDescription();
        AttributeType type = claim.getType() != null ? claim.getType() : AttributeType.STRING;
        boolean isMultiple = claim.isMultiple();

        ServiceClaim sc = serviceService.addClaim(serviceId, key,
                name, description, type, isMultiple);

        return sc;

    }

    public ServiceClaim updateServiceClaim(String realm, String serviceId, String key, ServiceClaim claim)
            throws NoSuchServiceException, NoSuchClaimException {

        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        if (StringUtils.hasText(claim.getServiceId()) && !serviceId.equals(claim.getServiceId())) {
            throw new IllegalArgumentException("claim does not match service");
        }

        if (StringUtils.hasText(claim.getKey()) && !key.equals(claim.getKey())) {
            throw new IllegalArgumentException("key does not match");
        }

        // explode
        String name = StringUtils.hasText(claim.getName()) ? claim.getName() : key;
        String description = claim.getDescription();
        AttributeType type = claim.getType() != null ? claim.getType() : AttributeType.STRING;
        boolean isMultiple = claim.isMultiple();

        ServiceClaim sc = serviceService.updateClaim(serviceId, key,
                name, description, type, isMultiple);

        return sc;
    }

    public void deleteServiceClaim(String realm, String serviceId, String key)
            throws NoSuchServiceException, NoSuchClaimException {

        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        ServiceClaim claim = serviceService.getClaim(serviceId, key);
        if (claim != null) {
            // TODO update custom extractors when implemented

            // we leave current tokens with the claim populated
            // remove only entity
            serviceService.deleteClaim(serviceId, key);
        }
    }

}
