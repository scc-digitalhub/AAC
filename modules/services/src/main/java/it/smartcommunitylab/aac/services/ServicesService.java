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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimRepository;
import it.smartcommunitylab.aac.services.persistence.ServiceEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceEntityRepository;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ServicesService {

    private final ServiceEntityRepository serviceRepository;
    private final ServiceScopeRepository scopeRepository;
    private final ServiceClaimRepository claimRepository;

    private final SubjectService subjectService;

    public ServicesService(
        ServiceEntityRepository serviceRepository,
        ServiceScopeRepository scopeRepository,
        ServiceClaimRepository claimRepository,
        SubjectService subjectService
    ) {
        Assert.notNull(serviceRepository, "service repository is required");
        Assert.notNull(scopeRepository, "scope repository is required");
        Assert.notNull(claimRepository, "claim repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.serviceRepository = serviceRepository;
        this.scopeRepository = scopeRepository;
        this.claimRepository = claimRepository;

        this.subjectService = subjectService;
    }

    /*
     * Namespaces
     */
    @Transactional(readOnly = true)
    public List<String> listNamespaces() {
        return serviceRepository.listAllNamespaces();
    }

    @Transactional(readOnly = true)
    public it.smartcommunitylab.aac.services.Service findServiceByNamespace(String namespace) {
        ServiceEntity s = serviceRepository.findByNamespace(namespace);
        if (s == null) {
            return null;
        }

        return toService(s);
    }

    @Transactional(readOnly = true)
    public it.smartcommunitylab.aac.services.Service getServiceByNamespace(String namespace)
        throws NoSuchServiceException {
        ServiceEntity s = serviceRepository.findByNamespace(namespace);
        if (s == null) {
            throw new NoSuchServiceException();
        }

        String serviceId = s.getServiceId();

        List<ServiceScope> scopes = listScopes(serviceId);
        List<ServiceClaim> claims = listClaims(serviceId);

        return toService(s, scopes, claims);
    }

    /*
     * Services
     */

    @Transactional(readOnly = true)
    public it.smartcommunitylab.aac.services.Service getService(String serviceId) throws NoSuchServiceException {
        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceScope> scopes = listScopes(serviceId);
        List<ServiceClaim> claims = listClaims(serviceId);

        return toService(s, scopes, claims);
    }

    @Transactional(readOnly = true)
    public it.smartcommunitylab.aac.services.Service findService(String serviceId) {
        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s == null) {
            return null;
        }

        return toService(s);
    }

    @Transactional(readOnly = true)
    public List<it.smartcommunitylab.aac.services.Service> listServices() {
        List<ServiceEntity> services = serviceRepository.findAll();
        return services.stream().map(s -> toService(s)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<it.smartcommunitylab.aac.services.Service> listServices(String realm) {
        List<ServiceEntity> services = serviceRepository.findByRealm(realm);
        return services
            .stream()
            .map(s -> {
                try {
                    return toService(s, listScopes(s.getServiceId()), null);
                } catch (NoSuchServiceException e) {
                    return null;
                }
            })
            .filter(s -> s != null)
            .collect(Collectors.toList());
    }

    public it.smartcommunitylab.aac.services.Service addService(
        String realm,
        String serviceId,
        String namespace,
        String name,
        String description
    ) throws RegistrationException {
        if (!StringUtils.hasText(realm)) {
            throw new MissingDataException("realm");
        }

        if (!StringUtils.hasText(namespace)) {
            throw new MissingDataException("namespace");
        }

        ServiceEntity se = serviceRepository.findByNamespace(namespace);
        if (se != null) {
            throw new DuplicatedDataException("namespace");
        }

        if (!StringUtils.hasText(serviceId)) {
            serviceId = subjectService.generateUuid(SystemKeys.RESOURCE_SERVICE);
        }

        // create subject
        Subject s = subjectService.addSubject(serviceId, realm, SystemKeys.RESOURCE_SERVICE, namespace);

        se = new ServiceEntity();
        se.setServiceId(serviceId);
        se.setNamespace(namespace);
        se.setRealm(realm);

        se.setName(name);
        se.setDescription(description);

        se = serviceRepository.save(se);
        return toService(se);
    }

    public it.smartcommunitylab.aac.services.Service updateService(
        String serviceId,
        String name,
        String description,
        Map<String, String> claimMapping
    ) throws NoSuchServiceException {
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        se.setName(name);
        se.setDescription(description);
        se.setClaimMappings(claimMapping);

        se = serviceRepository.save(se);

        // check if subject exists
        Subject s = subjectService.findSubject(serviceId);
        if (s == null) {
            s = subjectService.addSubject(serviceId, se.getRealm(), SystemKeys.RESOURCE_SERVICE, se.getNamespace());
        }

        return toService(se);
    }

    public void deleteService(String serviceId) throws NoSuchServiceException {
        // delete related
        List<ServiceScope> scopes = listScopes(serviceId);
        if (!scopes.isEmpty()) {
            for (ServiceScope ss : scopes) {
                deleteScope(serviceId, ss.getScope());
            }
        }

        List<ServiceClaim> claims = listClaims(serviceId);
        if (!claims.isEmpty()) {
            for (ServiceClaim sc : claims) {
                deleteClaim(serviceId, sc.getKey());
            }
        }

        // delete
        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s != null) {
            serviceRepository.delete(s);
        }

        // remove subject if exists
        subjectService.deleteSubject(serviceId);
    }

    private it.smartcommunitylab.aac.services.Service toService(ServiceEntity s) {
        it.smartcommunitylab.aac.services.Service service = new it.smartcommunitylab.aac.services.Service();
        service.setServiceId(s.getServiceId());
        service.setRealm(s.getRealm());

        service.setNamespace(s.getNamespace());
        service.setName(s.getName());
        service.setDescription(s.getDescription());
        service.setClaimMapping(s.getClaimMappings() != null ? s.getClaimMappings() : Collections.emptyMap());

        return service;
    }

    private it.smartcommunitylab.aac.services.Service toService(
        ServiceEntity s,
        List<ServiceScope> scopes,
        List<ServiceClaim> claims
    ) {
        it.smartcommunitylab.aac.services.Service service = toService(s);
        service.setScopes(scopes);
        service.setClaims(claims);
        return service;
    }

    /*
     * Service scopes
     */
    @Transactional(readOnly = true)
    public ServiceScope getScope(String serviceId, String scope) throws NoSuchScopeException, NoSuchServiceException {
        ServiceScopeEntity s = scopeRepository.findByServiceIdAndScope(serviceId, scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        return ServiceScope.from(s, se.getNamespace());
    }

    @Transactional(readOnly = true)
    public ServiceScope findScope(String serviceId, String scope) throws NoSuchServiceException {
        ServiceScopeEntity s = scopeRepository.findByServiceIdAndScope(serviceId, scope);
        if (s == null) {
            return null;
        }
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }
        return ServiceScope.from(s, se.getNamespace());
    }

    @Transactional(readOnly = true)
    public List<ServiceScope> listScopes(String serviceId) throws NoSuchServiceException {
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceScopeEntity> list = scopeRepository.findByServiceId(serviceId);
        return list.stream().map(s -> ServiceScope.from(s, se.getNamespace())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceScope> listScopes(String serviceId, String type) throws NoSuchServiceException {
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceScopeEntity> list = scopeRepository.findByServiceIdAndType(serviceId, type);
        return list.stream().map(s -> ServiceScope.from(s, se.getNamespace())).collect(Collectors.toList());
    }

    public ServiceScope addScope(
        String serviceId,
        String scope,
        String name,
        String description,
        ScopeType type,
        Collection<String> claims,
        Collection<String> roles,
        Collection<String> spaceRoles,
        String approvalFunction,
        boolean approvalRequired,
        boolean approvalAny
    ) throws NoSuchServiceException, RegistrationException {
        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        if (type == null) {
            type = ScopeType.GENERIC;
        }

        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        ServiceScopeEntity s = scopeRepository.findByServiceIdAndScope(serviceId, scope);
        if (s != null) {
            throw new DuplicatedDataException("scope");
        }

        ServiceScopeEntity se = new ServiceScopeEntity();
        se.setScope(scope);
        se.setServiceId(serviceId);

        se.setName(name);
        se.setDescription(description);

        se.setType(type.getValue());

        se.setClaims(StringUtils.collectionToCommaDelimitedString(claims));
        se.setApprovalRoles(StringUtils.collectionToCommaDelimitedString(roles));
        se.setApprovalSpaceRoles(StringUtils.collectionToCommaDelimitedString(spaceRoles));
        se.setApprovalFunction(approvalFunction);

        se.setApprovalAny(approvalAny);
        se.setApprovalRequired(approvalRequired);

        se = scopeRepository.save(se);

        return ServiceScope.from(se, service.getNamespace());
    }

    public ServiceScope updateScope(
        String serviceId,
        String scope,
        String name,
        String description,
        ScopeType type,
        Collection<String> claims,
        Collection<String> roles,
        Collection<String> spaceRoles,
        String approvalFunction,
        boolean approvalRequired,
        boolean approvalAny
    ) throws NoSuchServiceException, NoSuchScopeException {
        if (type == null) {
            type = ScopeType.GENERIC;
        }

        ServiceScopeEntity se = scopeRepository.findByServiceIdAndScope(serviceId, scope);
        if (se == null) {
            throw new NoSuchScopeException();
        }

        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        se.setName(name);
        se.setDescription(description);

        se.setType(type.getValue());

        se.setClaims(StringUtils.collectionToCommaDelimitedString(claims));
        se.setApprovalRoles(StringUtils.collectionToCommaDelimitedString(roles));
        se.setApprovalSpaceRoles(StringUtils.collectionToCommaDelimitedString(spaceRoles));
        se.setApprovalFunction(approvalFunction);

        se.setApprovalAny(approvalAny);
        se.setApprovalRequired(approvalRequired);

        se = scopeRepository.save(se);

        return ServiceScope.from(se, service.getNamespace());
    }

    public void deleteScope(String serviceId, String scope) {
        ServiceScopeEntity s = scopeRepository.findByServiceIdAndScope(serviceId, scope);
        if (s != null) {
            scopeRepository.delete(s);
        }
    }

    /*
     * Service claims
     */
    @Transactional(readOnly = true)
    public ServiceClaim getClaim(String serviceId, String key) throws NoSuchClaimException, NoSuchServiceException {
        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        ServiceClaimEntity s = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (s == null) {
            throw new NoSuchClaimException();
        }

        return ServiceClaim.from(s);
    }

    @Transactional(readOnly = true)
    public ServiceClaim findClaim(String serviceId, String key) throws NoSuchClaimException {
        ServiceClaimEntity s = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (s == null) {
            return null;
        }

        return ServiceClaim.from(s);
    }

    @Transactional(readOnly = true)
    public List<ServiceClaim> findClaims(String serviceId) {
        List<ServiceClaimEntity> list = claimRepository.findByServiceId(serviceId);
        return list.stream().map(se -> ServiceClaim.from(se)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceClaim> listClaims(String serviceId) throws NoSuchServiceException {
        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceClaimEntity> list = claimRepository.findByServiceId(serviceId);
        return list.stream().map(se -> ServiceClaim.from(se)).collect(Collectors.toList());
    }

    public ServiceClaim addClaim(
        String serviceId,
        String key,
        String name,
        String description,
        AttributeType type,
        boolean multiple
    ) throws NoSuchServiceException, RegistrationException {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("invalid key");
        }

        if (type == null) {
            type = AttributeType.STRING;
        }

        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        ServiceClaimEntity s = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (s != null) {
            throw new DuplicatedDataException("key");
        }

        ServiceClaimEntity sc = new ServiceClaimEntity();
        sc.setKey(key);
        sc.setServiceId(serviceId);

        sc.setName(name);
        sc.setDescription(description);

        sc.setType(type.getValue());
        sc.setMultiple(multiple);

        sc = claimRepository.save(sc);

        return ServiceClaim.from(sc);
    }

    public ServiceClaim updateClaim(
        String serviceId,
        String key,
        String name,
        String description,
        AttributeType type,
        boolean multiple
    ) throws NoSuchServiceException, NoSuchClaimException {
        if (type == null) {
            type = AttributeType.STRING;
        }

        ServiceClaimEntity sc = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (sc == null) {
            throw new NoSuchClaimException();
        }

        sc.setName(name);
        sc.setDescription(description);

        sc.setType(type.getValue());
        sc.setMultiple(multiple);

        sc = claimRepository.save(sc);

        return ServiceClaim.from(sc);
    }

    public void deleteClaim(String serviceId, String key) {
        ServiceClaimEntity sc = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (sc != null) {
            claimRepository.delete(sc);
        }
    }
    //    /*
    //     * Scope provider, used at bootstrap to feed registry
    //     *
    //     * TODO reevaluate, providers should be one per service and registered
    //     */
    //
    //    @Override
    //    public String getResourceId() {
    //        return null;
    //    }
    //
    //    @Override
    //    public Collection<Scope> getScopes() {
    //        List<Scope> scopes = new ArrayList<>();
    //
    //        // fetch all services and related scopes
    //        List<ServiceEntity> services = serviceRepository.findAll();
    //        for (ServiceEntity se : services) {
    //            List<ServiceScopeEntity> list = scopeRepository.findByServiceId(se.getServiceId());
    //            scopes.addAll(list.stream().map(s -> ServiceScope.from(s, se.getNamespace())).collect(Collectors.toList()));
    //        }
    //
    //        return scopes;
    //
    //    }
}
