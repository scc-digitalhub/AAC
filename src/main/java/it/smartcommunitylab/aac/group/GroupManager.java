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

package it.smartcommunitylab.aac.group;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.group.model.Group;
import it.smartcommunitylab.aac.group.model.NoSuchGroupException;
import it.smartcommunitylab.aac.group.persistence.GroupEntity;
import it.smartcommunitylab.aac.group.service.GroupService;
import it.smartcommunitylab.aac.model.Realm;

/**
 * @author raman
 *
 */
@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class GroupManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());
	// TODO
	@Autowired
	private GroupService groupService;
    @Autowired
    private RealmService realmService;
    @Autowired 
	private UserService userService;

    
	@Transactional(readOnly = true)
	public List<Group> getSubjectGroups(String subject, String realm) throws NoSuchRealmException {
        logger.debug("get group {} for realm {}", String.valueOf(subject), realm);
        realmService.getRealm(realm);
        return groupService.getSubjectGroups(subject);

	}
	
	@Transactional(readOnly = true)
	public List<Group> getRealmGroups(String realm) throws NoSuchRealmException {
        logger.debug("get groups for realm {}", realm);
        Realm r = realmService.getRealm(realm);
		return groupService.getRealmGroups(r.getSlug());
	}
	
	@Transactional(readOnly = true)
	public Page<Group> searchGroupsWithSpec(String realm, Specification<GroupEntity> spec, PageRequest pageRequest) throws NoSuchRealmException {
        logger.debug("search groups for realm {}", realm);
        Realm r = realmService.getRealm(realm);
		return groupService.getRealmGroups(r.getSlug(), spec, pageRequest);
	}

	@Transactional(readOnly = true)
	public Page<Group> getRealmGroups(String realm, Pageable pageRequest) throws NoSuchRealmException {
        logger.debug("get groups for realm {}", realm);
        Realm r = realmService.getRealm(realm);
		return groupService.getRealmGroups(r.getSlug(), pageRequest);
	}
	
	@Transactional(readOnly = true)
	public Group getGroup(String groupId, String realm) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("get group members for group {} realm {}", groupId, realm);
        Realm r = realmService.getRealm(realm);
        Group g = groupService.getGroup(groupId, true);
        if (!g.getRealm().equals(r.getSlug())) throw new NoSuchGroupException("Group not found in realm");
		return g;
	}
	
	public Group createGroup(String realm, String name, String externalId) throws NoSuchRealmException {
        logger.debug("Create group for realm {}", realm);
        Realm r = realmService.getRealm(realm);
        return groupService.createGroup(r.getSlug(), name, externalId);
	}
	public Group updateGroup(String id, String realm, String name, String externalId) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("Update group {id} for realm {}", realm);
        Realm r = realmService.getRealm(realm);
        getGroup(id, r.getSlug());
        return groupService.updateGroup(id, name, externalId);
	}

	public void setSubjectGroups(String subject, String realm, List<String> groups) throws NoSuchUserException, NoSuchRealmException, NoSuchGroupException {
        logger.debug("Set subject groups {} realm {}", subject, realm);
        Realm r = realmService.getRealm(realm);
        userService.getUser(subject, r.getSlug());
        // check groups of realm
        for (String g : groups) {
        	getGroup(g, realm);
        }
        groupService.setSubjectGroups(subject, groups);
	}
	public void setGroupMembers(String group, String realm, List<String> subjects) throws NoSuchRealmException, NoSuchGroupException, NoSuchUserException {
        logger.debug("Set group members {} realm {}", group, realm);
        Realm r = realmService.getRealm(realm);
    	getGroup(group, realm);
        // check subjects of realm
        for (String s : subjects) {
            userService.getUser(s, r.getSlug());
        }
        groupService.setGroupMembers(group, subjects);
	}
	
	public void deleteGroup(String group, String realm) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("delete group {} realm {}", group, realm);
        realmService.getRealm(realm);
    	getGroup(group, realm);
    	groupService.deleteGroup(group);
	}

	public void deleteUserFromGroups(String id, String realm) throws NoSuchRealmException, NoSuchUserException {
        logger.debug("delete subject from groups {} realm {}", id, realm);
        Realm r = realmService.getRealm(realm);
        userService.getUser(id, r.getSlug());
    	groupService.deleteUserFromGroups(id);
		
	}

}
