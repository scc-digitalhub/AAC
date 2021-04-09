package it.smartcommunitylab.aac.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimRepository;
import it.smartcommunitylab.aac.services.persistence.ServiceEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceEntityRepository;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeRepository;

@Service
public class ServicesService implements ScopeProvider {

    private final ServiceEntityRepository serviceRepository;
    private final ServiceScopeRepository scopeRepository;
    private final ServiceClaimRepository claimRepository;

    public ServicesService(
            ServiceEntityRepository serviceRepository,
            ServiceScopeRepository scopeRepository,
            ServiceClaimRepository claimRepository) {
        Assert.notNull(serviceRepository, "service repository is required");
        Assert.notNull(scopeRepository, "scope repository is required");
        Assert.notNull(claimRepository, "claim repository is required");

        this.serviceRepository = serviceRepository;
        this.scopeRepository = scopeRepository;
        this.claimRepository = claimRepository;

    }

    /*
     * Namespaces
     */
    public List<String> listNamespaces() {
        return serviceRepository.listAllNamespaces();
    }

    public it.smartcommunitylab.aac.services.Service findServiceByNamespace(String namespace) {
        ServiceEntity s = serviceRepository.findByNamespace(namespace);
        if (s == null) {
            return null;
        }

        return toService(s);
    }

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

    public it.smartcommunitylab.aac.services.Service getService(String serviceId) throws NoSuchServiceException {
        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceScope> scopes = listScopes(serviceId);
        List<ServiceClaim> claims = listClaims(serviceId);

        return toService(s, scopes, claims);
    }

    public it.smartcommunitylab.aac.services.Service findService(String serviceId) {
        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s == null) {
            return null;
        }

        return toService(s);
    }

    public List<it.smartcommunitylab.aac.services.Service> listServices(String realm) {
        List<ServiceEntity> services = serviceRepository.findByRealm(realm);
        return services.stream().map(s -> toService(s)).collect(Collectors.toList());
    }

    public it.smartcommunitylab.aac.services.Service addService(
            String realm, String namespace,
            String name, String description) {

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("empty realm");
        }

        if (!StringUtils.hasText(namespace)) {
            throw new IllegalArgumentException("empty namespace");
        }

        ServiceEntity se = serviceRepository.findByNamespace(namespace);
        if (se != null) {
            throw new RegistrationException("duplicated namespace");
        }

        // generate unique serviceId
        String uuid = UUID.randomUUID().toString();

        se = new ServiceEntity();
        se.setServiceId(uuid);
        se.setNamespace(namespace);
        se.setRealm(realm);

        se.setName(name);
        se.setDescription(description);

        se = serviceRepository.save(se);
        return toService(se);
    }

    public it.smartcommunitylab.aac.services.Service updateService(
            String serviceId,
            String name, String description,
            Map<String, String> claimMapping) throws NoSuchServiceException {

        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        se.setName(name);
        se.setDescription(description);
        se.setClaimMappings(claimMapping);

        se = serviceRepository.save(se);
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

    }

    private it.smartcommunitylab.aac.services.Service toService(ServiceEntity s) {
        it.smartcommunitylab.aac.services.Service service = new it.smartcommunitylab.aac.services.Service();
        service.setServiceId(s.getServiceId());
        service.setRealm(s.getRealm());

        service.setNamespace(s.getNamespace());
        service.setName(s.getName());
        service.setDescription(s.getDescription());
        service.setClaimMapping(s.getClaimMappings());

        return service;
    }

    private it.smartcommunitylab.aac.services.Service toService(ServiceEntity s,
            List<ServiceScope> scopes, List<ServiceClaim> claims) {
        it.smartcommunitylab.aac.services.Service service = toService(s);
        service.setScopes(scopes);
        service.setClaims(claims);

        return service;
    }

    /*
     * Service scopes
     */
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

    public List<ServiceScope> listScopes(String serviceId) throws NoSuchServiceException {
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceScopeEntity> list = scopeRepository.findByServiceId(serviceId);
        return list.stream().map(s -> ServiceScope.from(s, se.getNamespace())).collect(Collectors.toList());
    }

    public List<ServiceScope> listScopes(String serviceId, String type) throws NoSuchServiceException {
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceScopeEntity> list = scopeRepository.findByServiceIdAndType(serviceId, type);
        return list.stream().map(s -> ServiceScope.from(s, se.getNamespace())).collect(Collectors.toList());
    }

    public ServiceScope addScope(
            String serviceId, String scope,
            String name, String description,
            ScopeType type,
            Collection<String> claims, Collection<String> roles,
            boolean approvalRequired) throws NoSuchServiceException, RegistrationException {

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
            throw new RegistrationException("duplicated scope");
        }

        ServiceScopeEntity se = new ServiceScopeEntity();
        se.setScope(scope);
        se.setServiceId(serviceId);

        se.setName(name);
        se.setDescription(description);

        se.setType(type.getValue());

        se.setClaims(StringUtils.collectionToCommaDelimitedString(claims));
        se.setRoles(StringUtils.collectionToCommaDelimitedString(roles));

        se.setApprovalRequired(approvalRequired);

        se = scopeRepository.save(se);

        return ServiceScope.from(se, service.getNamespace());

    }

    public ServiceScope updateScope(
            String serviceId, String scope,
            String name, String description,
            ScopeType type,
            Collection<String> claims, Collection<String> roles,
            boolean approvalRequired) throws NoSuchServiceException, NoSuchScopeException {

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
        se.setRoles(StringUtils.collectionToCommaDelimitedString(roles));

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

    public ServiceClaim findClaim(String serviceId, String key) throws NoSuchClaimException {
        ServiceClaimEntity s = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (s == null) {
            return null;
        }

        return ServiceClaim.from(s);
    }

    public List<ServiceClaim> findClaims(String serviceId) {
        List<ServiceClaimEntity> list = claimRepository.findByServiceId(serviceId);
        return list.stream().map(se -> ServiceClaim.from(se)).collect(Collectors.toList());
    }

    public List<ServiceClaim> listClaims(String serviceId) throws NoSuchServiceException {
        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        List<ServiceClaimEntity> list = claimRepository.findByServiceId(serviceId);
        return list.stream().map(se -> ServiceClaim.from(se)).collect(Collectors.toList());
    }

    public ServiceClaim addClaim(
            String serviceId, String key,
            String name, String description,
            AttributeType type,
            boolean multiple) throws NoSuchServiceException, RegistrationException {

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
            throw new RegistrationException("duplicated key");
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
            String serviceId, String key,
            String name, String description,
            AttributeType type,
            boolean multiple) throws NoSuchServiceException, NoSuchClaimException {

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

    /*
     * Scope provider, used at bootstrap to feed registry
     * 
     * TODO reevaluate, providers should be one per service and registered
     */

    @Override
    public String getResourceId() {
        return null;
    }

    @Override
    public Collection<Scope> getScopes() {
        List<Scope> scopes = new ArrayList<>();

        // fetch all services and related scopes
        List<ServiceEntity> services = serviceRepository.findAll();
        for (ServiceEntity se : services) {
            List<ServiceScopeEntity> list = scopeRepository.findByServiceId(se.getServiceId());
            scopes.addAll(list.stream().map(s -> ServiceScope.from(s, se.getNamespace())).collect(Collectors.toList()));
        }

        return scopes;

    }
}
