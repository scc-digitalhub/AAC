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

package it.smartcommunitylab.aac.scim.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;
import org.wso2.charon3.core.attributes.Attribute;
import org.wso2.charon3.core.attributes.ComplexAttribute;
import org.wso2.charon3.core.attributes.MultiValuedAttribute;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.ConflictException;
import org.wso2.charon3.core.exceptions.NotFoundException;
import org.wso2.charon3.core.exceptions.NotImplementedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.objects.plainobjects.MultiValuedComplexType;
import org.wso2.charon3.core.objects.plainobjects.ScimName;
import org.wso2.charon3.core.protocol.ResponseCodeConstants;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.schema.SCIMDefinitions.DataType;
import org.wso2.charon3.core.utils.CopyUtil;
import org.wso2.charon3.core.utils.codeutils.Node;
import org.wso2.charon3.core.utils.codeutils.SearchRequest;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;

/**
 * WSO2 Charon extenstion for User management. 
 * TODO use standard AAC set of components: attribute set (realm-specific), mapper, extractor, provider (based on internal storage)
 * @author raman
 *
 */
public class SCIMUserManager implements UserManager {
	
	private String realm;
	private it.smartcommunitylab.aac.core.UserManager userManager;
	
	private static final Logger logger = LoggerFactory.getLogger(SCIMUserManager.class);
    //in memory user manager stores users
    ConcurrentHashMap<String, Group> inMemoryGroupList = new ConcurrentHashMap<String, Group>();

	public SCIMUserManager(String realm, it.smartcommunitylab.aac.core.UserManager userManager) {
		super();
		this.realm = realm;
		this.userManager = userManager;
	}

	@Override
	public User getUser(String id, Map<String, Boolean> requiredAttributes)
			throws CharonException, BadRequestException, NotFoundException {
		it.smartcommunitylab.aac.model.User user;
		try {
			user = userManager.getUser(realm, id);
		} catch (NoSuchUserException | NoSuchRealmException e) {
			 throw new NotFoundException("User not found");
		}
		if (!user.getRealm().equals(realm)) throw new NotFoundException("User not found");
		// TODO groups
		return convertUser(user, requiredAttributes);
	}

	/**
	 * @param user
	 * @param requiredAttributes 
	 * @return
	 * @throws BadRequestException 
	 * @throws CharonException 
	 */
	private User convertUser(it.smartcommunitylab.aac.model.User user, Map<String, Boolean> requiredAttributes) throws CharonException, BadRequestException {
		User scimUser = new User();
		scimUser.setSchemas();
		scimUser.setId(user.getSubjectId());
		scimUser.setCreatedInstant(user.getCreateDate().toInstant());
		scimUser.setLastModifiedInstant(user.getModifiedDate().toInstant());
		scimUser.setUserName(user.getUsername());
		scimUser.replaceActive(!user.isLocked());
		if (user.getEmail() != null) {
			scimUser.replaceEmails(Collections.singletonList(new MultiValuedComplexType(null, true, null, user.getEmail(), null)));
		}
		user.getAttributes().forEach(ua -> {
			if (OpenIdAttributesSet.IDENTIFIER.equals(ua.getIdentifier())) {
				ScimName name = new ScimName();
				ua.getAttributes().forEach(a -> {
					if (OpenIdAttributesSet.GIVEN_NAME.equals(a.getKey())) {
						name.setGivenName(a.getValue().toString());
					}
					if (OpenIdAttributesSet.FAMILY_NAME.equals(a.getKey())) {
						name.setFamilyName(a.getValue().toString());
					}
					if (OpenIdAttributesSet.MIDDLE_NAME.equals(a.getKey())) {
						name.setMiddleName(a.getValue().toString());
					}
					if (OpenIdAttributesSet.EMAIL.equals(a.getKey())) {
						scimUser.replaceEmails(Collections.singletonList(new MultiValuedComplexType(null, true, null, a.getValue().toString(), null)));
					}
				});
				scimUser.replaceName(name);
				
			}
		});
		List<MultiValuedComplexType> roles = user.getAuthorities().stream().filter(r -> r instanceof RealmGrantedAuthority && ((RealmGrantedAuthority)r).getRealm().equals(realm)).map(r -> new MultiValuedComplexType(null, true, null, ((RealmGrantedAuthority)r).getRole(), null)).collect(Collectors.toList());
		if (roles.size() > 0)  {
			scimUser.replaceRoles(roles);
		}
		
		adjustAttributes(scimUser);
		return scimUser;
	}

    /**
     * Hack to overcome the bug caused by validator - missing type in the attributes of complex types.
	 * @param scimUser
	 */
	private void adjustAttributes(User scimUser) {
        scimUser.getAttributeList().keySet().forEach(key -> {
        	Attribute a = scimUser.getAttribute(key);
        	if (a instanceof ComplexAttribute) {
        		((ComplexAttribute)a).setType(DataType.COMPLEX);
        		((ComplexAttribute) a).getSubAttributesList().values().forEach(sa -> {
                	if (sa instanceof ComplexAttribute) {
                		((ComplexAttribute)sa).setType(DataType.COMPLEX);
                	}
        		});
        	}
        	if (a instanceof MultiValuedAttribute) {
        		((MultiValuedAttribute) a).getAttributeValues().forEach(sa -> {
                	if (sa instanceof ComplexAttribute) {
                		((ComplexAttribute)sa).setType(DataType.COMPLEX);
                	}
        		});
        	}
        });
    }

	@Override
    public User createUser(User user, Map<String, Boolean> map)
            throws CharonException, ConflictException, BadRequestException {
		
		// ignore readonly ID attribute
		if (user.getId() != null) user.setId(null);
		// set username lowercase
		user.setUserName(user.getUsername().toLowerCase());
		
		try {
			List<it.smartcommunitylab.aac.model.User> list = userManager.findUsersByUsername(realm, user.getUsername().trim());
			if (list != null && !list.isEmpty()) {
				it.smartcommunitylab.aac.model.User newUser = list.get(0);
				user.setId(newUser.getSubjectId());
				return updateUser(user, map);
			}
		} catch (NoSuchRealmException e) {
			 throw new BadRequestException("Realm not found");
		} catch (NotFoundException e) {
			throw new CharonException(e.getMessage());
		} catch (NotImplementedException e) {
			throw new CharonException(e.getMessage());
		}
		
		try {
			String id = userManager.inviteUser(realm, user.getUserName(), null);
			user.setId(id);
			return updateUser(user, map);
		} catch (RegistrationException | NoSuchRealmException | NoSuchProviderException | NoSuchUserException | NotFoundException | NotImplementedException e) {
			throw new CharonException(e.getMessage());
		}
    }

    @Override
    public void deleteUser(String id)
            throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
    	try {
			userManager.removeUser(realm, id);
		} catch (NoSuchUserException | NoSuchRealmException e) {
            throw new NotFoundException("No user with the id : " + id);
		}
    }

    @Override
    public List<Object> listUsersWithGET(Node rootNode, int startIndex, int count, String sortBy,
                                         String sortOrder, String domainName, Map<String, Boolean> requiredAttributes)
            throws CharonException, NotImplementedException, BadRequestException {
    	
    	PageRequest pageRequest = PageRequest.of(startIndex / count, count * 2, SCIMConstants.OperationalConstants.ASCENDING.equals(sortOrder) ? Direction.ASC : Direction.DESC, sortBy);
    	Page<it.smartcommunitylab.aac.model.User> page;
		try {
			page = rootNode != null 
					? userManager.searchUsersWithSpec(realm, FilterManager.buildQuery(rootNode), pageRequest)
					: userManager.searchUsers(realm, null, pageRequest);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage(), ResponseCodeConstants.INVALID_FILTER);
		} catch (NoSuchRealmException e) {
			 throw new BadRequestException("Realm not found", ResponseCodeConstants.INVALID_REQUEST);
		}
    	if (!page.hasContent()) return Collections.emptyList();
    	int idx = startIndex - startIndex / count - 1;
    	List<it.smartcommunitylab.aac.model.User> list = page.getContent().subList(idx, Math.min(idx + count, page.getContent().size()));
    	List<Object> result = new LinkedList<>();
    	// TODO groups
    	for (it.smartcommunitylab.aac.model.User u : list) {
    		result.add(convertUser(u, requiredAttributes));
    	}
    	return result;
    	
    }


    @Override
    public List<Object> listUsersWithPost(SearchRequest searchRequest, Map<String, Boolean> requiredAttributes)
            throws CharonException, NotImplementedException, BadRequestException {

        return listUsersWithGET(searchRequest.getFilter(), searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getSortBy(), searchRequest.getSortOder(), searchRequest.getDomainName(),
                requiredAttributes);
    }

    @Override
    public User updateUser(User user, Map<String, Boolean> map)
            throws NotImplementedException, CharonException, BadRequestException, NotFoundException {
    	// TODO
       if (user.getId() != null) {
    	   try {
    		    it.smartcommunitylab.aac.model.User existing = userManager.getUser(realm, user.getId());
    		    if (!existing.getRealm().equals(realm)) throw new NotFoundException("User not found");
    			String name = user.getName() != null ? user.getName().getGivenName() : null;
    			String surname = user.getName() != null ? user.getName().getFamilyName() : null;
    		    userManager.updateUser(realm, user.getId(), name, surname, user.getPreferredLanguage());
    		    if (user.getActive()) userManager.unlockUser(realm, user.getId());
    		    else userManager.lockUser(realm, user.getId());
    		    // TODO update groups
    		    return getUser(user.getId(), map);
   			} catch (NoSuchUserException | NoSuchRealmException | NoSuchProviderException e) {
   				throw new NotFoundException("User not found");
			}
       } else {
           throw new NotFoundException("No user with the id : " + user.getId());
       }
    }

    @Override
    public User getMe(String s, Map<String, Boolean> map)
            throws CharonException, BadRequestException, NotFoundException {
        return null;
    }

    @Override
    public User createMe(User user, Map<String, Boolean> map)
            throws CharonException, ConflictException, BadRequestException {
        return null;
    }

    @Override
    public void deleteMe(String s)
            throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
    }

    @Override
    public User updateMe(User user, Map<String, Boolean> map)
            throws NotImplementedException, CharonException, BadRequestException, NotFoundException {
    	return null;
    }

    @Override
    public Group createGroup(Group group, Map<String, Boolean> map)
            throws CharonException, ConflictException, NotImplementedException, BadRequestException {
    	// TODO
        inMemoryGroupList.put(group.getId(), group);
        return (Group) CopyUtil.deepCopy(group);
    }

    @Override
    public Group getGroup(String id, Map<String, Boolean> map)
            throws NotImplementedException, BadRequestException, CharonException, NotFoundException {
    	// TODO
        if (inMemoryGroupList.get(id) != null) {
            return (Group) CopyUtil.deepCopy(inMemoryGroupList.get(id));
        } else {
            throw new NotFoundException("No user with the id : " + id);
        }
    }

    @Override
    public void deleteGroup(String id)
            throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
    	// TODO
        if (inMemoryGroupList.get(id) == null) {
            throw new NotFoundException("No user with the id : " + id);
        } else {
            inMemoryGroupList.remove(id);
        }
    }

    @Override
    public List<Object> listGroupsWithGET(Node rootNode, int startIndex, int count, String sortBy, String sortOrder,
                                          String domainName, Map<String, Boolean> requiredAttributes)
            throws CharonException, NotImplementedException, BadRequestException {
        if (sortBy != null || sortOrder != null) {
            throw new NotImplementedException("Sorting is not supported");
        }  else if (startIndex != 1) {
            throw new NotImplementedException("Pagination is not supported");
        } else if (rootNode != null) {
            throw new NotImplementedException("Filtering is not supported");
        } else {
            return listGroups(requiredAttributes);
        }
    }

    private List<Object> listGroups(Map<String, Boolean> requiredAttributes) {
    	// TODO
        List<Object> groupList = new ArrayList<>();
        groupList.add(0, 0);
        for (Group group : inMemoryGroupList.values()) {
            groupList.add(group);
        }
        groupList.set(0, groupList.size() - 1);
        try {
            return (List<Object>) CopyUtil.deepCopy(groupList);
        } catch (CharonException e) {
            logger.error("Error in listing groups");
            return  null;
        }

    }

    @Override
    public Group updateGroup(Group group, Group group1, Map<String, Boolean> map)
            throws NotImplementedException, BadRequestException, CharonException, NotFoundException {
    	// TODO
        if (group.getId() != null) {
            inMemoryGroupList.replace(group.getId(), group);
            return (Group) CopyUtil.deepCopy(group);
        } else {
            throw new NotFoundException("No user with the id : " + group.getId());
        }
    }

    @Override
    public List<Object> listGroupsWithPost(SearchRequest searchRequest, Map<String, Boolean> requiredAttributes)
            throws NotImplementedException, BadRequestException, CharonException {

        return listGroupsWithGET(searchRequest.getFilter(), searchRequest.getStartIndex(),
                searchRequest.getCount(), searchRequest.getSortBy(), searchRequest.getSortOder(),
                searchRequest.getDomainName(), requiredAttributes);
    }
}
