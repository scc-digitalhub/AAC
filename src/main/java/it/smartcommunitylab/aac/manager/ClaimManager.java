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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nimbusds.jwt.JWTClaimsSet;

import delight.graaljssandbox.GraalSandbox;
import delight.graaljssandbox.GraalSandboxes;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.CLAIM_TYPE;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.dto.ClientClaimProfileDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.UserClaimProfileDTO;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientClaim;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.ServiceClaim;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.model.UserClaim;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.repository.ClientClaimRepository;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserClaimRepository;

/**
 * @author raman
 *
 */
@Component
@Transactional
public class ClaimManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
	
    @Autowired
    private UserManager userManager;
	@Autowired
	private ProfileManager profileManager;
	@Autowired
	private ServiceManager serviceManager;
	@Autowired
	private UserClaimRepository userClaimRepository;
	@Autowired
	private ClientClaimRepository clientClaimRepository;
    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

	private Set<String> profileScopes = Sets.newHashSet(Config.SCOPE_BASIC_PROFILE);
	private Set<String> accountScopes = Sets.newHashSet(Config.SCOPE_ACCOUNT_PROFILE);
	private Set<String> roleScopes = Sets.newHashSet(Config.SCOPE_ROLE, Config.SCOPE_GROUP);

	// claims that should not be overwritten
	private Set<String> reservedScopes = JWTClaimsSet.getRegisteredNames();
	private Set<String> systemScopes = Sets.newHashSet("scope", "token_type", "client_id", "active", "roles", "groups", "username", "user_name", "space");

	private static final ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * Create user claims map considering the authorized scopes, authorized claims and request claims.
	 * The claims are constructed based on the user roles, user info, and accounts info 
	 * @param user
	 * @param authorities
	 * @param client
	 * @param scopes
	 * @param authorizedClaims
	 * @param requestedClaims
	 * @return
	 */
	
   public Map<String, Object> getUserClaims(String userId, Collection<? extends GrantedAuthority> authorities, ClientDetailsEntity client, Set<String> scopes) {
       return getUserClaims(userId, authorities, client, scopes, null, null);
   }	
	
    @Transactional(readOnly = true)
    public Map<String, Object> getUserClaims(String userId, Collection<? extends GrantedAuthority> authorities,
            ClientDetailsEntity client, Set<String> scopes, Set<String> authorizedClaims, Set<String> requestedClaims) {
        // TODO handle authorized/requested claims in caller
        ClientAppInfo appInfo = ClientAppInfo.convert(client.getAdditionalInformation());
        try {
            return createUserClaims(userId, authorities, appInfo.getClaimMapping(), scopes, authorizedClaims,
                    requestedClaims, appInfo.getUniqueSpaces(), appInfo.getRolePrefixes(), true, null);
        } catch (InvalidDefinitionException e) {
            // never arrives here
            return null;
        }
    }

	/**
	 * Return claims explicitly associated to the client application
	 * @param clientId
	 * @param scopes
	 * @return
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getClientClaims(String clientId, Set<String> scopes) {
		List<ClientClaim> claims = clientClaimRepository.findByClient(clientId);
		if (claims.isEmpty()) return Collections.emptyMap();
		
		Map<String, Object> res = new HashMap<>();
		Set<String> allowedByScope = getClaimsForScopeSet(scopes);
		Set<String> authorizedByClaims = Collections.emptySet();
		Set<String> requestedByClaims = Collections.emptySet();

		for (ClientClaim claim: claims) {
			String qname = ServiceClaim.qualifiedName(claim.getClaim().getService().getNamespace(), claim.getClaim().getClaim());
			if (claimAllowed(qname, allowedByScope, authorizedByClaims, requestedByClaims)) {
				res.put(qname, ServiceClaim.typedValue(claim.getClaim(), claim.getValue()));
			}
		}
		return res;
	}
	
	
	/**
	 * Create user claims map considering the authorized scopes.
	 * The claims are constructed based on the user roles, user info, and accounts info 
	 * @param user
	 * @param appInfo
	 * @param scopes
	 * @param authorizedClaims
	 * @param requestedClaims
	 * @return
	 * @throws InvalidDefinitionException 
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> validateUserClaimsForClientApp(User user, ClientAppInfo appInfo, Set<String> scopes) throws InvalidDefinitionException {
		return createUserClaims(user.getId().toString(), user.getRoles(), appInfo.getClaimMapping(), scopes, null, null, appInfo.getUniqueSpaces(), appInfo.getRolePrefixes(), false, null);
	}
		
	/**
	 * Validate service claim mapping definition. Check if and only if the valid claims are produced 
	 * @param user
	 * @param serviceId
	 * @param mapping
	 * @param scopes
	 * @return
	 * @throws InvalidDefinitionException
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> validateClaimMapping(User user, String serviceId, String mapping, Set<String> scopes) throws InvalidDefinitionException {
		Map<String, Object> res = createUserClaims(user.getId().toString(), user.getRoles(), mapping, scopes, null, null, null, null, false, serviceId);
		ServiceDTO service = serviceManager.getService(serviceId);
		service.setClaims(serviceManager.getServiceClaims(serviceId));
		validateMappedClaims(res, service);
		return res;
	}
	
	
	/**
	 * Get users with the stored claims for the specified service 
	 * @param owner
	 * @param serviceId
	 * @param name
	 * @param page
	 * @return
	 */
	@Transactional(readOnly = true)
	public Page<UserClaimProfileDTO> getServiceUserClaims(User owner, String serviceId, String name, Pageable page) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return new PageImpl<>(Collections.emptyList());
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		if (name == null) name = "";
		Page<Object[]> userData = userClaimRepository.findUserDataByService(serviceId.toLowerCase(), name.toLowerCase(), page);
		return userData.map(arr -> new UserClaimProfileDTO(arr[0] == null ? null: ((Long)arr[0]).toString(), (String)arr[1]));
	}


	/**
	 * Get custom claims of the user for the specified service
	 * @param owner
	 * @param serviceId
	 * @param userId
	 * @return
	 */
	@Transactional(readOnly = true)
	public UserClaimProfileDTO getServiceUserClaims(User owner, String serviceId, String userId) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return null;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		List<UserClaim> claims = userClaimRepository.findByUserAndService(Long.parseLong(userId), serviceId);
		UserClaimProfileDTO user = new UserClaimProfileDTO();
		User dbUser = claims.get(0).getUser();

		if (claims.isEmpty()) {
			dbUser = userManager.findOne(Long.parseLong(userId)); 
		} else {
			dbUser = claims.get(0).getUser();
		}
		user.setUserId(dbUser.getId().toString());
		user.setUsername(dbUser.getUsername());
		user.setClaims(new HashMap<>());
		claims.forEach(uc -> user.getClaims().put(ServiceClaim.qualifiedName(service.getNamespace(), uc.getClaim().getClaim()), ServiceClaim.typedValue(uc.getClaim(), uc.getValue())));
		return user;
		
	}
	@Transactional(readOnly = true)
	public UserClaimProfileDTO getServiceUserClaimsForUsername(User owner, String serviceId, String username) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return null;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		List<UserClaim> claims = userClaimRepository.findByUsernameAndService(username, serviceId);
		UserClaimProfileDTO user = new UserClaimProfileDTO();
		user.setUsername(username);
		user.setClaims(new HashMap<>());
		claims.forEach(uc -> user.getClaims().put(ServiceClaim.qualifiedName(service.getNamespace(), uc.getClaim().getClaim()), ServiceClaim.typedValue(uc.getClaim(), uc.getValue())));
		return user;
		
	}

	/**
	 * Update claims for the specified user
	 * @param owner
	 * @param serviceId
	 * @param userId
	 * @param dto
	 * @return
	 * @throws InvalidDefinitionException 
	 */
	public UserClaimProfileDTO saveServiceUserClaims(User owner, String serviceId, String userId, UserClaimProfileDTO dto) throws InvalidDefinitionException {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return null;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		List<UserClaim> claims = userClaimRepository.findByUserAndService(Long.parseLong(userId), serviceId);
		if (!claims.isEmpty()) {
			userClaimRepository.deleteAll(claims);
		}
		Map<String, ServiceClaim> qClaims = serviceManager.getServiceClaimsDB(serviceId).stream().collect(Collectors.toMap(c -> ServiceClaim.qualifiedName(service.getNamespace(), c.getClaim()), c -> c));
		User dbUser = userManager.findOne(Long.parseLong(userId)); 

		for (Entry<String, Object> entry : dto.getClaims().entrySet()) {
			if (!qClaims.containsKey(entry.getKey())) {
				throw new SecurityException("Unauthorized claim modification: " + serviceId+", "+ entry.getKey());
			}
			ServiceClaim serviceClaim = qClaims.get(entry.getKey());
			UserClaim uc = new UserClaim();
			uc.setUser(dbUser);
			if (dbUser.getUsername() != null) uc.setUsername(dbUser.getUsername().toLowerCase());
			uc.setValue(retrieveClaimValue(entry, serviceClaim));
			uc.setClaim(serviceClaim);
			userClaimRepository.save(uc);
		}
		
		return getServiceUserClaims(owner, serviceId, userId);
	}

	public UserClaimProfileDTO saveServiceUserClaimsByUsername(User owner, String serviceId, String username, UserClaimProfileDTO dto) throws InvalidDefinitionException {
		User user = userManager.getUserByUsername(username);
		if (user != null) {
			return saveServiceUserClaims(owner, serviceId, user.getId().toString(), dto);
		}
		
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return null;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		List<UserClaim> claims = userClaimRepository.findByUsernameAndService(username, serviceId);
		if (!claims.isEmpty()) {
			userClaimRepository.deleteAll(claims);
		}
		Map<String, ServiceClaim> qClaims = serviceManager.getServiceClaimsDB(serviceId).stream().collect(Collectors.toMap(c -> ServiceClaim.qualifiedName(service.getNamespace(), c.getClaim()), c -> c));

		Map<String, Object> resClaims = new HashMap<>();
		for (Entry<String, Object> entry : dto.getClaims().entrySet()) {
			if (!qClaims.containsKey(entry.getKey())) {
				throw new SecurityException("Unauthorized claim modification: " + serviceId+", "+ entry.getKey());
			}
			ServiceClaim serviceClaim = qClaims.get(entry.getKey());
			UserClaim uc = new UserClaim();
			uc.setUser(null);
			uc.setUsername(username.toLowerCase());
			uc.setValue(retrieveClaimValue(entry, serviceClaim));
			uc.setClaim(serviceClaim);
			userClaimRepository.save(uc);
			resClaims.put(entry.getKey(), entry.getValue());
		}

		UserClaimProfileDTO res = new UserClaimProfileDTO();
		res.setUsername(username.toLowerCase());
		res.setClaims(resClaims);
		return res;

	}

	protected String retrieveClaimValue(Entry<String, Object> entry, ServiceClaim serviceClaim)
			throws InvalidDefinitionException {
		if (!ServiceClaim.ofType(entry.getValue(), serviceClaim.isMultiple(), serviceClaim.getType())) {
			throw new InvalidDefinitionException("Invalid claim value. Claim "+ entry.getKey() +", " +entry.getValue());
		}
		try {
			return mapper.writeValueAsString(entry.getValue());
		} catch (JsonProcessingException e) {
			throw new InvalidDefinitionException("Invalid claim value. Claim "+ entry.getKey() +", " +entry.getValue());
		}
	}
	
	public void deleteServiceUserClaims(User owner, String serviceId, String userId) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		userClaimRepository.deleteAll(userClaimRepository.findByUserAndService(Long.parseLong(userId), serviceId));
	}
	public void deleteServiceUserClaimsByUsername(User owner, String serviceId, String username) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		userClaimRepository.deleteAll(userClaimRepository.findByUsernameAndService(username, serviceId));
	}

	
	private Map<String, Object> createUserClaims(String userId, Collection<? extends GrantedAuthority> authorities, String mapping, Set<String> scopes, Set<String> authorizedClaims, Set<String>  requestedClaims, Collection<String> uniqueSpaces, Collection<String> rolePrefixes, boolean suppressErrors, String excludedServiceId) throws InvalidDefinitionException {
		AccountProfile ui = profileManager.getAccountProfileById(userId);
		
		// get the base object
		Map<String, Object> obj = toBaseJson(ui);
		
		/*
		 * OpenID claims
		 * see https://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims
		 */
	      //TODO rework as per spec: profile etc should be filters
        //openid
        if(scopes.contains(Config.SCOPE_OPENID)) {
            obj.putAll(toOpenIdJson(ui));
        }		
		//profile
		if(scopes.contains(Config.SCOPE_PROFILE)) {
		    obj.putAll(toProfileJson(ui));
		}
		
        // email
        if (scopes.contains(Config.SCOPE_EMAIL)) {
            obj.putAll(toEmailJson(ui));
        }
        
        //address
        //TODO

        //phone
        //TODO
        
        /*
         * AAC claims
         * (we can override openId claims) 
         */
		//basic profile
        if (!Sets.intersection(scopes, profileScopes).isEmpty()) {
            obj.putAll(toProfileJson(ui));
        }
		
		//account
		if (!Sets.intersection(scopes, accountScopes).isEmpty()) {
			// account profiles
			AccountProfile accounts = profileManager.getAccountProfileById(userId);
			if (accounts != null && accounts.getAccounts() != null) {
				obj.put("accounts", accounts.getAccounts());			
			}
		}
		
		//roles
		if (!Sets.intersection(scopes, roleScopes).isEmpty()) {
			try {
				Collection<? extends GrantedAuthority> filtered = authorities;
				if (rolePrefixes != null) {
					filtered = filtered.stream().filter(a -> {
						return rolePrefixes.stream().anyMatch(rp -> rp != null && a.getAuthority().toLowerCase().startsWith(rp));
					}).collect(Collectors.toSet());
				}
				Set<Role> roles = filtered.stream().map(a -> Role.parse(a.getAuthority())).collect(Collectors.toSet());
				
				if(scopes.contains(Config.SCOPE_ROLE)) {
				    //append roles as authorities list
				    populateRoleClaims(roles, obj);
				}
				
                if(scopes.contains(Config.SCOPE_GROUP)) {
                    //append groups as authorities list
                    populateGroupClaims(roles, obj);
                }				
				
				//spaces
				if(uniqueSpaces != null && !uniqueSpaces.isEmpty()) {
				    //merge and output a fixed claim for spaces selections
				    Set<String> spaces = new HashSet<>();
				    for(Role r : roles) {
				        if(uniqueSpaces.contains(r.getContext())) {
				            spaces.add(r.getSpace());
				        }
				    }
				    
				    //output only if populated
				    if(spaces.size() == 1) {
				        obj.put("space", spaces.iterator().next());
				    } else if (spaces.size() > 1) {
				        obj.put("space", spaces.toArray(new String[0]));
				    }
				}
				
			} catch (Exception e) {
				logger.error("error fetching roles for user "+userId, e);
			}
		}
		
		Set<String> allowedByScope = getClaimsForScopeSet(scopes);
//		Set<String> authorizedByClaims = extractUserInfoClaimsIntoSet(authorizedClaims);
//		Set<String> requestedByClaims = extractUserInfoClaimsIntoSet(requestedClaims);
	    Set<String> authorizedByClaims = (authorizedClaims == null ? Collections.emptySet() : authorizedClaims);
        Set<String> requestedByClaims = (requestedClaims  == null ? Collections.emptySet() : requestedClaims);
		/*
		 * Filtering
		 */
		logger.trace("generated claims before filtering "+obj.toString());
		
		
		// Filter claims by performing a manual intersection of claims that are allowed by the given scope, requested, and authorized.
		// We cannot use Sets.intersection() or similar because Entry<> objects will evaluate to being unequal if their values are
		// different, whereas we are only interested in matching the Entry<>'s key values.
		Map<String, Object> result = new HashMap<String, Object>();
		for (Entry<String, Object> entry : obj.entrySet()) {
			
			if (claimAllowed(entry.getKey(), allowedByScope, authorizedByClaims, requestedByClaims)) {
					result.put(entry.getKey(), entry.getValue());
			} // otherwise there were specific claims requested and this wasn't one of them
		}
	    logger.trace("generated claims after filtering "+result.toString());

		// read store used claims and filter them for the scopes
		Map<String, Object> userClaims = getAllowedUserClaims(userId, allowedByScope, authorizedByClaims, requestedByClaims);
		result.putAll(userClaims);
		logger.trace("Claims after adding filtered user claims "+result.toString());

		// generate claims from service functions of the corresponding services. Add only new claims to the result
		for(String s: scopes) {
			//break for "default"
			if("default".equals(s)) {
				continue;
			}
			ServiceDTO service = serviceManager.getScopeService(s);
			if (service != null && service.getClaimMapping() != null && !service.getServiceId().equalsIgnoreCase(excludedServiceId)) {
				try {
					Map<String, Object> serviceMapping = customMapping(service.getClaimMapping(), new HashMap<>(result));
					serviceMapping = filterMappedClaims(serviceMapping, service);
					for (Entry<String, Object> entry : serviceMapping.entrySet()) {
						// skip generated claims or claims set explicitly
						if (!result.containsKey(entry.getKey()) && claimAllowed(entry.getKey(), allowedByScope, authorizedByClaims, requestedByClaims)) {
								result.put(entry.getKey(), entry.getValue());
						} 
					}
				} catch (InvalidDefinitionException e) {
					if (suppressErrors) {
					    logger.error("custom mapping failed: "+e.getMessage()+". Service id: " + service.getServiceId());
					} else {
						throw e;
					}

				}
			}
		}
		logger.trace("Claims after adding mapped service claims "+result.toString());

		// apply mapping to correct result
		Map<String, Object> copy = new HashMap<>(result);
		try {
			obj = customMapping(mapping, copy);
		} catch (InvalidDefinitionException e) {
			if (suppressErrors) {
				logger.error("error mapping claims for user "+userId, e);
			} else {
				throw e;
			}
		}
		copy.clear();
		// keep all the new claims apart the system ones
		for (Entry<String, Object> entry : obj.entrySet()) {
			// skip system claims
			if (systemScopes.contains(entry.getKey()))  continue;
			copy.put(entry.getKey(), entry.getValue());

		}
		// add the system claims back from the original version
		for (String key : systemScopes) {
			if (result.containsKey(key)) {
				copy.put(key, result.get(key));
			}
		}

		//strip all reserved since they will be handled by jwt service
        for (String key : reservedScopes) {
            if (copy.containsKey(key)) {
                copy.remove(key);
            }
        }
        
        logger.trace("claims after custom mapping "+copy.toString());

		return copy;
	}

	/**
	 * Filter claim by the specified Service
	 * @param serviceMapping
	 * @param service
	 */
	private Map<String, Object> filterMappedClaims(Map<String, Object> serviceMapping, ServiceDTO service) {
		// verify that the result contains only the claims of this service 
		Map<String, Object> res = new HashMap<>(serviceMapping);
		for (Entry<String, Object> entry : serviceMapping.entrySet()) {
			if (!service.claimMap().containsKey(entry.getKey())) {
				res.remove(entry.getKey());
			}
		}
		return res;
	}
	
	/**
	 * @param serviceMapping
	 * @param service
	 */
	private void validateMappedClaims(Map<String, Object> serviceMapping, ServiceDTO service) throws InvalidDefinitionException {
		// verify that the result contains only the claims of in correct format
		for (Entry<String, Object> entry : serviceMapping.entrySet()) {
			ServiceClaimDTO claim = service.claimMap().get(entry.getKey());
			if (claim == null) {
				throw new InvalidDefinitionException("Unknow claim: "+ entry.getKey());
			}
			Object v = entry.getValue();
			if (v != null) {
				if (!ServiceClaim.ofType(v, claim.isMultiple(), CLAIM_TYPE.get(claim.getType()))) {
					throw new InvalidDefinitionException("Invalid claim value. Claim " + claim.getClaim() +", value = " + v);
				}
			}
		}
	}

	protected Map<String, Object> getAllowedUserClaims(String userId, Set<String> allowedByScope, Set<String> authorizedByClaims, Set<String> requestedByClaims) {
		List<UserClaim> userClaims = userClaimRepository.findByUser(Long.parseLong(userId));
		Map<String, Object> claimMap = new HashMap<>();
		for (UserClaim uc : userClaims) {
			String qname = ServiceClaim.qualifiedName(uc.getClaim().getService().getNamespace(), uc.getClaim().getClaim());
			if (claimAllowed(qname, allowedByScope, authorizedByClaims, requestedByClaims)) {
				claimMap.put(qname, ServiceClaim.typedValue(uc.getClaim(), uc.getValue()));
			}
		}
		return  claimMap;
	}

	private boolean claimAllowed(String claim, Set<String> allowedByScope, Set<String> authorizedByClaims, Set<String> requestedByClaims) {
		return 
				// it's allowed either by scope or by the authorized claims (either way is fine with us)
				(allowedByScope.contains(claim) || authorizedByClaims.contains(claim)) && 
				// the requested claims are empty (so we allow all), or they're not empty and this claim was specifically asked for
    			(requestedByClaims.isEmpty() || requestedByClaims.contains(claim));
	}
	
	/**
	 * Custom mapping for the user claims if defined by client
	 * @param user
	 * @param client
	 * @param obj
	 * @return
	 */
	private Map<String, Object> customMapping(String mapping, Map<String, Object> obj) throws InvalidDefinitionException {
		if (StringUtils.isEmpty(mapping)) return new HashMap<>(obj);

		String func = mapping;
		try {
			return executeScript(obj, func);
		} catch (ScriptCPUAbuseException e) {
			throw new InvalidDefinitionException("Script resource abuse: " + e.getMessage());
		} catch (Exception e) {
			throw new InvalidDefinitionException("Execution error: " + e.getMessage());
		}
	}

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeScript(Map<String, Object> obj, String func)
            throws ScriptException, IOException {
        // use scriptmanager, java11+
        // TODO handle resources
        // TODO use a threadPool/cache to avoid the expensive sandbox creation at each call
        // TODO handle cache for mapping
        // TODO move to dedicated service for JS execution
//	    NashornSandbox sandbox = createSandbox();		
        GraalSandbox sandbox = createSandbox();
		try {
			ObjectMapper mapper = new ObjectMapper();
			StringWriter writer = new StringWriter();
			writer.append("claims = ");
			mapper.writeValue(writer, obj);
			writer.append(";");
			writer.append(func);
			writer.append("; result = JSON.stringify(claimMapping(claims))");
			sandbox.eval(writer.toString());
			String o = (String)sandbox.get("result");
			return mapper.readValue(o, HashMap.class);
		} finally {
			sandbox.getExecutor().shutdown();
		}
    }

	/**
	 * @param ui
	 * @return
	 */
    private Map<String, Object> toBaseJson(BasicProfile ui) {

        Map<String, Object> obj = new HashMap<>();
        // disable sub since it is reserved
//		obj.put("sub", ui.getUserId());
   
        return obj;
    }

    private Map<String, Object> toOpenIdJson(AccountProfile ui) {

        Map<String, Object> obj = new HashMap<>();
        obj.put("preferred_username", ui.getUsername());
        // also write username in alternate claim
        obj.put("username", ui.getUsername());
        // also write username in a spring-friendly form
        //https://github.com/spring-projects/spring-security-oauth/blob/master/spring-security-oauth2/src/main/java/org/springframework/security/oauth2/provider/token/UserAuthenticationConverter.java
        obj.put("user_name", ui.getUsername());

        return obj;
    }
    
    private Map<String, Object> toProfileJson(AccountProfile ui) {

        Map<String, Object> obj = new HashMap<>();
        obj.put("name", ui.getSurname() + " " + ui.getName());
        obj.put("given_name", ui.getName());
        obj.put("family_name", ui.getSurname());
        
        //write username claims here so we get them even without openid scope
        obj.put("preferred_username", ui.getUsername());
        // also write username in alternate claim
        obj.put("username", ui.getUsername());
        // also write username in a spring-friendly form
        //https://github.com/spring-projects/spring-security-oauth/blob/master/spring-security-oauth2/src/main/java/org/springframework/security/oauth2/provider/token/UserAuthenticationConverter.java
        obj.put("user_name", ui.getUsername());                

        return obj;
    }    
	
    private Map<String, Object> toEmailJson(AccountProfile ui) {

        Map<String, Object> obj = new HashMap<>();
        if (ui.getAccounts() != null) {
        	for (String account: ui.getAccounts().keySet()) {
        		Map<String, String> attrs = ui.getAccounts().get(account);
        		if (attrs.containsKey(Config.USER_ATTR_EMAIL)) {
        	        obj.put("email", ui.getUsername());
        	        obj.put("email_verified", true);
        	        return obj;
        		}
        	}
        }

        return obj;
    }        
    
	private Set<String> getClaimsForScopeSet(Set<String> scopes) {
		return serviceManager.getClaimsForScopes(scopes);
	}	
	
	/**
	 * Pull the claims that have been targeted into a set for processing.
	 * Returns an empty set if the input is null.
	 * @param claims the claims request to process
	 */
	//TODO move to openid managers / protocol spec handling
	public Set<String> extractUserInfoClaimsIntoSet(Map<String, Serializable> claims) {
		Set<String> target = new HashSet<>();
		if (claims != null) {
		    //TODO parse via objectmapper or disassemble in caller
		    //for now unsupported
//			JsonObject userinfoAuthorized = claims.getAsJsonObject("userinfo");
		    Serializable userinfoAuthorized = claims.get("userinfo");
			if (userinfoAuthorized != null) {
//				for (Entry<String, JsonElement> entry : userinfoAuthorized.entrySet()) {
//					target.add(entry.getKey());
//				}
			}
		}
		return target;
	}

    private void populateGroupClaims(Set<Role> roles, Map<String, Object> claims) {
        if (roles != null) {
            
            // build list of "groups" (as plain array)
            // define a group as context+space, ignoring role
            Set<String> groups = new HashSet<>();
            for (Role role : roles) {
//                if (!StringUtils.isEmpty(role.getContext()) && !StringUtils.isEmpty(role.getSpace())) {
//                    groups.add(role.getContext() + "/" + role.getSpace());
//                }
                if (!StringUtils.isEmpty(role.getSpace())) {
                    groups.add(role.asSlug());
                }
            }
            claims.put("groups", groups.toArray(new String[0]));                   
        }
    }	
	
	private void populateRoleClaims(Set<Role> roles, Map<String, Object> claims) {
		if (roles != null) {
            // build roles list as plain array
            Set<String> rolesList = new HashSet<>();
            for (Role role : roles) {
                rolesList.add(role.getAuthority());
            }
            claims.put("roles", rolesList.toArray(new String[0]));
//            if (!claims.containsKey("authorities")) {
//                claims.put("authorities", claims.get("roles"));
//            }
            // build also as realm/resource claims
            Set<String> realmRoles = new HashSet<>();
            for (Role role : roles) {
                if(StringUtils.isEmpty(role.getContext()) && StringUtils.isEmpty(role.getSpace())) {
                    realmRoles.add(role.getRole());
                }
            }
//            net.minidev.json.JSONObject realmRoleObj = new net.minidev.json.JSONObject();
//            realmRoleObj.put("roles", realmRoles.toArray(new String[0]));
//            claims.put("realm_access", realmRoleObj);
            
            Map<String,Set<String>> resourceRoles = new HashMap<>();
            for (Role role : roles) {
                // role is context/space:role 
                if(!StringUtils.isEmpty(role.getContext()) && !StringUtils.isEmpty(role.getSpace())) {
                    // put as context/space:role 
                    String key = role.getContext()+ "/"+role.getSpace();
                    if(!resourceRoles.containsKey(key)) {
                        resourceRoles.put(key, new HashSet<String>());
                    }                               
                    resourceRoles.get(key).add(role.getRole());                                
                } else if(!StringUtils.isEmpty(role.getContext()) && StringUtils.isEmpty(role.getSpace())) { 
                    //put as context:role
                    if(!resourceRoles.containsKey(role.getContext())) {
                        resourceRoles.put(role.getContext(), new HashSet<String>());
                    }                                
                    resourceRoles.get(role.getContext()).add(role.getRole());                                
                }                                                       
            }
            
//            net.minidev.json.JSONObject resourceRolesObj = new net.minidev.json.JSONObject();
//            for(String res : resourceRoles.keySet()) {
//                net.minidev.json.JSONObject resObj = new net.minidev.json.JSONObject();                                                      
//                resObj.put("roles", resourceRoles.get(res).toArray(new String[0]));
//                resourceRolesObj.put(res, resObj);
//            }                        
//            claims.put("resource_access", resourceRolesObj);
            
            
            //MOVED to custom scope
//            // also build list of "groups" (as plain array)
//            // define a group as context+space, ignoring role
//            Set<String> groups = new HashSet<>();
//            for (Role role : roles) {
////                if (!StringUtils.isEmpty(role.getContext()) && !StringUtils.isEmpty(role.getSpace())) {
////                    groups.add(role.getContext() + "/" + role.getSpace());
////                }
//            	if (!StringUtils.isEmpty(role.getSpace())) {
//            		groups.add(role.asSlug());
//            	}
//            }
//            claims.put("groups", groups.toArray(new String[0]));                   
        }
	}
	
//	private NashornSandbox createSandbox() {
//		NashornSandbox sandbox;
//		sandbox = NashornSandboxes.create();
//		sandbox.setMaxCPUTime(100);
//		sandbox.setMaxMemory(10*1024*1024);
//		sandbox.setMaxPreparedStatements(30); // because preparing scripts for execution is expensive
//		sandbox.setExecutor(Executors.newSingleThreadExecutor());
//		return sandbox;
//	}
	
	   private GraalSandbox createSandbox() {
	       GraalSandbox sandbox;
	        sandbox = GraalSandboxes.create();
	        sandbox.setMaxCPUTime(100);
	        sandbox.setMaxMemory(10*1024*1024);
	        sandbox.setMaxPreparedStatements(30); // because preparing scripts for execution is expensive
	        sandbox.setExecutor(Executors.newSingleThreadExecutor());
	        return sandbox;
	    } 
	
//	
//	public static void main(String[] args) throws Exception {
//		Map<String, Object> res = new ClaimManager().executeScript(
//				Collections.singletonMap("key", "value"),
//				"function(claims) {claims['a'] = 'b'; return claims;}"
//		);
//		System.err.println(res);
//	}

	/**
	 * @param owner
	 * @param serviceId
	 * @param name
	 * @param page
	 * @return
	 */
	@Transactional(readOnly = true)
	public Page<ClientClaimProfileDTO> getServiceClientClaims(User owner, String serviceId, Pageable page) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return new PageImpl<>(Collections.emptyList());
		
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		Page<String[]> clientData = clientClaimRepository.findClientDataByService(serviceId.toLowerCase(), page);
		return clientData.map(arr -> new ClientClaimProfileDTO(arr[0], arr[1]));
	}

	/**
	 * @param owner
	 * @param serviceId
	 * @param clientId
	 * @return
	 */
	@Transactional(readOnly = true)
	public ClientClaimProfileDTO getServiceClientClaims(User owner, String serviceId, String clientId) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return null;
		
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		List<ClientClaim> claims = clientClaimRepository.findByClientAndService(clientId, serviceId);
		ClientClaimProfileDTO clientClaims = new ClientClaimProfileDTO();
		ClientDetailsEntity client = null;

		if (claims.isEmpty()) {
			client = clientDetailsRepository.findByClientId(clientId); 
		} else {
			client = claims.get(0).getClient();
		}
		clientClaims.setClientId(clientId);
		clientClaims.setName(client.getName());
		clientClaims.setClaims(new HashMap<>());
		claims.forEach(cc -> clientClaims.getClaims().put(ServiceClaim.qualifiedName(service.getNamespace(), cc.getClaim().getClaim()), ServiceClaim.typedValue(cc.getClaim(), cc.getValue())));
		return clientClaims;	
	}

	/**
	 * @param owner
	 * @param serviceId
	 * @param clientId
	 * @param dto
	 * @return
	 * @throws InvalidDefinitionException 
	 */
	public ClientClaimProfileDTO saveServiceClientClaims(User owner, String serviceId, String clientId, ClientClaimProfileDTO dto) throws InvalidDefinitionException {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return null;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		List<ClientClaim> claims = clientClaimRepository.findByClientAndService(clientId, serviceId);
		if (!claims.isEmpty()) {
			clientClaimRepository.deleteAll(claims);
		}
		Map<String, ServiceClaim> qClaims = serviceManager.getServiceClaimsDB(serviceId).stream().collect(Collectors.toMap(c -> ServiceClaim.qualifiedName(service.getNamespace(), c.getClaim()), c -> c));
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId); 

		for (Entry<String, Object> entry : dto.getClaims().entrySet()) {
			if (!qClaims.containsKey(entry.getKey())) {
				throw new SecurityException("Unauthorized claim modification: " + serviceId+", "+ entry.getKey());
			}
			ServiceClaim serviceClaim = qClaims.get(entry.getKey());
			ClientClaim cc = new ClientClaim();
			cc.setClient(client);
			cc.setValue(retrieveClaimValue(entry, serviceClaim));
			cc.setClaim(serviceClaim);
			clientClaimRepository.save(cc);
		}
		
		return getServiceClientClaims(owner, serviceId, clientId);	
	}

	/**
	 * @param owner
	 * @param serviceId
	 * @param clientId
	 */
	public void deleteServiceClientClaims(User owner, String serviceId, String clientId) {
		Set<String> userContexts = serviceManager.getUserContexts(owner);
		ServiceDTO service = serviceManager.getService(serviceId.toLowerCase());
		if (service == null) return;
		if (!userContexts.contains(service.getContext())) {
			throw new SecurityException("Not authorized to access service " + serviceId);
		}
		clientClaimRepository.deleteAll(clientClaimRepository.findByClientAndService(clientId, serviceId));
	}
}
