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
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;

import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.dto.AccountProfile;
import it.smartcommunitylab.aac.dto.BasicProfile;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;

/**
 * @author raman
 *
 */
@Component
public class ClaimManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private BasicProfileManager profileManager;

	private SetMultimap<String, String> scopesToClaims = HashMultimap.create();
	
	private Set<String> profileScopes = Sets.newHashSet(Config.BASIC_PROFILE_SCOPE);
	private Set<String> accountScopes = Sets.newHashSet(Config.ACCOUNT_PROFILE_SCOPE);
	private Set<String> roleScopes = Sets.newHashSet("user.roles.me");

	// claims that should not be overwritten
	private Set<String> reservedScopes = JWTClaimsSet.getRegisteredNames();
	private Set<String> systemScopes = Sets.newHashSet("scope", "token_type", "client_id", "active", "roles", "username", "user_name");

	// TODO
	// keep roles instead of authorities, change groups to become flat, remove authorities, realms, and role_access
	
	public ClaimManager() {
		super();
		// standard
		scopesToClaims.put("openid", "sub");
		scopesToClaims.put("openid", "username");
        scopesToClaims.put("openid", "user_name");		
		scopesToClaims.put("openid", "preferred_username");
		// standard
		scopesToClaims.put("profile", "name");
		scopesToClaims.put("profile", "preferred_username");
		scopesToClaims.put("profile", "given_name");
		scopesToClaims.put("profile", "family_name");
		scopesToClaims.put("profile", "middle_name");
		scopesToClaims.put("profile", "nickname");
		scopesToClaims.put("profile", "profile");
		scopesToClaims.put("profile", "picture");
		scopesToClaims.put("profile", "website");
		scopesToClaims.put("profile", "gender");
		scopesToClaims.put("profile", "zoneinfo");
		scopesToClaims.put("profile", "locale");
		scopesToClaims.put("profile", "updated_at");
		scopesToClaims.put("profile", "birthdate");
		// standard
		scopesToClaims.put("email", "email");
		scopesToClaims.put("email", "email_verified");
		// standard
		scopesToClaims.put("phone", "phone_number");
		scopesToClaims.put("phone", "phone_number_verified");
		// standard
		scopesToClaims.put("address", "address");
		// aac-specific
		profileScopes.forEach(s -> {
			scopesToClaims.put(s, "name");
			scopesToClaims.put(s, "preferred_username");
			scopesToClaims.put(s, "given_name");
			scopesToClaims.put(s, "family_name");
			scopesToClaims.put(s, "email");
			scopesToClaims.put(s, "username");
            scopesToClaims.put(s, "user_name");			
		});
		accountScopes.forEach(s -> {
			scopesToClaims.put(s, "accounts");
		});
		roleScopes.forEach(s -> {
			scopesToClaims.put(s, "authorities");
			scopesToClaims.put(s, "roles");
			scopesToClaims.put(s, "realm_access");
			scopesToClaims.put(s, "resource_access");
			scopesToClaims.put(s, "groups");
		});
	}

	/**
	 * Create user claims map considering the authorized scopes, authorized claims and request claims.
	 * THe claims are constructed based on the user roles, user info, and accounts info 
	 * @param user
	 * @param authorities
	 * @param client
	 * @param scopes
	 * @param authorizedClaims
	 * @param requestedClaims
	 * @return
	 */
	public Map<String, Object> createUserClaims(String userId, Collection<? extends GrantedAuthority> authorities, ClientDetailsEntity client, Set<String> scopes, JsonObject authorizedClaims, JsonObject requestedClaims) {
		ClientAppInfo appInfo = ClientAppInfo.convert(client.getAdditionalInformation());
		try {
			return createUserClaims(userId, authorities, appInfo.getClaimMapping(), scopes, authorizedClaims, requestedClaims, true);
		} catch (InvalidDefinitionException e) {
			// never arrives here
			return null;
		}
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
	public Map<String, Object> createUserClaims(User user, ClientAppInfo appInfo, Set<String> scopes) throws InvalidDefinitionException {
		return createUserClaims(user.getId().toString(), user.getRoles(), appInfo.getClaimMapping(), scopes, null, null, false);
	}
		
	
	public Map<String, Object> createUserClaims(User user, String mapping, Set<String> scopes) throws InvalidDefinitionException {
		return createUserClaims(user.getId().toString(), user.getRoles(), mapping, scopes, null, null, false);
	} 
	
	private Map<String, Object> createUserClaims(String userId, Collection<? extends GrantedAuthority> authorities, String mapping, Set<String> scopes, JsonObject authorizedClaims, JsonObject requestedClaims, boolean suppressErrors) throws InvalidDefinitionException {
		BasicProfile ui = profileManager.getBasicProfileById(userId);

		// get the base object
		Map<String, Object> obj = toJson(ui);

		if (!Sets.intersection(scopes, accountScopes).isEmpty()) {
			// account profiles
			AccountProfile accounts = profileManager.getAccountProfileById(userId);
			if (accounts != null && accounts.getAccounts() != null) {
				obj.put("accounts", accounts.getAccounts());			
			}
		}
		if (!Sets.intersection(scopes, roleScopes).isEmpty()) {
			try {
				Set<Role> roles = authorities.stream().map(a -> Role.parse(a.getAuthority())).collect(Collectors.toSet());
				//append roles as authorities list
				populateRoleClaims(roles, obj);
			} catch (Exception e) {
				logger.error("error fetching roles for user "+userId, e);
			}
		}
		
		
		Set<String> allowedByScope = getClaimsForScopeSet(scopes);
		Set<String> authorizedByClaims = extractUserInfoClaimsIntoSet(authorizedClaims);
		Set<String> requestedByClaims = extractUserInfoClaimsIntoSet(requestedClaims);

		// Filter claims by performing a manual intersection of claims that are allowed by the given scope, requested, and authorized.
		// We cannot use Sets.intersection() or similar because Entry<> objects will evaluate to being unequal if their values are
		// different, whereas we are only interested in matching the Entry<>'s key values.
		Map<String, Object> result = new HashMap<String, Object>();
		for (Entry<String, Object> entry : obj.entrySet()) {
			
			if (allowedByScope.contains(entry.getKey())
					|| authorizedByClaims.contains(entry.getKey())) {
				// it's allowed either by scope or by the authorized claims (either way is fine with us)

				if (requestedByClaims.isEmpty() || requestedByClaims.contains(entry.getKey())) {
					// the requested claims are empty (so we allow all), or they're not empty and this claim was specifically asked for
					result.put(entry.getKey(), entry.getValue());
				} // otherwise there were specific claims requested and this wasn't one of them
			}
		}

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
		return copy;
	}

	/**
	 * Custom mapping for the user claims if defined by client
	 * @param user
	 * @param client
	 * @param obj
	 * @return
	 */
	public Map<String, Object> customMapping(String mapping, Map<String, Object> obj) throws InvalidDefinitionException {
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
	protected Map<String, Object> executeScript(Map<String, Object> obj, String func) throws ScriptException, IOException {
		NashornSandbox sandbox = createSandbox();		
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
	private Map<String, Object> toJson(BasicProfile ui) {

		Map<String, Object> obj = new HashMap<>();
		//disable sub since it is reserved
//		obj.put("sub", ui.getUserId());

		obj.put("name", ui.getSurname() + " " + ui.getName());
		obj.put("preferred_username", ui.getUsername());
		obj.put("given_name", ui.getName());
		obj.put("family_name", ui.getSurname());

		obj.put("username", ui.getUsername());
		obj.put("email", ui.getUsername());

		//also write username in a spring-friendly form
        obj.put("user_name", ui.getUsername());

		return obj;
	}
	
	private Set<String> getClaimsForScope(String scope) {
		if (scopesToClaims.containsKey(scope)) {
			return scopesToClaims.get(scope);
		} else {
			return new HashSet<>();
		}
	}
	private Set<String> getClaimsForScopeSet(Set<String> scopes) {
		Set<String> result = new HashSet<>();
		for (String scope : scopes) {
			result.addAll(getClaimsForScope(scope));
		}
		return result;
	}	
	
	/**
	 * Pull the claims that have been targeted into a set for processing.
	 * Returns an empty set if the input is null.
	 * @param claims the claims request to process
	 */
	private Set<String> extractUserInfoClaimsIntoSet(JsonObject claims) {
		Set<String> target = new HashSet<>();
		if (claims != null) {
			JsonObject userinfoAuthorized = claims.getAsJsonObject("userinfo");
			if (userinfoAuthorized != null) {
				for (Entry<String, JsonElement> entry : userinfoAuthorized.entrySet()) {
					target.add(entry.getKey());
				}
			}
		}
		return target;
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
            
            
            // also build list of "groups" (as plain array)
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
	
	private NashornSandbox createSandbox() {
		NashornSandbox sandbox;
		sandbox = NashornSandboxes.create();
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
}
