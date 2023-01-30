package it.smartcommunitylab.aac.services.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.services.model.ApiService;
import it.smartcommunitylab.aac.services.model.ApiServiceClaimDefinition;
import it.smartcommunitylab.aac.services.model.ApiServiceScope;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimRepository;
import it.smartcommunitylab.aac.services.persistence.ServiceEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceEntityRepository;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeEntity;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeRepository;

@Service
@Transactional
public class ServicesService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceEntityRepository serviceRepository;
    private final ServiceScopeRepository scopeRepository;
    private final ServiceClaimRepository claimRepository;

    private final SubjectService subjectService;

    private final String baseUrl;

    public ServicesService(
            ServiceEntityRepository serviceRepository,
            ServiceScopeRepository scopeRepository,
            ServiceClaimRepository claimRepository,
            SubjectService subjectService,
            @Value("${application.url}") String baseUrl) {
        Assert.notNull(serviceRepository, "service repository is required");
        Assert.notNull(scopeRepository, "scope repository is required");
        Assert.notNull(claimRepository, "claim repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.hasText(baseUrl, "baseUrl can not be null or empty");

        this.serviceRepository = serviceRepository;
        this.scopeRepository = scopeRepository;
        this.claimRepository = claimRepository;

        this.subjectService = subjectService;

        this.baseUrl = baseUrl;
    }

    /*
     * Namespaces
     */
    @Transactional(readOnly = true)
    public List<String> listNamespaces(@NotBlank String realm) {
        logger.debug("list namespaces for realm {}", String.valueOf(realm));

        return serviceRepository.listAllNamespacesByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<String> listResources(@NotBlank String realm) {
        logger.debug("list resources for realm {}", String.valueOf(realm));

        return serviceRepository.listAllResourcesByRealm(realm);
    }

    /*
     * Api Service
     * 
     * notes: find methods will resolve only the service, while get/list will
     * resolve also related entities and load
     */
    private ApiService loadService(ServiceEntity s) {
        String serviceId = s.getServiceId();

        List<ServiceScopeEntity> scopes = fetchScopes(serviceId);
        List<ServiceClaimEntity> claims = fetchClaims(serviceId);

        return toService(s, scopes, claims);
    }

    @Transactional(readOnly = true)
    public ApiService findService(String serviceId) {
        logger.debug("find service with id {}", String.valueOf(serviceId));

        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s == null) {
            return null;
        }

        // find loads only the service definition, not the whole object
        return toService(s);
    }

    @Transactional(readOnly = true)
    public ApiService getService(String serviceId) throws NoSuchServiceException {
        logger.debug("get service with id {}", String.valueOf(serviceId));

        ServiceEntity s = serviceRepository.findOne(serviceId);
        if (s == null) {
            throw new NoSuchServiceException();
        }

        // get loads the whole object
        return loadService(s);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public ApiService findServiceByNamespace(@NotBlank String realm, @NotBlank String namespace) {
        logger.debug("find service with namespace {} for realm {}", String.valueOf(namespace), String.valueOf(realm));

        ServiceEntity s = serviceRepository.findByRealmAndNamespace(realm, namespace);
        if (s == null) {
            return null;
        }

        return toService(s);
    }

    @Transactional(readOnly = true)
    public ApiService findServiceByResource(@NotBlank String realm, @NotBlank String resource) {
        logger.debug("find service with resource {} for realm {}", String.valueOf(resource), String.valueOf(realm));

        ServiceEntity s = serviceRepository.findByRealmAndResource(realm, resource);
        if (s == null) {
            return null;
        }

        return toService(s);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public ApiService getServiceByNamespace(@NotBlank String realm, @NotBlank String namespace)
            throws NoSuchServiceException {
        logger.debug("get service with namespace {} for realm {}", String.valueOf(namespace), String.valueOf(realm));

        ServiceEntity s = serviceRepository.findByRealmAndNamespace(realm, namespace);
        if (s == null) {
            throw new NoSuchServiceException();
        }

        return loadService(s);
    }

    @Transactional(readOnly = true)
    public ApiService getServiceByResource(@NotBlank String realm, @NotBlank String resource)
            throws NoSuchServiceException {
        logger.debug("find service with resource {} for realm {}", String.valueOf(resource), String.valueOf(realm));

        ServiceEntity s = serviceRepository.findByRealmAndResource(realm, resource);
        if (s == null) {
            throw new NoSuchServiceException();
        }

        return loadService(s);
    }

    @Transactional(readOnly = true)
    public List<ApiService> listServices() {
        List<ServiceEntity> services = serviceRepository.findAll();
        return services.stream().map(s -> loadService(s)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApiService> listServices(String realm) {
        logger.debug("list services for realm {}", String.valueOf(realm));

        List<ServiceEntity> services = serviceRepository.findByRealm(realm);
        return services.stream()
                .map(s -> loadService(s))
                .collect(Collectors.toList());
    }

    public ApiService addService(@NotBlank String realm, @Nullable String serviceId, @NotNull ApiService reg)
            throws RegistrationException {
        logger.debug("add service with id {} for realm {}", String.valueOf(serviceId), String.valueOf(realm));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        if (!StringUtils.hasText(realm)) {
            throw new MissingDataException("realm");
        }

        if (!StringUtils.hasText(serviceId)) {
            serviceId = reg.getServiceId();
        }

        if (!StringUtils.hasText(serviceId)) {
            serviceId = generateId();
        }

        String namespace = reg.getNamespace();
        String resource = reg.getResource();
        String name = reg.getName();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }

        if (!StringUtils.hasText(namespace)) {
            throw new MissingDataException("namespace");
        }

        if (!StringUtils.hasText(resource)) {
            throw new MissingDataException("resource");
        }

        if (!StringUtils.hasText(name)) {
            name = resource;
        }

        // validate
        if (resource.startsWith("aac.") || resource.startsWith(baseUrl)) {
            throw new InvalidDataException("resource");
        }
        if (namespace.startsWith("aac.") || namespace.startsWith(baseUrl)) {
            throw new InvalidDataException("namespace");
        }

        // resource indicator must be an uri
        if (!isValidUri(resource)) {
            throw new InvalidDataException("resource");
        }

        String title = reg.getTitle();
        if (StringUtils.hasText(title)) {
            title = Jsoup.clean(title, Safelist.none());
        }

        String description = reg.getDescription();
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        String userClaimsExtractor = reg.getUserClaimsExtractor() != null ? reg.getUserClaimsExtractor() : "";
        String clientClaimsExtractor = reg.getClientClaimsExtractor() != null ? reg.getClientClaimsExtractor() : "";

        // TODO validate extractors
        Map<String, String> claimMappings = new HashMap<>();
        claimMappings.put(SubjectType.USER.getValue(), userClaimsExtractor);
        claimMappings.put(SubjectType.CLIENT.getValue(), clientClaimsExtractor);

        String webhookExtractor = reg.getWebhookExtractor();
        if (StringUtils.hasText(webhookExtractor)) {
            if (!isValidUrl(webhookExtractor)) {
                throw new InvalidDataException("webhook");
            }
        } else {
            webhookExtractor = null;
        }

        // check for collisions
        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se != null) {
            throw new AlreadyRegisteredException();
        }

        se = serviceRepository.findByRealmAndNamespace(realm, namespace);
        if (se != null) {
            throw new DuplicatedDataException("namespace");
        }

        se = serviceRepository.findByRealmAndResource(realm, namespace);
        if (se != null) {
            throw new DuplicatedDataException("resource");
        }

        // create subject
        // we let services approve users/clients so we need a valid subject
        subjectService.addSubject(serviceId, realm, SystemKeys.RESOURCE_SERVICE, name);

        se = new ServiceEntity();
        se.setServiceId(serviceId);
        se.setRealm(realm);

        se.setResource(resource);
        se.setNamespace(namespace);

        se.setName(name);
        se.setTitle(title);
        se.setDescription(description);

        se.setClaimMappings(claimMappings);
        se.setClaimWebhook(webhookExtractor);

        logger.debug("save service {} for realm {}", serviceId, realm);
        if (logger.isTraceEnabled()) {
            logger.trace("service: {}", String.valueOf(reg));
        }

        se = serviceRepository.save(se);

        ApiService service = toService(se);

        // related entities
        if (reg.getScopes() != null) {
            List<ApiServiceScope> scopes = reg.getScopes().stream()
                    .map(s -> {
                        try {
                            return addScope(service.getServiceId(), s.getScopeId(), s);
                        } catch (NoSuchServiceException | RegistrationException e) {
                            // ignore
                            return null;
                        }
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            service.setScopes(scopes);
        }

        if (reg.getClaims() != null) {
            List<ApiServiceClaimDefinition> claims = reg.getClaims().stream()
                    .map(c -> {
                        try {
                            return addClaim(service.getServiceId(), c.getClaimId(), c);
                        } catch (NoSuchServiceException | RegistrationException e) {
                            // ignore
                            return null;
                        }
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            service.setClaims(claims);
        }

        return service;
    }

    public ApiService updateService(@NotBlank String serviceId, @NotNull ApiService reg)
            throws NoSuchServiceException, RegistrationException {
        logger.debug("update service with id {}", String.valueOf(serviceId));

        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        String realm = se.getRealm();
        String namespace = reg.getNamespace();
        String resource = reg.getResource();
        String name = reg.getName();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }

        if (!StringUtils.hasText(namespace)) {
            throw new MissingDataException("namespace");
        }

        if (!StringUtils.hasText(resource)) {
            throw new MissingDataException("resource");
        }

        if (!StringUtils.hasText(name)) {
            name = resource;
        }

        // validate
        if (resource.startsWith("aac.") || resource.startsWith(baseUrl)) {
            throw new InvalidDataException("resource");
        }
        if (namespace.startsWith("aac.") || namespace.startsWith(baseUrl)) {
            throw new InvalidDataException("namespace");
        }

        // resource indicator must be an uri
        if (!isValidUri(resource)) {
            throw new InvalidDataException("resource");
        }

        String title = reg.getTitle();
        if (StringUtils.hasText(title)) {
            title = Jsoup.clean(title, Safelist.none());
        }

        String description = reg.getDescription();
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        String userClaimsExtractor = reg.getUserClaimsExtractor() != null ? reg.getUserClaimsExtractor() : "";
        String clientClaimsExtractor = reg.getClientClaimsExtractor() != null ? reg.getClientClaimsExtractor() : "";

        // TODO validate extractors
        Map<String, String> claimMappings = new HashMap<>();
        claimMappings.put(SubjectType.USER.getValue(), userClaimsExtractor);
        claimMappings.put(SubjectType.CLIENT.getValue(), clientClaimsExtractor);

        String webhookExtractor = reg.getWebhookExtractor();
        if (StringUtils.hasText(webhookExtractor)) {
            if (!isValidUrl(webhookExtractor)) {
                throw new InvalidDataException("webhook");
            }
        } else {
            webhookExtractor = null;
        }

        // check for collisions if required
        if (!se.getNamespace().equals(namespace)) {
            ServiceEntity s = serviceRepository.findByRealmAndNamespace(realm, namespace);
            if (s != null) {
                throw new DuplicatedDataException("namespace");
            }
        }
        if (!se.getResource().equals(resource)) {
            ServiceEntity s = serviceRepository.findByRealmAndResource(realm, namespace);
            if (s != null) {
                throw new DuplicatedDataException("resource");
            }
        }

        se.setName(name);
        se.setDescription(description);
        se.setClaimMappings(claimMappings);
        se.setClaimWebhook(webhookExtractor);

        logger.debug("save service {} for realm {}", serviceId, realm);
        if (logger.isTraceEnabled()) {
            logger.trace("service: {}", String.valueOf(se));
        }

        se = serviceRepository.save(se);

        ApiService service = toService(se);

        // related entities
        if (reg.getScopes() != null) {
            // fetch current
            List<ApiServiceScope> curScopes = findScopes(serviceId);

            // update from registration
            List<ApiServiceScope> scopes = reg.getScopes().stream()
                    .map(s -> {
                        try {
                            return addOrUpdateScope(service.getServiceId(), s.getScopeId(), s);
                        } catch (NoSuchServiceException | RegistrationException e) {
                            // ignore
                            return null;
                        }
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            // remove orphans
            List<ApiServiceScope> toRemove = curScopes.stream()
                    .filter(s -> scopes.contains(s))
                    .collect(Collectors.toList());
            toRemove.forEach(s -> {
                deleteScope(s.getScopeId());
            });

            service.setScopes(scopes);
        }

        if (reg.getClaims() != null) {
            // fetch current
            List<ApiServiceClaimDefinition> curClaims = findClaims(serviceId);

            // update from registration
            List<ApiServiceClaimDefinition> claims = reg.getClaims().stream()
                    .map(c -> {
                        try {
                            return addOrUpdateClaim(service.getServiceId(), c.getClaimId(), c);
                        } catch (NoSuchServiceException | RegistrationException e) {
                            // ignore
                            return null;
                        }
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            // remove orphans
            List<ApiServiceClaimDefinition> toRemove = curClaims.stream()
                    .filter(c -> claims.contains(c))
                    .collect(Collectors.toList());
            toRemove.forEach(c -> {
                deleteClaim(c.getClaimId());
            });

            service.setClaims(claims);
        }

        return service;
    }

    public void deleteService(String serviceId) throws NoSuchServiceException {
        logger.debug("delete service with id {}", String.valueOf(serviceId));

        // delete related
        List<ApiServiceScope> scopes = findScopes(serviceId);
        if (!scopes.isEmpty()) {
            for (ApiServiceScope ss : scopes) {
                deleteScope(ss.getScopeId());
            }
        }

        List<ApiServiceClaimDefinition> claims = findClaims(serviceId);
        if (!claims.isEmpty()) {
            for (ApiServiceClaimDefinition sc : claims) {
                deleteClaim(sc.getClaimId());
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

    private ApiService toService(ServiceEntity se) {
        ApiService service = new ApiService(se.getServiceId());
        service.setServiceId(se.getServiceId());
        service.setRealm(se.getRealm());

        service.setResource(se.getResource());
        service.setNamespace(se.getNamespace());

        service.setName(se.getName());
        service.setName(se.getTitle());
        service.setDescription(se.getDescription());

        String userClaimsExtractor = se.getClaimMappings() != null
                && se.getClaimMappings().get(SubjectType.USER.getValue()) != null
                        ? se.getClaimMappings().get(SubjectType.USER.getValue())
                        : null;

        String clientClaimsExtractor = se.getClaimMappings() != null
                && se.getClaimMappings().get(SubjectType.CLIENT.getValue()) != null
                        ? se.getClaimMappings().get(SubjectType.CLIENT.getValue())
                        : null;

        service.setUserClaimsExtractor(userClaimsExtractor);
        service.setClientClaimsExtractor(clientClaimsExtractor);

        service.setWebhookExtractor(se.getClaimWebhook());

        return service;
    }

    private ApiService toService(ServiceEntity se,
            List<ServiceScopeEntity> scopes, List<ServiceClaimEntity> claims) {
        ApiService service = toService(se);

        service.setScopes(scopes.stream().map(s -> toScope(s)).collect(Collectors.toList()));
        service.setClaims(claims.stream().map(c -> toClaim(c)).collect(Collectors.toList()));
        return service;
    }

    /*
     * Service scopes
     */
    private ApiServiceScope toScope(ServiceScopeEntity s) {
        ApiServiceScope scope = new ApiServiceScope(s.getServiceId());
        scope.setScopeId(s.getScopeId());
        scope.setRealm(s.getRealm());
        scope.setScope(s.getScope());

        scope.setName(s.getName());
        scope.setDescription(s.getDescription());

        scope.setApprovalType(s.getApprovalType() != null ? SubjectType.parse(s.getApprovalType()) : null);
        scope.setApprovalFunction(s.getApprovalFunction());
        scope.setApprovalRequired(s.isApprovalRequired());
        scope.setApprovalAny(s.isApprovalAny());

        return scope;
    }

    private List<ServiceScopeEntity> fetchScopes(String serviceId) {
        return scopeRepository.findByServiceId(serviceId);
    }

    @Transactional(readOnly = true)
    public ApiServiceScope findScopeByScope(String realm, String scope) throws NoSuchServiceException {
        logger.debug("find scope {} for realm {}", String.valueOf(scope), String.valueOf(realm));

        ServiceScopeEntity s = scopeRepository.findByServiceIdAndScope(realm, scope);
        if (s == null) {
            return null;
        }

        return toScope(s);
    }

    @Transactional(readOnly = true)
    public ApiServiceScope findScope(@NotBlank String scopeId) {
        logger.debug("find scope with id {}", String.valueOf(scopeId));

        ServiceScopeEntity s = scopeRepository.findOne(scopeId);
        if (s == null) {
            return null;
        }

        return toScope(s);
    }

    @Transactional(readOnly = true)
    public ApiServiceScope getScope(@NotBlank String scopeId) throws NoSuchScopeException {
        logger.debug("get scope with id {}", String.valueOf(scopeId));

        ServiceScopeEntity s = scopeRepository.findOne(scopeId);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return toScope(s);
    }

    @Transactional(readOnly = true)
    public List<ApiServiceScope> findScopes(String serviceId) {
        logger.debug("find scopes for service {}", String.valueOf(serviceId));

        List<ServiceScopeEntity> list = scopeRepository.findByServiceId(serviceId);
        return list.stream().map(s -> toScope(s)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApiServiceScope> listScopes(String serviceId) throws NoSuchServiceException {
        logger.debug("list scopes for service {}", String.valueOf(serviceId));

        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        return findScopes(serviceId);
    }

    public ApiServiceScope addScope(
            @NotBlank String serviceId, @Nullable String scopeId, @NotNull ApiServiceScope reg)
            throws NoSuchServiceException, RegistrationException {
        logger.debug("add scope with id {} for service {}", String.valueOf(scopeId), String.valueOf(serviceId));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        if (!StringUtils.hasText(scopeId)) {
            scopeId = reg.getScopeId();
        }

        if (!StringUtils.hasText(scopeId)) {
            scopeId = generateId();
        }

        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        String scope = reg.getScope();
        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("invalid-scope");
        }
        scope = Jsoup.clean(scope, Safelist.none());
        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("invalid-scope");
        }

        String realm = service.getRealm();

        // check for collisions
        ServiceScopeEntity se = scopeRepository.findOne(scopeId);
        if (se != null) {
            throw new AlreadyRegisteredException();
        }

        se = scopeRepository.findByRealmAndScope(realm, scope);
        if (se != null) {
            throw new DuplicatedDataException("scope");
        }

        String name = reg.getName();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }

        if (!StringUtils.hasText(name)) {
            name = scope;
        }

        String description = reg.getDescription();
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        String approvalType = reg.getApprovalType() != null ? reg.getApprovalType().getValue() : null;
        String approvalRoles = reg.getApprovalRoles() != null
                ? StringUtils.collectionToCommaDelimitedString(reg.getApprovalRoles())
                : null;
        String approvalFunction = reg.getApprovalFunction();

        // validate
        if (scope.startsWith("aac.")) {
            throw new InvalidDataException("scope");
        }
        // TODO validate approval function?

        se = new ServiceScopeEntity();
        se.setScopeId(scopeId);
        se.setServiceId(serviceId);
        se.setRealm(realm);
        se.setScope(scope);

        se.setName(name);
        se.setDescription(description);

        se.setApprovalType(approvalType);
        se.setApprovalRoles(approvalRoles);
        se.setApprovalFunction(approvalFunction);

        se.setApprovalAny(reg.getApprovalAny());
        se.setApprovalRequired(reg.getApprovalRequired());

        logger.debug("save scope {} for realm {}", scopeId, realm);
        if (logger.isTraceEnabled()) {
            logger.trace("scope: {}", String.valueOf(se));
        }

        se = scopeRepository.save(se);
        return toScope(se);
    }

    public ApiServiceScope updateScope(@NotBlank String scopeId, @NotNull ApiServiceScope reg)
            throws NoSuchServiceException, NoSuchScopeException, RegistrationException {
        logger.debug("update scope with id {}", String.valueOf(scopeId));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        ServiceScopeEntity se = scopeRepository.findOne(scopeId);
        if (se == null) {
            throw new NoSuchScopeException();
        }

        String realm = se.getRealm();
        String scope = se.getScope();

        String serviceId = se.getServiceId();
        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }
        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // scope is immutable because we may have already released tokens with the value
        if (reg.getScope() != null && !scope.equals(reg.getScope())) {
            throw new RegistrationException("invalid-scope");
        }

        String name = reg.getName();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }

        if (!StringUtils.hasText(name)) {
            name = scope;
        }

        String description = reg.getDescription();
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        String approvalType = reg.getApprovalType() != null ? reg.getApprovalType().getValue() : null;
        String approvalRoles = reg.getApprovalRoles() != null
                ? StringUtils.collectionToCommaDelimitedString(reg.getApprovalRoles())
                : null;
        String approvalFunction = reg.getApprovalFunction();
        // TODO validate approval function?

        se.setName(name);
        se.setDescription(description);

        se.setApprovalType(approvalType);
        se.setApprovalRoles(approvalRoles);
        se.setApprovalFunction(approvalFunction);

        se.setApprovalAny(reg.getApprovalAny());
        se.setApprovalRequired(reg.getApprovalRequired());

        logger.debug("save scope {} for realm {}", scopeId, realm);
        if (logger.isTraceEnabled()) {
            logger.trace("scope: {}", String.valueOf(se));
        }

        se = scopeRepository.save(se);
        return toScope(se);
    }

    public ApiServiceScope addOrUpdateScope(
            @NotBlank String serviceId, @Nullable String scopeId, @NotNull ApiServiceScope reg)
            throws NoSuchServiceException, RegistrationException {
        logger.debug("add or update scope with id {} for service {}", String.valueOf(scopeId),
                String.valueOf(serviceId));
        if (scopeId == null) {
            scopeId = reg.getScopeId();
        }

        if (scopeId == null) {
            return addScope(serviceId, null, reg);
        }

        ServiceScopeEntity se = scopeRepository.findOne(scopeId);
        if (se == null) {
            return addScope(serviceId, scopeId, reg);
        } else {
            try {
                return updateScope(scopeId, reg);
            } catch (NoSuchScopeException e) {
                // error
                throw new RegistrationException();
            }
        }
    }

    public void deleteScope(@NotBlank String scopeId) {
        logger.debug("delete scope with id {}", String.valueOf(scopeId));

        ServiceScopeEntity s = scopeRepository.findOne(scopeId);
        if (s != null) {
            scopeRepository.delete(s);
        }

        // NOTE: we don't care about scope usage here
    }

    /*
     * Service claims
     */
    private ApiServiceClaimDefinition toClaim(ServiceClaimEntity c) {
        ApiServiceClaimDefinition claim = new ApiServiceClaimDefinition(c.getKey());
        claim.setClaimId(c.getClaimId());
        claim.setServiceId(c.getServiceId());
        claim.setRealm(c.getRealm());

        claim.setType(c.getType() != null ? AttributeType.parse(c.getType()) : null);
        claim.setIsMultiple(c.getMultiple());

        return claim;
    }

    private List<ServiceClaimEntity> fetchClaims(String serviceId) {
        return claimRepository.findByServiceId(serviceId);
    }

    @Transactional(readOnly = true)
    public ApiServiceClaimDefinition findClaim(@NotBlank String claimId) throws NoSuchClaimException {
        logger.debug("find claim with id {}", String.valueOf(claimId));

        ServiceClaimEntity sc = claimRepository.findOne(claimId);
        if (sc == null) {
            return null;
        }

        return toClaim(sc);
    }

    @Transactional(readOnly = true)
    public ApiServiceClaimDefinition getClaim(@NotBlank String claimId) throws NoSuchClaimException {
        logger.debug("find claim with id {}", String.valueOf(claimId));

        ServiceClaimEntity sc = claimRepository.findOne(claimId);
        if (sc == null) {
            throw new NoSuchClaimException();
        }

        return toClaim(sc);
    }

    @Transactional(readOnly = true)
    public List<ApiServiceClaimDefinition> findClaims(String serviceId) {
        logger.debug("find claims for service {}", String.valueOf(serviceId));

        List<ServiceClaimEntity> list = claimRepository.findByServiceId(serviceId);
        return list.stream().map(sc -> toClaim(sc)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApiServiceClaimDefinition> listClaims(String serviceId) throws NoSuchServiceException {
        logger.debug("list claims for service {}", String.valueOf(serviceId));

        ServiceEntity se = serviceRepository.findOne(serviceId);
        if (se == null) {
            throw new NoSuchServiceException();
        }

        return findClaims(serviceId);
    }

    public ApiServiceClaimDefinition addClaim(@NotBlank String serviceId, @Nullable String claimId,
            @NotNull ApiServiceClaimDefinition reg) throws NoSuchServiceException, RegistrationException {
        logger.debug("add claim with id {} for service {}", String.valueOf(claimId), String.valueOf(serviceId));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        if (!StringUtils.hasText(claimId)) {
            claimId = reg.getClaimId();
        }

        if (!StringUtils.hasText(claimId)) {
            claimId = generateId();
        }

        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }

        String key = reg.getKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("invalid-key");
        }
        key = Jsoup.clean(key, Safelist.none());
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("invalid-key");
        }

        String realm = service.getRealm();

        // check for collisions
        ServiceClaimEntity sc = claimRepository.findOne(claimId);
        if (sc != null) {
            throw new AlreadyRegisteredException();
        }

        sc = claimRepository.findByServiceIdAndKey(serviceId, key);
        if (sc != null) {
            throw new DuplicatedDataException("key");
        }

        String type = reg.getType() != null ? reg.getType().getValue() : AttributeType.STRING.getValue();

        sc = new ServiceClaimEntity();
        sc.setClaimId(claimId);
        sc.setServiceId(serviceId);
        sc.setRealm(realm);

        sc.setKey(key);
        sc.setType(type);
        sc.setMultiple(reg.getIsMultiple());

        logger.debug("save claim {} for realm {}", claimId, realm);
        if (logger.isTraceEnabled()) {
            logger.trace("claim: {}", String.valueOf(sc));
        }

        sc = claimRepository.save(sc);
        return toClaim(sc);
    }

    public ApiServiceClaimDefinition updateClaim(@NotBlank String claimId, @NotNull ApiServiceClaimDefinition reg)
            throws NoSuchServiceException, NoSuchClaimException, RegistrationException {
        logger.debug("update claim with id {}", String.valueOf(claimId));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        ServiceClaimEntity sc = claimRepository.findOne(claimId);
        if (sc == null) {
            throw new NoSuchClaimException();
        }

        String realm = sc.getRealm();
        String serviceId = sc.getServiceId();

        ServiceEntity service = serviceRepository.findOne(serviceId);
        if (service == null) {
            throw new NoSuchServiceException();
        }
        if (!realm.equals(service.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        String key = reg.getKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("invalid-key");
        }
        key = Jsoup.clean(key, Safelist.none());
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("invalid-key");
        }

        // check for collisions if required
        if (!sc.getKey().equals(key)) {
            ServiceClaimEntity s = claimRepository.findByServiceIdAndKey(serviceId, key);
            if (s != null) {
                throw new DuplicatedDataException("key");
            }
        }

        String type = reg.getType() != null ? reg.getType().getValue() : AttributeType.STRING.getValue();

        sc.setKey(key);
        sc.setType(type);
        sc.setMultiple(reg.getIsMultiple());

        logger.debug("save claim {} for realm {}", claimId, realm);
        if (logger.isTraceEnabled()) {
            logger.trace("claim: {}", String.valueOf(sc));
        }

        sc = claimRepository.save(sc);
        return toClaim(sc);
    }

    public ApiServiceClaimDefinition addOrUpdateClaim(
            @NotBlank String serviceId, @Nullable String claimId, @NotNull ApiServiceClaimDefinition reg)
            throws NoSuchServiceException, RegistrationException {
        logger.debug("add or update claim with id {} for service {}", String.valueOf(claimId),
                String.valueOf(serviceId));
        if (claimId == null) {
            claimId = reg.getClaimId();
        }

        if (claimId == null) {
            return addClaim(serviceId, null, reg);
        }

        ServiceClaimEntity sc = claimRepository.findOne(claimId);
        if (sc == null) {
            return addClaim(serviceId, claimId, reg);
        } else {
            try {
                return updateClaim(claimId, reg);
            } catch (NoSuchClaimException e) {
                // error
                throw new RegistrationException();
            }
        }
    }

    public void deleteClaim(@NotBlank String claimId) {
        logger.debug("delete claim with id {}", String.valueOf(claimId));

        ServiceClaimEntity sc = claimRepository.findOne(claimId);
        if (sc != null) {
            claimRepository.delete(sc);
        }

        // NOTE: we don't care about usage here
    }

    /*
     * Helpers
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }

    boolean isValidUrl(String url) {
        try {
            URL u = new URL(url);
            return u != null;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    boolean isValidUri(String uri) {
        try {
            URI u = new URI(uri);
            return u != null;
        } catch (URISyntaxException e) {
            return false;
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
