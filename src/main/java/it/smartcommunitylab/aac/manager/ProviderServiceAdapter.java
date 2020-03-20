/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */
package it.smartcommunitylab.aac.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.Authority;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.model.UserClaim;
import it.smartcommunitylab.aac.repository.AttributeRepository;
import it.smartcommunitylab.aac.repository.AuthorityRepository;
import it.smartcommunitylab.aac.repository.UserClaimRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * This class manages operations of the service
 * 
 */
//TODO merge with registrationManager
@Component
@Transactional
public class ProviderServiceAdapter {

	@Autowired
	private AttributesAdapter attrAdapter;
	@Autowired
	private AuthorityRepository authorityRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AttributeRepository attributeRepository;
	@Autowired
	private UserClaimRepository claimRepository;

	/**
	 * Updates of user attributes using the values obtained from http request
	 * 
	 * @param authorityUrl
	 *            the url of authority used from user to authenticate himself
	 * @param map 
	 * @param req
	 *            the http request
	 * @return the authentication token of the user (renew if it's expired)
	 * @throws AcServiceException
	 */
	public User updateUser(String authorityUrl, Map<String, String> map, HttpServletRequest req) {
		Authority auth = authorityRepository.findByRedirectUrl(authorityUrl);
		if (auth == null) {
			throw new IllegalArgumentException("Unknown authority URL: " + authorityUrl);
		}
		// read received attribute values
		Map<String, String> attributes = attrAdapter.getAttributes(auth.getName(), map, req);
		
		return findAndUpdate(auth, attributes);
	}

	public User updateNativeUser(String authority, String token, Map<String, String> params) {
		Authority auth = authorityRepository.findByRedirectUrl(authority);
		if (auth == null) {
			throw new IllegalArgumentException("Unknown authority URL: " + authority);
		}
		// read received attribute values
		Map<String, String> attributes = attrAdapter.getNativeAttributes(auth.getName(), token, params);
		
		return findAndUpdate(auth, attributes);
	}
	
    // TODO rewrite to *only* return User and let updateUser do the update
    private User findAndUpdate(Authority auth, Map<String, String> attributes) {

        User user = null;
        // extract all attributes via adapter
        List<Attribute> list = extractIdentityAttributes(auth, attributes, true);

        // username is unique, if provided we have an *exact* match
        if (attributes.get(Config.USER_ATTR_USERNAME) != null) {
            user = userRepository.findByUsername(attributes.get(Config.USER_ATTR_USERNAME));
        }

        //TODO rewrite, too convoluted
        if (user == null) {
            // find user by identity all attributes
            List<User> users = userRepository.getUsersByAttributes(list);
            if (users.size() == 1) {
                // one match for all attributes, fetch
                user = users.get(0);
            } else if (users.size() > 1) {
                // restrict search to base attributes only
                list = extractIdentityAttributes(auth, attributes, false);
                users = userRepository.getUsersByAttributes(list);
                if (users.size() == 1) {
                    // one match for all attributes, fetch
                    user = users.get(0);
                } else if (users.size() > 1) {
                    // still too many matches, give up
                    throw new IllegalArgumentException("The request attributes identify more than one user");
                }
            }
        }

        // clear attribute list
        list.clear();
        
        if(user == null) {
            //get only new attributes
            list = populateAttributes(auth, attributes,null);
            
            // new user registration
            user = new User(attributes.get(Config.USER_ATTR_NAME), attributes.get(Config.USER_ATTR_SURNAME),
                    new HashSet<Attribute>(list));
            
            user.getRoles().add(Role.systemUser());
            user.setUsername(attributes.get(Config.USER_ATTR_USERNAME));
            user = userRepository.saveAndFlush(user);
            
            //TODO remove, new users should not have pre-existing claims
            updateClaims(user);
            
            return user;
        } else {
            //merge new and old attributes
            list = populateAttributes(auth, attributes, user.getAttributeEntities());
            
            attributeRepository.deleteInBatch(user.getAttributeEntities());
            user.setAttributeEntities(new HashSet<Attribute>(list));
            user.updateNames(attributes.get(Config.USER_ATTR_NAME), attributes.get(Config.USER_ATTR_SURNAME));

            //TODO remove, a pre-existing user without username should not exist
            if (user.getUsername() == null) {
                user.setUsername(attributes.get(Config.USER_ATTR_USERNAME));
                //TODO remove, username can NOT be updated in theory
                updateClaims(user);
            }
            
            user = userRepository.saveAndFlush(user);
            
            return user;
        }

        //DEPRECATED 
//        if (users.isEmpty()) {
//            // new user registration
//            user = new User(attributes.get(Config.USER_ATTR_NAME), attributes.get(Config.USER_ATTR_SURNAME),
//                    new HashSet<Attribute>(list));
//            user.getRoles().add(Role.systemUser());
//            user.setUsername(attributes.get(Config.USER_ATTR_USERNAME));
//            user = userRepository.saveAndFlush(user);
//            
//            //TODO remove, new users should not have pre-existing claims
//            updateClaims(user);
//        } else {
//            user = users.get(0);
//            attributeRepository.deleteInBatch(user.getAttributeEntities());
//            user.setAttributeEntities(new HashSet<Attribute>(list));
//            user.updateNames(attributes.get(Config.USER_ATTR_NAME), attributes.get(Config.USER_ATTR_SURNAME));
//            if (user.getUsername() == null) {
//                user.setUsername(attributes.get(Config.USER_ATTR_USERNAME));
//                updateClaims(user);
//            }
//            userRepository.saveAndFlush(user);
//        }
//        return user;
    }

	/**
	 * @param user
	 */
	//TODO evaluate removal, why user_claim has a field "username" 
	private void updateClaims(User user) {
		if (user.getUsername() == null) return;
		
		List<UserClaim> claims = claimRepository.findByUsername(user.getUsername());
		claims.forEach(c -> c.setUser(user));
		claimRepository.save(claims);
	}

	private  List<Attribute> populateAttributes(Authority auth, Map<String, String> attributes,  Set<Attribute> old) {
	    List<Attribute> list = new ArrayList<>();

	    //build new from attributes map
		for (String key : attributes.keySet()) {
			String value = attributes.get(key);
			Attribute attr = new Attribute();
			attr.setAuthority(auth);
			attr.setKey(key);
			attr.setValue(value);
			list.add(attr);
		}
		
		//merge old
		if (old != null) {
			for (Attribute a : old) {
				if (!a.getAuthority().equals(auth)) {
					Attribute attr = new Attribute();
					attr.setAuthority(a.getAuthority());
					attr.setKey(a.getKey());
					attr.setValue(a.getValue());
					list.add(attr);
				}
			}
		}
		
		return list;
	}

	/**
	 * Extract identity attribute values from all the attributes received for the specified authority.
	 * @param auth
	 * @param attributes
	 * @param all search for all atrribute matches or only for own identity attributes
	 * @return
	 */
	private List<Attribute> extractIdentityAttributes(Authority auth, Map<String, String> attributes, boolean all) {
		return attrAdapter.findAllIdentityAttributes(auth, attributes, all);
	}

}
