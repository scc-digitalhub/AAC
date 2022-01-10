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

package it.smartcommunitylab.aac.group.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.group.model.Group;
import it.smartcommunitylab.aac.group.persistence.GroupEntity;
import it.smartcommunitylab.aac.group.persistence.GroupEntityRepository;
import it.smartcommunitylab.aac.group.persistence.GroupMemberEntity;
import it.smartcommunitylab.aac.group.persistence.GroupMemberEntityRepository;

/**
 * @author raman
 *
 */
@Service
@Transactional
public class GroupService {

	private final GroupEntityRepository groupRepository;
	private final GroupMemberEntityRepository groupMemberRepository;
    private final SubjectService subjectService;
	
	public GroupService(
			GroupEntityRepository groupRepository,
			GroupMemberEntityRepository groupMemberRepository,
            SubjectService subjectService) {
        Assert.notNull(groupRepository, "groupRepository is mandatory");
        Assert.notNull(groupMemberRepository, "groupMemberRepository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.subjectService = subjectService;
    }
	
	@Transactional(readOnly = true)
	public List<Group> getSubjectGroups(String subject) {
		List<Group> groups = groupMemberRepository.findGroupsBySubject(subject).stream().map(this::convert).collect(Collectors.toList());
		return groups;
	}
	
	@Transactional(readOnly = true)
	public List<Group> getRealmGroups(String realm) {
		List<Group> groups = groupRepository.findByRealm(realm).stream().map(this::convert).collect(Collectors.toList());
		Map<String, List<GroupMemberEntity>> map = groupMemberRepository.findByGroupIn(groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet())).stream().collect(Collectors.groupingBy(GroupMemberEntity::getGroup));
		groups.forEach(g -> g.setMembers(map.getOrDefault(g.getGroupId(), Collections.emptyList()).stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList())));
		return groups;
	}
	
	@Transactional(readOnly = true)
	public Page<Group> getRealmGroups(String realm, Pageable pageRequest) {
		Page<Group> groups = groupRepository.findByRealm(realm, pageRequest).map(this::convert);
		Map<String, List<GroupMemberEntity>> map = groupMemberRepository.findByGroupIn(groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet())).stream().collect(Collectors.groupingBy(GroupMemberEntity::getGroup));
		groups.forEach(g -> g.setMembers(map.getOrDefault(g.getGroupId(), Collections.emptyList()).stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList())));
		return groups;
	}
	
	@Transactional(readOnly = true)
	public Page<Group> getRealmGroups(String slug, Specification<GroupEntity> spec, PageRequest pageRequest) {
		Page<Group> groups = groupRepository.findAll(spec, pageRequest).map(this::convert);
		Map<String, List<GroupMemberEntity>> map = groupMemberRepository.findByGroupIn(groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet())).stream().collect(Collectors.groupingBy(GroupMemberEntity::getGroup));
		groups.forEach(g -> g.setMembers(map.getOrDefault(g.getGroupId(), Collections.emptyList()).stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList())));
		return groups;
	}

	@Transactional(readOnly = true)
	public Group getGroup(String groupId, boolean withMembers) {
		Group g = convert(groupRepository.findOne(groupId));
		if (withMembers) g.setMembers(getGroupMembers(groupId));
		return g;
	}

	@Transactional(readOnly = true)
	public List<String> getGroupMembers(String groupId) {
		return groupMemberRepository.findByGroup(groupId).stream().map(m -> m.getSubject()).collect(Collectors.toList());
	}
	
	public Group createGroup(String realm, String name, String externalId) {
		GroupEntity res = new GroupEntity();
		res.setExternalId(externalId);
		res.setDisplayName(name);
		res.setRealm(realm);
		res.setUuid(subjectService.generateUuid(SystemKeys.RESOURCE_GROUP));
		res.setCreateDate(new Date());
		res.setModifiedDate(res.getCreateDate());
		res = groupRepository.save(res);
		return convert(res);
	}
	
	public Group updateGroup(String id, String name, String externalId) {
		GroupEntity res = groupRepository.findOne(id);
		res.setExternalId(externalId);
		res.setDisplayName(name);
		res.setModifiedDate(new Date());
		res = groupRepository.save(res);
		return convert(res);
	}

	public void setSubjectGroups(String subject, List<String> groups) {
		List<GroupMemberEntity> members = groupMemberRepository.findBySubject(subject);
		if (members.size() > 0) {
			groupMemberRepository.deleteInBatch(members);
		}
		groups.forEach(g -> {
			GroupMemberEntity gm = new GroupMemberEntity();
			gm.setGroup(g);
			gm.setSubject(subject);
			groupMemberRepository.save(gm);	
		});

	}
	public void setGroupMembers(String group, List<String> subjects) {
		List<GroupMemberEntity> members = groupMemberRepository.findByGroup(group);
		if (members.size() > 0) {
			groupMemberRepository.deleteInBatch(members);
		}
		subjects.forEach(s -> {
			GroupMemberEntity gm = new GroupMemberEntity();
			gm.setGroup(group);
			gm.setSubject(s);
			groupMemberRepository.save(gm);	
		});
		GroupEntity res = groupRepository.findOne(group);
		res.setModifiedDate(new Date());
		groupRepository.save(res);
	}
	

	/**
	 * @param id
	 */
	public void deleteUserFromGroups(String id) {
		List<GroupMemberEntity> memberships = groupMemberRepository.findBySubject(id);
		groupMemberRepository.deleteAll(memberships);
	}

	public void deleteGroup(String group) {
		List<GroupMemberEntity> members = groupMemberRepository.findByGroup(group);
		if (members.size() > 0) {
			groupMemberRepository.deleteInBatch(members);
		}
		groupRepository.deleteById(group);
		subjectService.deleteSubject(group);
	}

	private Group convert(GroupEntity ge) {
		Group g = new Group();
		g.setDisplayName(ge.getDisplayName());
		g.setExternalId(ge.getExternalId());
		g.setGroupId(ge.getUuid());
		g.setRealm(ge.getRealm());
		g.setCreateDate(ge.getCreateDate());
		g.setModifiedDate(ge.getModifiedDate());
		return g;
	}

	
}
