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

package it.smartcommunitylab.aac.services;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.scope.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.CombinedScopeApprover;
import it.smartcommunitylab.aac.scope.DelegateScopeApprover;
import it.smartcommunitylab.aac.scope.RoleScopeApprover;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import it.smartcommunitylab.aac.scope.ScriptScopeApprover;
import it.smartcommunitylab.aac.scope.StoreScopeApprover;
import it.smartcommunitylab.aac.scope.WhitelistScopeApprover;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/*
 * Manage services and their integration.
 * Also handles permissions, by comparing roles in realm
 */

@Component
@PreAuthorize(
    "hasAuthority('" +
    Config.R_ADMIN +
    "')" +
    " or hasAuthority(#realm+':" +
    Config.R_ADMIN +
    "')" +
    " or hasAuthority(#realm+':" +
    Config.R_DEVELOPER +
    "')"
)
public class ServicesManager implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ServicesService serviceService;

    @Autowired
    private RealmService realmService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private ExtractorsRegistry extractorsRegistry;

    @Autowired
    private SearchableApprovalStore approvalStore;

    @Autowired
    private ScriptExecutionService executionService;

    private ServiceResourceClaimsExtractorProvider resourceClaimsExtractorProvider;

    public ServicesManager() {}

    @Override
    public void afterPropertiesSet() throws Exception {
        reload();
    }
  
    public void reload() throws Exception {
        // build and register the claims extractor
        resourceClaimsExtractorProvider = new ServiceResourceClaimsExtractorProvider(serviceService);
        resourceClaimsExtractorProvider.setExecutionService(executionService);

        extractorsRegistry.registerExtractorProvider(resourceClaimsExtractorProvider);

        // export all scope providers to registry
        List<Service> services = serviceService.listServices();
        for (Service service : services) {
            String serviceId = service.getServiceId();
            List<ServiceScope> scopes = serviceService.listScopes(serviceId);
            service.setScopes(scopes);

            // build provider
            ServiceScopeProvider sp = new ServiceScopeProvider(service);

            // add approvers where needed
            for (ServiceScope sc : scopes) {
                ScopeApprover approver = buildScopeApprover(service.getRealm(), service.getNamespace(), sc);
                if (approver != null) {
                    sp.addApprover(sc.getScope(), approver);
                }
            }

            // unregister provider if present
            String namespace = service.getNamespace().toLowerCase();
            ScopeProvider spr = scopeRegistry.findScopeProvider(namespace);
            if (spr != null && spr instanceof ServiceScopeProvider) {
                scopeRegistry.unregisterScopeProvider(spr);
            }

            // register
            scopeRegistry.registerScopeProvider(sp);
        }
    } 


    /*
     * Services
     *
     */
    public Service findService(String realm, String serviceId) {
        Service service = serviceService.findService(serviceId);

        if (service == null) {
            return null;
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        // note: find returns a bare model
        return service;
    }

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

    public Service addService(String realm, Service service) throws NoSuchRealmException, RegistrationException {
        // fetch realm
        Realm re = realmService.getRealm(realm);
        String serviceId = service.getServiceId();
        String namespace = Jsoup.clean(service.getNamespace().toLowerCase(), Safelist.none());
        String name = service.getName();
        String description = service.getDescription();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (!StringUtils.hasText(name)) {
            name = namespace;
        }

        // add
        Service s = serviceService.addService(re.getSlug(), serviceId, namespace, name, description);
        serviceId = s.getServiceId();

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

            // build provider
            if (!serviceScopes.isEmpty()) {
                ServiceScopeProvider sp = new ServiceScopeProvider(s);

                // add approvers where needed
                for (ServiceScope sc : serviceScopes) {
                    ScopeApprover approver = buildScopeApprover(s.getRealm(), s.getNamespace(), sc);
                    if (approver != null) {
                        sp.addApprover(sc.getScope(), approver);
                    }
                }

                // register
                scopeRegistry.registerScopeProvider(sp);
            }
        } catch (NoSuchServiceException e) {
            // something broken
            throw new SystemException();
        }

        return s;
    }

    public Service updateService(String realm, String serviceId, Service service)
        throws NoSuchServiceException, NoSuchRealmException, RegistrationException {
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
        String namespace = service.getNamespace().toLowerCase();
        String name = service.getName();
        String description = service.getDescription();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (!StringUtils.hasText(name)) {
            name = namespace;
        }
        Map<String, String> claimMapping = service.getClaimMapping();

        // update
        Service result = serviceService.updateService(serviceId, name, description, claimMapping);
        // inflate with old data
        result.setScopes(ss.getScopes());
        result.setClaims(ss.getClaims());

        try {
            // unregister provider if present
            ScopeProvider spr = scopeRegistry.findScopeProvider(namespace);
            if (spr != null && spr instanceof ServiceScopeProvider) {
                scopeRegistry.unregisterScopeProvider(spr);
            }

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
            Set<ServiceScope> removedScopes = ss
                .getScopes()
                .stream()
                .filter(s -> !serviceScopes.contains(s))
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
            Set<ServiceClaim> removedClaims = ss
                .getClaims()
                .stream()
                .filter(s -> !serviceClaims.contains(s))
                .collect(Collectors.toSet());
            for (ServiceClaim sc : removedClaims) {
                try {
                    deleteServiceClaim(realm, serviceId, sc.getKey());
                } catch (NoSuchClaimException e) {
                    // already removed
                }
            }

            result.setClaims(serviceClaims);

            // build provider
            if (!serviceScopes.isEmpty()) {
                ServiceScopeProvider sp = new ServiceScopeProvider(result);

                // add approvers where needed
                for (ServiceScope sc : serviceScopes) {
                    ScopeApprover approver = buildScopeApprover(result.getRealm(), result.getNamespace(), sc);
                    if (approver != null) {
                        sp.addApprover(sc.getScope(), approver);
                    }
                }

                // register
                scopeRegistry.registerScopeProvider(sp);
            }
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

            // unregister provider if present
            String namespace = service.getNamespace();
            ScopeProvider spr = scopeRegistry.findScopeProvider(namespace);
            if (spr != null && spr instanceof ServiceScopeProvider) {
                scopeRegistry.unregisterScopeProvider(spr);
            }

            // cleanup scopes
            for (ServiceScope sc : service.getScopes()) {
                // TODO invalidate tokens with this scope?

                // remove approvals
                try {
                    Collection<Approval> approvals = approvalStore.findScopeApprovals(sc.getScope());
                    approvalStore.revokeApprovals(approvals);
                } catch (Exception e) {}
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
        String scope = Jsoup.clean(sc.getScope().toLowerCase(), Safelist.none());
        String name = sc.getName();
        String description = sc.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (!StringUtils.hasText(name)) {
            name = scope;
        }

        // check if scope if already defined elsewhere
        Scope se = scopeRegistry.findScope(scope);
        if (se != null) {
            throw new DuplicatedDataException("scope");
        }

        ScopeType type = sc.getType() != null ? sc.getType() : ScopeType.GENERIC;
        Set<String> claims = sc.getClaims();
        Set<String> roles = sc.getApprovalRoles();
        Set<String> spaceRoles = sc.getApprovalSpaceRoles();
        String approvalFunction = sc.getApprovalFunction();

        boolean approvalAny = sc.isApprovalAny();
        boolean approvalRequired = sc.isApprovalRequired();

        ServiceScope s = serviceService.addScope(
            serviceId,
            scope,
            name,
            description,
            type,
            claims,
            roles,
            spaceRoles,
            approvalFunction,
            approvalRequired,
            approvalAny
        );

        // refresh service
        String namespace = service.getNamespace();
        List<ServiceScope> serviceScopes = serviceService.listScopes(serviceId);
        service.setScopes(serviceScopes);

        // unregister provider if present
        ScopeProvider spr = scopeRegistry.findScopeProvider(namespace);
        if (spr != null && spr instanceof ServiceScopeProvider) {
            scopeRegistry.unregisterScopeProvider(spr);
        }

        // build provider
        if (!serviceScopes.isEmpty()) {
            ServiceScopeProvider sp = new ServiceScopeProvider(service);

            // add approvers where needed
            for (ServiceScope scs : serviceScopes) {
                ScopeApprover approver = buildScopeApprover(service.getRealm(), service.getNamespace(), scs);
                if (approver != null) {
                    sp.addApprover(scs.getScope(), approver);
                }
            }

            // register
            scopeRegistry.registerScopeProvider(sp);
        }

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
        String name = sc.getName();
        String description = sc.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (!StringUtils.hasText(name)) {
            name = scope;
        }
        ScopeType type = sc.getType() != null ? sc.getType() : ScopeType.GENERIC;
        Set<String> claims = sc.getClaims();
        Set<String> roles = sc.getApprovalRoles();
        Set<String> spaceRoles = sc.getApprovalSpaceRoles();
        String approvalFunction = sc.getApprovalFunction();

        boolean approvalAny = sc.isApprovalAny();
        boolean approvalRequired = sc.isApprovalRequired();

        ServiceScope s = serviceService.updateScope(
            serviceId,
            scope,
            name,
            description,
            type,
            claims,
            roles,
            spaceRoles,
            approvalFunction,
            approvalRequired,
            approvalAny
        );

        // refresh service
        String namespace = service.getNamespace();
        List<ServiceScope> serviceScopes = serviceService.listScopes(serviceId);
        service.setScopes(serviceScopes);

        // unregister provider if present
        ScopeProvider spr = scopeRegistry.findScopeProvider(namespace);
        if (spr != null && spr instanceof ServiceScopeProvider) {
            scopeRegistry.unregisterScopeProvider(spr);
        }

        // build provider
        if (!serviceScopes.isEmpty()) {
            ServiceScopeProvider sp = new ServiceScopeProvider(service);

            // add approvers where needed
            for (ServiceScope scs : serviceScopes) {
                ScopeApprover approver = buildScopeApprover(service.getRealm(), service.getNamespace(), scs);
                if (approver != null) {
                    sp.addApprover(scs.getScope(), approver);
                }
            }

            // register
            scopeRegistry.registerScopeProvider(sp);
        }

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
            String namespace = service.getNamespace();

            // unregister provider if present
            ScopeProvider spr = scopeRegistry.findScopeProvider(namespace);
            if (spr != null && spr instanceof ServiceScopeProvider) {
                scopeRegistry.unregisterScopeProvider(spr);
            }

            // TODO invalidate tokens with this scope?

            // remove approvals
            try {
                Collection<Approval> approvals = approvalStore.findScopeApprovals(scope);
                approvalStore.revokeApprovals(approvals);
            } catch (Exception e) {}

            // remove
            serviceService.deleteScope(serviceId, scope);

            // refresh service
            List<ServiceScope> serviceScopes = serviceService.listScopes(serviceId);
            service.setScopes(serviceScopes);

            // build provider
            if (!serviceScopes.isEmpty()) {
                ServiceScopeProvider sp = new ServiceScopeProvider(service);

                // add approvers where needed
                for (ServiceScope scs : serviceScopes) {
                    ScopeApprover approver = buildScopeApprover(service.getRealm(), service.getNamespace(), scs);
                    if (approver != null) {
                        sp.addApprover(scs.getScope(), approver);
                    }
                }

                // register
                scopeRegistry.registerScopeProvider(sp);
            }
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
        String key = Jsoup.clean(claim.getKey(), Safelist.none());
        String name = claim.getName();
        String description = claim.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (!StringUtils.hasText(name)) {
            name = key;
        }
        AttributeType type = claim.getType() != null ? claim.getType() : AttributeType.STRING;
        boolean isMultiple = claim.isMultiple();

        ServiceClaim sc = serviceService.addClaim(serviceId, key, name, description, type, isMultiple);

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
        String name = claim.getName();
        String description = claim.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (!StringUtils.hasText(name)) {
            name = key;
        }
        AttributeType type = claim.getType() != null ? claim.getType() : AttributeType.STRING;
        boolean isMultiple = claim.isMultiple();

        ServiceClaim sc = serviceService.updateClaim(serviceId, key, name, description, type, isMultiple);

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

    /*
     * Scope Approvals
     */

    public Collection<Approval> getServiceScopeApprovals(String realm, String serviceId, String scope)
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

        // we use serviceId as id
        String resourceId = serviceId;

        // fetch approvals from store, serviceId is stored in userId
        return approvalStore.findUserScopeApprovals(resourceId, sc.getScope());
    }

    /**
     * @param realm
     * @param serviceId
     * @return
     * @throws NoSuchServiceException
     */
    public Collection<Approval> getServiceApprovals(String realm, String serviceId) throws NoSuchServiceException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }
        // we use serviceId as id
        String resourceId = serviceId;
        return approvalStore.findUserApprovals(resourceId);
    }

    public Approval addServiceScopeApproval(
        String realm,
        String serviceId,
        String scope,
        String subjectId,
        int duration,
        boolean approved
    ) throws NoSuchServiceException, NoSuchScopeException {
        // use finder to avoid loading all related
        Service service = serviceService.findService(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("service does not match realm");
        }

        ServiceScope sc = serviceService.getScope(serviceId, scope);

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

        Approval approval = new Approval(resourceId, subjectId, sc.getScope(), expiresAt, approvalStatus);
        approvalStore.addApprovals(Collections.singleton(approval));

        return approvalStore.findApproval(serviceId, subjectId, scope);
    }

    public void revokeServiceScopeApproval(String realm, String serviceId, String scope, String subjectId)
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

        // we use serviceId as id
        String resourceId = serviceId;

        Approval approval = approvalStore.findApproval(resourceId, subjectId, sc.getScope());
        if (approval != null) {
            approvalStore.revokeApprovals(Collections.singleton(approval));
        }
    }

    private ScopeApprover buildScopeApprover(String realm, String namespace, ServiceScope sc) {
        String scope = sc.getScope();
        List<ScopeApprover> approvers = new ArrayList<>();
        if (StringUtils.hasText(sc.getApprovalFunction())) {
            ScriptScopeApprover sa = new ScriptScopeApprover(realm, namespace, scope);
            sa.setExecutionService(executionService);
            sa.setFunctionCode(sc.getApprovalFunction());
            approvers.add(sa);
        }

        if (sc.getApprovalRoles() != null && !sc.getApprovalRoles().isEmpty()) {
            AuthorityScopeApprover sa = new AuthorityScopeApprover(realm, namespace, scope);
            sa.setAuthorities(sc.getApprovalRoles());
            approvers.add(sa);
        }

        if (sc.getApprovalSpaceRoles() != null && !sc.getApprovalSpaceRoles().isEmpty()) {
            RoleScopeApprover sa = new RoleScopeApprover(realm, namespace, scope);
            sa.setRoles(sc.getApprovalSpaceRoles());
            approvers.add(sa);
        }

        if (sc.isApprovalRequired()) {
            StoreScopeApprover sa = new StoreScopeApprover(realm, namespace, scope);
            sa.setApprovalStore(approvalStore);
            // we use serviceId as the authorizer authority
            sa.setUserId(sc.getServiceId());
            approvers.add(sa);
        }

        if (approvers.isEmpty()) {
            // use whitelist, scope is autoapproved
            return new WhitelistScopeApprover(realm, namespace, scope);
        }

        if (approvers.size() == 1) {
            return approvers.get(0);
        }

        if (sc.isApprovalAny()) {
            return new DelegateScopeApprover(realm, namespace, scope, approvers);
        } else {
            return new CombinedScopeApprover(realm, namespace, scope, approvers);
        }
    }

    /**
     *
     * @param serviceNamespace
     * @return
     */
    public Boolean checkServiceNamespace(String realm, String serviceNamespace) {
        return serviceService.findServiceByNamespace(serviceNamespace.toLowerCase()) != null;
    }
}
