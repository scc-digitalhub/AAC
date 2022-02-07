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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.wso2.charon3.core.attributes.Attribute;
import org.wso2.charon3.core.attributes.ComplexAttribute;
import org.wso2.charon3.core.attributes.MultiValuedAttribute;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.ConflictException;
import org.wso2.charon3.core.exceptions.NotFoundException;
import org.wso2.charon3.core.exceptions.NotImplementedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.objects.AbstractSCIMObject;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.objects.plainobjects.MultiValuedComplexType;
import org.wso2.charon3.core.objects.plainobjects.ScimName;
import org.wso2.charon3.core.protocol.ResponseCodeConstants;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.schema.SCIMDefinitions.DataType;
import org.wso2.charon3.core.utils.codeutils.Node;
import org.wso2.charon3.core.utils.codeutils.SearchRequest;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.group.GroupManager;

/**
 * WSO2 Charon extenstion for User management. 
 * TODO 
 * - use standard AAC set of components: attribute set (realm-specific), mapper, extractor, provider (based on internal storage)
 * - improve / extend conversion of attributes
 * @author raman
 *
 */
public class SCIMUserManager implements UserManager {
	
	private String realm;
	private it.smartcommunitylab.aac.core.UserManager userManager;
	private GroupManager groupManager;
	private String applicationUrl;
	
	private static final Logger logger = LoggerFactory.getLogger(SCIMUserManager.class);

	public SCIMUserManager(String realm, it.smartcommunitylab.aac.core.UserManager userManager, GroupManager groupManager, String applicationUrl) {
		super();
		this.realm = realm;
		this.userManager = userManager;
		this.groupManager = groupManager;
		this.applicationUrl = applicationUrl;
	}

	@Override
	public User getUser(String id, Map<String, Boolean> requiredAttributes)
			throws CharonException, BadRequestException, NotFoundException {
		it.smartcommunitylab.aac.model.User user;
		try {
			user = userManager.getUser(realm, id);
			Collection<it.smartcommunitylab.aac.model.Group> subjectGroups = groupManager.getSubjectGroups(id, realm);
			if (!user.getRealm().equals(realm)) throw new NotFoundException("User not found");
			return convertUser(user, subjectGroups, requiredAttributes);
		} catch (NoSuchUserException | NoSuchRealmException | NoSuchSubjectException e) {
			 throw new NotFoundException("User not found");
		}
	}

	/**
	 * @param user
	 * @param subjectGroups 
	 * @param requiredAttributes 
	 * @return
	 * @throws BadRequestException 
	 * @throws CharonException 
	 */
	private User convertUser(it.smartcommunitylab.aac.model.User user, Collection<it.smartcommunitylab.aac.model.Group> subjectGroups, Map<String, Boolean> requiredAttributes) throws CharonException, BadRequestException {
		User scimUser = new User();
		scimUser.setSchemas();
		scimUser.setId(user.getSubjectId());
		scimUser.setCreatedInstant(user.getCreateDate().toInstant());
		scimUser.setLastModifiedInstant(user.getModifiedDate().toInstant());
		scimUser.setLocation(createRef(user.getSubjectId(), "Users"));
		scimUser.setResourceType(SCIMConstants.USER);
		scimUser.setUserName(user.getUsername());
		scimUser.replaceActive(!user.isLocked());
		if (user.getEmail() != null) {
			scimUser.replaceEmails(Collections.singletonList(new MultiValuedComplexType(null, true, null, user.getEmail(), null)));
		}
		user.getIdentities().forEach(id -> id.getAttributes().forEach(ua -> {
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
		}));
		List<MultiValuedComplexType> roles = user.getAuthorities().stream().filter(r -> r instanceof RealmGrantedAuthority && ((RealmGrantedAuthority)r).getRealm().equals(realm)).map(r -> new MultiValuedComplexType(null, true, null, ((RealmGrantedAuthority)r).getRole(), null)).collect(Collectors.toList());
		if (roles.size() > 0)  {
			scimUser.replaceRoles(roles);
		}
		List<MultiValuedComplexType> groups = subjectGroups.stream().map(g -> new MultiValuedComplexType(null, false, g.getName(), g.getGroupId(), createRef(g.getGroupId(), "Groups"))).collect(Collectors.toList());
		if (groups.size() > 0) {
			scimUser.replaceGroups(groups);
		}
		
		adjustAttributes(scimUser);
		return scimUser;
	}

    /**
     * Hack to overcome the bug caused by validator - missing type in the attributes of complex types.
	 * @param obj
	 */
	private void adjustAttributes(AbstractSCIMObject obj) {
        obj.getAttributeList().keySet().forEach(key -> {
        	Attribute a = obj.getAttribute(key);
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
		if (user.getId() != null) user.deleteAttribute("id");
		// set username lowercase
		user.setUserName(user.getUsername().toLowerCase());
		
		try {
			List<it.smartcommunitylab.aac.model.User> list = userManager.findUsersByUsername(realm, user.getUsername().trim());
			if (list != null && !list.isEmpty()) {
				throw new ConflictException("The user with the specified userName already exists");
			}
		} catch (NoSuchRealmException e) {
			 throw new BadRequestException("Realm not found");
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
			groupManager.deleteSubjectFromGroups(id, realm);
		} catch (NoSuchUserException | NoSuchRealmException | NoSuchSubjectException e) {
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
					? userManager.searchUsersWithSpec(realm, FilterManager.buildQuery(rootNode, realm), pageRequest)
					: userManager.searchUsers(realm, null, pageRequest);
			
	    	if (!page.hasContent()) return Collections.emptyList();
	    	int idx = startIndex - startIndex / count - 1;
	    	List<it.smartcommunitylab.aac.model.User> list = page.getContent().subList(idx, Math.min(idx + count, page.getContent().size()));
	    	List<Object> result = new LinkedList<>();
	    	for (it.smartcommunitylab.aac.model.User u : list) {
	    	    try {
				Collection<it.smartcommunitylab.aac.model.Group> subjectGroups = groupManager.getSubjectGroups(u.getSubjectId(), realm);
	    		result.add(convertUser(u, subjectGroups, requiredAttributes));
	    	    } catch (NoSuchSubjectException se) {
	    	        //skip user
	    	    }
	    	}
	    	return result;
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage(), ResponseCodeConstants.INVALID_FILTER);
		} catch (NoSuchRealmException e) {
			 throw new BadRequestException("Realm not found", ResponseCodeConstants.INVALID_REQUEST);
		}
    	
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
       if (user.getId() != null) {
    	   try {
    		    it.smartcommunitylab.aac.model.User existing = userManager.getUser(realm, user.getId());
    		    if (!existing.getRealm().equals(realm)) throw new NotFoundException("User not found");
    			String name = user.getName() != null ? user.getName().getGivenName() : null;
    			String surname = user.getName() != null ? user.getName().getFamilyName() : null;
    		    userManager.updateUser(realm, user.getId(), name, surname, user.getPreferredLanguage());
    		    if (user.isAttributeExist(SCIMConstants.UserSchemaConstants.ACTIVE)) {
        		    if (user.getActive()) userManager.unlockUser(realm, user.getId());
        		    else userManager.lockUser(realm, user.getId());
    		    }
    		    // Group membership is modifiable only through group resource
//    		    if (user.isAttributeExist(SCIMConstants.UserSchemaConstants.GROUP) && user.getGroups() != null) {
//    		    	List<String> groups = user.getGroups().stream().map(v -> v.getValue()).collect(Collectors.toList());
//        		    groupManager.setSubjectGroups(user.getId(), realm, groups);
//    		    }
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
    	try {
    	    it.smartcommunitylab.aac.model.Group g = new it.smartcommunitylab.aac.model.Group();
    	    g.setGroupId(group.getId());
    	    g.setGroup(group.getExternalId());
    	    g.setName(group.getDisplayName());
    	    
			it.smartcommunitylab.aac.model.Group created = groupManager.addRealmGroup(realm, g);
			if (group.isAttributeExist(SCIMConstants.GroupSchemaConstants.MEMBERS) && group.getMembers() != null) {
				List<String> members = group.getMembers().stream().map(m -> m.toString()).collect(Collectors.toList());
			    groupManager.setGroupMembers(created.getGroupId(), realm, members);
			}

			return getGroup(created.getGroupId(), map);
		} catch (NotFoundException | NoSuchRealmException | NoSuchGroupException e) {
			throw new CharonException(e.getMessage());
		}
    }

    @Override
    public Group getGroup(String id, Map<String, Boolean> map)
            throws NotImplementedException, BadRequestException, CharonException, NotFoundException {
    	
		try {
	    	it.smartcommunitylab.aac.model.Group group = groupManager.getRealmGroup(realm, id, true);
	    	return convertGroup(group, map);
		} catch (NoSuchRealmException | NoSuchGroupException e) {
			throw new NotFoundException("Group not found");
		}
    }

    /**
	 * @param group
	 * @param map
     * @throws BadRequestException 
     * @throws CharonException 
	 */
	private Group convertGroup(it.smartcommunitylab.aac.model.Group group, Map<String, Boolean> map) throws CharonException, BadRequestException {
		Group sciGroup = new Group();
		sciGroup.setSchemas();
		sciGroup.setId(group.getGroupId());
		sciGroup.setExternalId(group.getGroup());
		sciGroup.setDisplayName(group.getName());
		sciGroup.setCreatedInstant(group.getCreateDate().toInstant());
		sciGroup.setLastModifiedInstant(group.getModifiedDate().toInstant());
		sciGroup.setLocation(createRef(group.getGroupId(), "Groups"));
		sciGroup.setResourceType(SCIMConstants.GROUP);
		if (group.getMembers().size() > 0) {
			for (String m : group.getMembers()) {
				sciGroup.setMember(m, null, createRef(m, "Users"), null);
			}
		}
		adjustAttributes(sciGroup);
		return sciGroup;		
	}

	@Override
    public void deleteGroup(String id)
            throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
		try {
			groupManager.deleteGroup(id, realm);
		} catch (NoSuchRealmException | NoSuchGroupException e) {
			throw new NotFoundException("Group not found");
		}
    }

    @Override
    public List<Object> listGroupsWithGET(Node rootNode, int startIndex, int count, String sortBy, String sortOrder,
                                          String domainName, Map<String, Boolean> requiredAttributes)
            throws CharonException, NotImplementedException, BadRequestException {
    	PageRequest pageRequest = PageRequest.of(startIndex / count, count * 2, SCIMConstants.OperationalConstants.ASCENDING.equals(sortOrder) ? Direction.ASC : Direction.DESC, sortBy);
    	Page<it.smartcommunitylab.aac.model.Group> page;
		try {
			page = rootNode != null 
					? groupManager.searchGroupsWithSpec(realm, FilterManager.buildQuery(rootNode, realm), pageRequest)
					: groupManager.getRealmGroups(realm, pageRequest);
			
	    	if (!page.hasContent()) return Collections.emptyList();
	    	int idx = startIndex - startIndex / count - 1;
	    	List<it.smartcommunitylab.aac.model.Group> list = page.getContent().subList(idx, Math.min(idx + count, page.getContent().size()));
	    	List<Object> result = new LinkedList<>();
	    	for (it.smartcommunitylab.aac.model.Group g : list) {
	    		result.add(convertGroup(g, requiredAttributes));
	    	}
	    	return result;
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage(), ResponseCodeConstants.INVALID_FILTER);
		} catch (NoSuchRealmException e) {
			 throw new BadRequestException("Realm not found", ResponseCodeConstants.INVALID_REQUEST);
		}    
	}



    @Override
    public Group updateGroup(Group group, Group group1, Map<String, Boolean> map)
            throws NotImplementedException, BadRequestException, CharonException, NotFoundException {
    	try {
    	    it.smartcommunitylab.aac.model.Group g = new it.smartcommunitylab.aac.model.Group();
            g.setGroupId(group.getId());
            g.setGroup(group.getExternalId());
            g.setName(group.getDisplayName());
            
			groupManager.updateRealmGroup(realm, group.getId(), g);
			if (group1.isAttributeExist(SCIMConstants.GroupSchemaConstants.MEMBERS) && group1.getMembers() != null) {
				List<String> members = group1.getMembers().stream().map(m -> m.toString()).collect(Collectors.toList());
			    groupManager.setGroupMembers(group1.getId(), realm, members);
			}
		} catch (NoSuchRealmException | NoSuchGroupException e) {
			throw new NotFoundException("Group not found");
		}
	    return getGroup(group1.getId(), map);
    }

    @Override
    public List<Object> listGroupsWithPost(SearchRequest searchRequest, Map<String, Boolean> requiredAttributes)
            throws NotImplementedException, BadRequestException, CharonException {

        return listGroupsWithGET(searchRequest.getFilter(), searchRequest.getStartIndex(),
                searchRequest.getCount(), searchRequest.getSortBy(), searchRequest.getSortOder(),
                searchRequest.getDomainName(), requiredAttributes);
    }
    
    private String createRef(String id, String type) {
    	return (applicationUrl.endsWith("/") ? applicationUrl.substring(0, applicationUrl.length()-1) : applicationUrl) + "/scim/v2/" + realm + "/" + type + "/" + id;
    }
    
}
