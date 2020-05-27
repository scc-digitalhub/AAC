/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.manager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.CLAIM_TYPE;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.Service;
import it.smartcommunitylab.aac.model.ServiceClaim;
import it.smartcommunitylab.aac.model.ServiceScope;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ServiceClaimRepository;
import it.smartcommunitylab.aac.repository.ServiceModelRepository;
import it.smartcommunitylab.aac.repository.ServiceScopeRepository;

/**
 * Manage service (OAuth2 'resource' concept) definitions: scopes, claims, etc.
 * Each service is associated with a set of scopes and claims. 
 * Custom services should have their own namespace that is unique.  
 * 
 * @author raman
 *
 */
@Component
@Transactional
public class ServiceManager {

	private static final String SERVICE_CONTEXT = "services";
	
    @Value("${admin.username}")
    private String adminUsername;   

	@Autowired
	private ServiceModelRepository serviceRepo;
	@Autowired
	private ServiceClaimRepository claimRepo;
	@Autowired
	private ServiceScopeRepository scopeRepo;
//	@Autowired
//	private RoleManager roleManager;
	
	/**
	 * Cache with scope -> claims mapping
	 */
	LoadingCache<String, Set<String>> claimCache = CacheBuilder
			.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
			.build(new CacheLoader<String,  Set<String>>() {
				@Override
				public  Set<String> load(String scope) throws Exception {
					ServiceScope scopeObj = getServiceScope(scope);
					if (scopeObj != null && !StringUtils.isEmpty(scopeObj.getClaims())) {
						String ns = scopeObj.getService().getNamespace();
						return StringUtils.commaDelimitedListToSet(scopeObj.getClaims())
								.stream().map(local -> ServiceClaim.qualifiedName(ns, local))
								.collect(Collectors.toSet());
						
					}
					return Collections.emptySet();
				}
				
			});
	
	/**
	 * Cache with scope -> claims mapping
	 */
	LoadingCache<String, ServiceDTO> serviceCache = CacheBuilder
			.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
			.build(new CacheLoader<String,  ServiceDTO>() {
				@Override
				public  ServiceDTO load(String scope) throws Exception {
					ServiceScope scopeObj = getServiceScope(scope);
					if (scopeObj != null) {
						ServiceDTO service = toDTO(scopeObj.getService());
						service.setScopes(getServiceScopes(service.getServiceId()));
						service.setClaims(getServiceClaims(service.getServiceId()));
						return service;
						
					}
					return null;
				}
				
			});


//	@PostConstruct
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void init() throws Exception {
        ServiceDTO[] services = new ObjectMapper().readValue(
                getClass().getResourceAsStream("services.json"),
                ServiceDTO[].class);
        
        // do not delete obsolete ones: do it via console.
        for (ServiceDTO serviceDTO : services) {
            // update existing or create new ones
            Service service = saveService(serviceDTO);
            
            if (serviceDTO.getClaims() != null) {
                serviceDTO.getClaims().forEach(claim -> {
                    ServiceClaim duplicate = claimRepo.findByServiceAndClaim(serviceDTO.getServiceId(),
                            claim.getClaim());
                    if (duplicate == null) {
                        saveServiceClaim(service, claim);
                    }
                });
            }
            
            if (serviceDTO.getScopes() != null) {
                serviceDTO.getScopes().forEach(scope -> saveServiceScope(service, scope));
            }
        }
    }
	
	/**
	 * Read all services available
	 * @param pageable
	 * @return
	 */
	public Page<ServiceDTO> getAllServices(String name, Pageable pageable) {
		Page<Service> page = null;
		if (StringUtils.isEmpty(name)) {
			page = serviceRepo.findAll(pageable);
		} else {
			page = serviceRepo.findByName(name, pageable);
		}
		return page.map(s -> toDTO(s));
	}


	/**
	 * @return
	 */
	public List<ServiceScopeDTO> getAllScopes() {
		return scopeRepo.findAll().stream().map(s -> toDTO(s)).collect(Collectors.toList());
	}

	public ServiceDTO getService(String serviceId) {
		return toDTO(serviceRepo.findOne(serviceId.toLowerCase()));
	}
	/**
	 * Read all services the user can manage
	 * @param userId
	 * @return
	 */
	public List<ServiceDTO> getUserServices(User user) {
		Set<String> contexts = getUserContexts(user);
		boolean withNull = contexts.contains(null);
		contexts.remove(null);
		List<Service> list = null;
		if (withNull && contexts.isEmpty()) {
			list = serviceRepo.findByContext(null);
		} else if (contexts.isEmpty()) {
			list = Collections.emptyList();
		} else {
			list = serviceRepo.findByContexts(contexts, withNull);
		}
		return list.stream().map(s -> toDTO(s)).collect(Collectors.toList());
	}

	/**
	 * Extract user-owned contexts for service management
	 * @param user
	 * @return
	 */
	public Set<String> getUserContexts(User user) {
		Role sysadmin = Role.systemAdmin();
		Set<String> contexts = user.getRoles()
				.stream()
				.filter(r -> r.equals(sysadmin) || r.getContext() != null && r.getRole().equals(Config.R_PROVIDER) && r.getContext().equals(SERVICE_CONTEXT))
				.map(r -> r.getSpace()).collect(Collectors.toSet());
		return contexts;
	}

	/**
	 * Save service object, not claims and scopes.
	 * @param userId
	 * @param service
	 * @return
	 */
    public ServiceDTO saveService(User user, ServiceDTO dto) {
        Set<String> contexts = getUserContexts(user);
        if (!contexts.contains(dto.getContext())) {
            throw new SecurityException("Invalid context: " + dto.getContext());
        }
        return toDTO(saveService(dto));
    }

    private Service saveService(ServiceDTO dto) {
        Service service = serviceRepo.findOne(dto.getServiceId().toLowerCase());
        if (dto.getNamespace() != null) {
            dto.setNamespace(dto.getNamespace().trim());
        }

        if (service != null) {
            // if previous namespace was null (core service), keep that namespace
            if (service.getNamespace() == null) {
                dto.setNamespace(null);
            }
        }
        service = toService(dto);
        validateServiceData(service);
        // namespace are unique for services (if defined)
        if (dto.getNamespace() != null) {
            Service nsService = serviceRepo.findByNamespace(dto.getNamespace());
            if (nsService != null && !nsService.getServiceId().equals(service.getServiceId())) {
                throw new IllegalArgumentException("Duplicate service namespace: " + dto.getNamespace());
            }
        }
        return serviceRepo.save(service);
    }
	
	/**
	 * Delete specified service
	 * @param user
	 * @param serviceId
	 */
	public void deleteService(User user, String serviceId) {
		Service service = serviceRepo.findOne(serviceId.toLowerCase());
		if (service != null) {
			Set<String> contexts = getUserContexts(user);
			if (!contexts.contains(service.getContext())) {
				throw new SecurityException("Unauthorized operation for service: " + serviceId);
			}
			Set<String> scopes = getServiceScopes(serviceId).stream().map(s -> s.getScope()).collect(Collectors.toSet());
			serviceRepo.delete(serviceId.toLowerCase());
			// refresh claim cache
			scopes.forEach(s -> resetCache(s));
		}
		// TODO - what to do with the client scope/resource associations?
	}

	/**
	 * 
	 * List scopes of the specified service
	 * @param serviceId
	 * @return
	 */
	public List<ServiceScopeDTO> getServiceScopes(String serviceId) {
		return scopeRepo.findByService(serviceId).stream().map(s -> toDTO(s)).collect(Collectors.toList());
	} 
	/**
	 * List claims of the specified service
	 * @param serviceId
	 * @return
	 */
	public List<ServiceClaimDTO> getServiceClaims(String serviceId) {
		return claimRepo.findByService(serviceId).stream().map(s -> toDTO(s)).collect(Collectors.toList());
	}
	
	/**
	 * List claims of the specified service
	 * @param serviceId
	 * @return
	 */
	public List<ServiceClaim> getServiceClaimsDB(String serviceId) {
		return claimRepo.findByService(serviceId);
	}
	
	/**
	 * Save service scope definition
	 * @param user
	 * @param serviceId
	 * @param scope
	 * @return
	 */
	public ServiceScopeDTO saveServiceScope(User user, String serviceId, ServiceScopeDTO dto) {
        Service service = serviceRepo.findOne(serviceId.toLowerCase());
        if (service == null) {
            throw new SecurityException("Unknown service: " + serviceId);
        }
        Set<String> contexts = getUserContexts(user);
        if (!contexts.contains(service.getContext())) {
            throw new SecurityException("Unauthorized operation for scope: " + serviceId +", scope: " + dto.getScope());
        }
	    
        return toDTO(saveServiceScope(service, dto));
	}
	
    private ServiceScope saveServiceScope(Service service, ServiceScopeDTO dto) {
        String serviceId = service.getServiceId();
        dto.setServiceId(serviceId);
        ServiceScope scopeObj = scopeRepo.findOne(dto.getScope().toLowerCase());
        Set<String> availableClaims = claimRepo.findByService(serviceId.toLowerCase()).stream().map(c -> c.getClaim()).collect(Collectors.toSet());
        dto.setClaims(dto.getClaims().stream().filter(c -> availableClaims.contains(c.toLowerCase())).collect(Collectors.toList()));
        if (scopeObj != null && !serviceId.toLowerCase().equals(scopeObj.getService().getServiceId())) {
            throw new SecurityException("Unauthorized operation for scope: " + serviceId +", scope: " + dto.getScope());
        }
        ServiceScope scope = toScope(dto);
        validateScopeData(scope);
        scope.setService(service);
        ServiceScope saved = scopeRepo.save(scope);
        // refresh claim cache
        refreshCache(scope.getScope());
        return saved;
    }	

	/**
	 * Save service scope definition
	 * @param user
	 * @param serviceId
	 * @param scope
	 * @return
	 */
    public ServiceClaimDTO saveServiceClaim(User user, String serviceId, ServiceClaimDTO dto) {
        Service service = serviceRepo.findOne(serviceId.toLowerCase());
        if (service == null) {
            throw new SecurityException("Unknown service: " + serviceId);
        }
        Set<String> contexts = getUserContexts(user);
        if (!contexts.contains(service.getContext())) {
            throw new SecurityException(
                    "Unauthorized operation for claim: " + serviceId + ", claim: " + dto.getClaim());
        }

        return toDTO(saveServiceClaim(service, dto));

    }
	
    private ServiceClaim saveServiceClaim(Service service, ServiceClaimDTO dto) {
        String serviceId = service.getServiceId();
        ServiceClaim old = null;
        if (dto.getClaimId() != null) old = claimRepo.findOne(dto.getClaimId());
        ServiceClaim duplicate = claimRepo.findByServiceAndClaim(serviceId, dto.getClaim());
        if (duplicate != null && (old == null || !old.getClaimId().equals(duplicate.getClaimId()))) {
            throw new IllegalArgumentException("Duplicate claim for service: " + serviceId +", claim: " + dto.getClaim());
        }
        
        dto.setServiceId(serviceId);

        ServiceClaim claim = toClaim(dto);
        claim.setService(service);
        if (old != null) {
            claim.setClaimId(old.getClaimId());
        }
        validateClaimData(claim);
        ServiceClaim saved = claimRepo.save(claim);
        //refresh cache
        getServiceScopes(serviceId)
            .stream()
            .forEach(s -> refreshCache(s.getScope()));

        return saved;  
    }
    
	/**
	 * Delete specified scope
	 * @param user
	 * @param serviceId
	 * @param scope
	 */
	public void deleteServiceScope(User user, String serviceId, String scope) {
		serviceId = serviceId.toLowerCase();
		scope = scope.toLowerCase();
		Service service = serviceRepo.findOne(serviceId);
		ServiceScope scopeObj = scopeRepo.findOne(scope);
		if (service != null && scopeObj != null) {
			Set<String> contexts = getUserContexts(user);
			if (!contexts.contains(service.getContext()) || !serviceId.equals(scopeObj.getService().getServiceId())) {
				throw new SecurityException("Unauthorized operation for scope: " + serviceId +", scope: " + scope);
			}
			scopeRepo.delete(scopeObj);
			resetCache(scope.toLowerCase());
		}
		// TODO - what to do with the client scope association?
	}

	/**
	 * Delete specified claim
	 * @param user
	 * @param serviceId
	 * @param claim
	 */
	public void deleteServiceClaim(User user, String serviceId, String claim) {
		Service service = serviceRepo.findOne(serviceId.toLowerCase());
		ServiceClaim claimObj = claimRepo.findByServiceAndClaim(serviceId, claim);
		if (service != null && claimObj != null) {
			Set<String> contexts = getUserContexts(user);
			if (!contexts.contains(service.getContext())) {
				throw new SecurityException("Unauthorized operation for claim: " + serviceId +", claim: " + claim);
			}
			claimRepo.delete(claimObj);
			getServiceScopes(serviceId)
			.stream()
			.forEach(s -> resetCache(s.getScope()));
		}
	}

	/**
	 * @param requestedScope
	 * @return
	 */
	public ServiceScope getServiceScope(String requestedScope) {
		return scopeRepo.findOne(requestedScope.toLowerCase());
	}
	
	public ServiceScopeDTO getServiceScopeDTO(String requestedScope) {
	    ServiceScope service = getServiceScope(requestedScope);
		return service != null ? toDTO(service) : null;
	}

	/**
	 * @param scopes
	 * @return
	 */
	public Set<String> findServiceIdsByScopes(Set<String> scopes) {
		if (scopes == null) return Collections.emptySet();
		return serviceRepo.findServiceIdsByScopes(scopes.stream().map(s -> s.toLowerCase()).collect(Collectors.toSet()));
	}

	/**
	 * @return
	 */
	public List<ServiceScope> findAllScopes() {
		return scopeRepo.findAll();
	}
	
	/**
	 * Get claims associated to the specified scopes
	 * @param scopes
	 * @return
	 */
	public Set<String> getClaimsForScopes(Set<String> scopes) {
		return scopes == null
				? Collections.emptySet() 
				: scopes.stream().flatMap(s -> {
					try {
						return claimCache.get(s).stream();
					} catch (ExecutionException e) {
						return Collections.<String>emptySet().stream();
					}
				}).collect(Collectors.toSet());
	}
	
	/**
	 * Return service definition for the service defining the scope
	 * @param scope
	 * @return
	 */
	public ServiceDTO getScopeService(String scope) {
		try {
			return scope == null
					? null 
					: serviceCache.get(scope);
		} catch (ExecutionException e) {
			return null;
		}
	}

    private void refreshCache(String scope) {
        claimCache.refresh(scope);
        serviceCache.refresh(scope);
    }
    
	private void resetCache(String scope) {
		claimCache.invalidate(scope);
		serviceCache.invalidate(scope);
	}
	
	private ServiceDTO toDTO(Service service) {
		if (service == null) return null;
		
		ServiceDTO dto = new ServiceDTO();
		dto.setClaimMapping(service.getClaimMapping());
		dto.setContext(service.getContext());
		dto.setDescription(service.getDescription());
		dto.setName(service.getName());
		dto.setNamespace(service.getNamespace());
		dto.setServiceId(service.getServiceId());
		return dto;
	}
	private Service toService(ServiceDTO dto) {
		Service service = new Service();
		service.setClaimMapping(dto.getClaimMapping());
		if (dto.getContext() != null) service.setContext(dto.getContext().trim());
		service.setDescription(dto.getDescription());
		service.setName(dto.getName());
		if (dto.getNamespace() != null) {
			service.setNamespace(dto.getNamespace().trim().toLowerCase());
		}
		service.setServiceId(dto.getServiceId().toLowerCase());
		return service;
	}

	private ServiceScopeDTO toDTO(ServiceScope scope) {
		ServiceScopeDTO dto = new ServiceScopeDTO();
		dto.setApprovalRequired(scope.isApprovalRequired());
		dto.setAuthority(scope.getAuthority());
		if (scope.getClaims() != null) {
			dto.setClaims(new LinkedList<>(StringUtils.commaDelimitedListToSet(scope.getClaims())));
		}
		dto.setDescription(scope.getDescription());
		dto.setName(scope.getName());
		if (scope.getRoles() != null) {
			dto.setRoles(new LinkedList<>(StringUtils.commaDelimitedListToSet(scope.getRoles())));
		}
		dto.setScope(scope.getScope());
		dto.setServiceId(scope.getService().getServiceId());
		return dto;
	} 
	private ServiceClaimDTO toDTO(ServiceClaim obj) {
		ServiceClaimDTO res = new ServiceClaimDTO();
		res.setClaim(obj.getClaim());
		res.setClaimId(obj.getClaimId());
		res.setMultiple(obj.isMultiple());
		res.setName(obj.getName());
		res.setServiceId(obj.getService().getServiceId());
		res.setType(obj.getType().getLitType());
		return res;
	} 
	private ServiceScope toScope(ServiceScopeDTO dto) {
		ServiceScope scope = new ServiceScope();
		scope.setApprovalRequired(dto.isApprovalRequired());
		scope.setAuthority(dto.getAuthority());
		if (dto.getClaims() != null) {
			scope.setClaims(StringUtils.collectionToCommaDelimitedString(dto.getClaims()));
		}
		scope.setDescription(dto.getDescription());
		scope.setName(dto.getName());
		if (dto.getRoles() != null) {
			scope.setRoles(StringUtils.collectionToCommaDelimitedString(dto.getRoles()));
		}
		scope.setScope(dto.getScope().trim().toLowerCase());
		return scope;
	}
	private ServiceClaim toClaim(ServiceClaimDTO obj) {
		ServiceClaim res = new ServiceClaim();
		res.setClaim(obj.getClaim().trim().toLowerCase());
		res.setClaimId(obj.getClaimId());
		res.setMultiple(obj.isMultiple());
		res.setName(obj.getName());
		res.setType(CLAIM_TYPE.get(obj.getType()));
		return res;
	}

	/**
	 * Validate service fields
	 * @param service
	 */
	private void validateServiceData(Service service) {
		if (!StringUtils.hasText(service.getServiceId())) {
			throw new IllegalArgumentException("empty service ID");
		}
		if (!service.getServiceId().matches("[\\w\\.-]+")) {
			throw new IllegalArgumentException("Invalid service ID value: only alpha-numeric characters and '_.-' allowed");
		}
		// namespace can be empty only for default services
		if (!StringUtils.isEmpty(service.getContext()) && !StringUtils.hasText(service.getNamespace())) {
			throw new IllegalArgumentException("empty namespace");
		}
		if (StringUtils.hasText(service.getNamespace()) && !service.getNamespace().matches("[\\w\\.-]+")) {
			throw new IllegalArgumentException("Invalid service namespace value: only alpha-numeric characters and '_.-' allowed");
		}
		
		if (!StringUtils.hasText(service.getName())) {
			throw new IllegalArgumentException("empty service name");
		}
		if (!StringUtils.hasText(service.getDescription())) {
			throw new IllegalArgumentException("empty service description");
		}
	}

	/**
	 * @param scope
	 */
	private void validateScopeData(ServiceScope scope) {
		if (!StringUtils.hasText(scope.getScope())) {
			throw new IllegalArgumentException("empty resource mapping ID");
		}
		if (!scope.getScope().matches("[\\w\\.-]+")) {
			throw new IllegalArgumentException("Invalid service scope value: only alpha-numeric characters and '_.-' allowed");
		};
		if (!StringUtils.hasText(scope.getName())) {
			throw new IllegalArgumentException("empty resource mapping name");
		}
		if (!StringUtils.hasText(scope.getDescription())) {
			throw new IllegalArgumentException("empty resource mapping description");
		}
	}

	/**
	 * @param scope
	 */
	private void validateClaimData(ServiceClaim claim) {
		if (!StringUtils.hasText(claim.getClaim())) {
			throw new IllegalArgumentException("empty resource mapping ID");
		}
		if (!claim.getClaim().matches("[\\w\\.-]+")) {
			throw new IllegalArgumentException("Invalid service scope value: only alpha-numeric characters and '_.-' allowed");
		}if (!StringUtils.hasText(claim.getName())) {
			throw new IllegalArgumentException("empty resource mapping name");
		}
	}

	
	
}
