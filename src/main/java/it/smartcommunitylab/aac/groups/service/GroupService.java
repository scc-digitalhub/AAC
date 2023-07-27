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

package it.smartcommunitylab.aac.groups.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.groups.persistence.GroupEntity;
import it.smartcommunitylab.aac.groups.persistence.GroupEntityRepository;
import it.smartcommunitylab.aac.groups.persistence.GroupMemberEntity;
import it.smartcommunitylab.aac.groups.persistence.GroupMemberEntityRepository;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.Subject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
        SubjectService subjectService
    ) {
        Assert.notNull(groupRepository, "group repository is mandatory");
        Assert.notNull(groupMemberRepository, "group members repository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.subjectService = subjectService;
    }

    /*
     * Realm Groups
     */

    public Group addGroup(
        String uuid,
        String realm,
        String group,
        String parentGroup,
        String name,
        String description
    ) {
        GroupEntity g = groupRepository.findByRealmAndGroup(realm, group);
        if (g != null) {
            throw new IllegalArgumentException("group already exists with the same key");
        }

        if (!StringUtils.hasText(group)) {
            throw new IllegalArgumentException("group key is null or empty");
        }

        if (StringUtils.hasText(parentGroup)) {
            // check parent is distinct
            if (group.equals(parentGroup)) {
                throw new IllegalArgumentException("parent group must be distinct from this group");
            }

            // check if parent exists in the same realm
            GroupEntity pg = groupRepository.findByRealmAndGroup(realm, parentGroup);
            if (pg == null) {
                throw new IllegalArgumentException("parent group does not exists");
            }
        }

        if (!StringUtils.hasText(uuid)) {
            uuid = subjectService.generateUuid(SystemKeys.RESOURCE_GROUP);
        }

        // create a subject, will throw error if exists
        Subject s = subjectService.addSubject(uuid, realm, SystemKeys.RESOURCE_GROUP, group);

        // create group
        g = new GroupEntity();
        g.setUuid(s.getSubjectId());
        g.setRealm(realm);
        g.setGroup(group);
        g.setParentGroup(parentGroup);

        g.setName(name);
        g.setDescription(description);

        g = groupRepository.save(g);

        return toGroup(g, 0L);
    }

    @Transactional(readOnly = true)
    public Group fetchGroup(String uuid) {
        GroupEntity g = groupRepository.findOne(uuid);
        if (g == null) {
            return null;
        }

        return toGroup(g, null);
    }

    @Transactional(readOnly = true)
    public Group getGroup(String uuid) throws NoSuchGroupException {
        return getGroup(uuid, false);
    }

    @Transactional(readOnly = true)
    public Group getGroup(String uuid, boolean withMembers) throws NoSuchGroupException {
        GroupEntity g = groupRepository.findOne(uuid);
        if (g == null) {
            throw new NoSuchGroupException();
        }

        long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());
        if (withMembers) {
            Collection<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(
                g.getRealm(),
                g.getGroup()
            );
            return toGroup(g, size, members);
        }
        return toGroup(g, size);
    }

    @Transactional(readOnly = true)
    public Group findGroup(String realm, String group) {
        GroupEntity g = groupRepository.findByRealmAndGroup(realm, group);
        if (g == null) {
            return null;
        }

        return toGroup(g, null);
    }

    @Transactional(readOnly = true)
    public Group getGroup(String realm, String group) throws NoSuchGroupException {
        return getGroup(realm, group, true);
    }

    @Transactional(readOnly = true)
    public Group getGroup(String realm, String group, boolean withMembers) throws NoSuchGroupException {
        GroupEntity g = groupRepository.findByRealmAndGroup(realm, group);
        if (g == null) {
            throw new NoSuchGroupException();
        }

        long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());
        if (withMembers) {
            Collection<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(realm, group);
            return toGroup(g, size, members);
        }

        return toGroup(g, size);
    }

    @Transactional(readOnly = true)
    public Collection<Group> listGroups(String realm) {
        return listGroups(realm, false);
    }

    @Transactional(readOnly = true)
    public Collection<Group> listGroups(String realm, boolean withMembers) {
        List<Group> groups = groupRepository
            .findByRealm(realm)
            .stream()
            .map(g -> {
                long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());
                return toGroup(g, size);
            })
            .collect(Collectors.toList());
        if (withMembers) {
            groups.forEach(g -> {
                Collection<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(realm, g.getGroup());
                List<String> mm = members.stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList());
                g.setMembers(mm);
            });
        }
        return groups;
    }

    @Transactional(readOnly = true)
    public Page<Group> listGroups(String realm, Pageable pageRequest) {
        return listGroups(realm, pageRequest, false);
    }

    @Transactional(readOnly = true)
    public Page<Group> listGroups(String realm, Pageable pageRequest, boolean withMembers) {
        Page<Group> groups = groupRepository
            .findByRealm(realm, pageRequest)
            .map(g -> {
                long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());
                return toGroup(g, size);
            });
        if (withMembers) {
            groups.forEach(g -> {
                Collection<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(realm, g.getGroup());
                List<String> mm = members.stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList());
                g.setMembers(mm);
            });
        }

        return groups;
    }

    @Transactional(readOnly = true)
    public Collection<Group> listGroupsByParentGroup(String realm, String parentGroup) {
        return listGroupsByParentGroup(realm, parentGroup, false);
    }

    @Transactional(readOnly = true)
    public Collection<Group> listGroupsByParentGroup(String realm, String parentGroup, boolean withMembers) {
        List<Group> groups = groupRepository
            .findByRealmAndParentGroup(realm, parentGroup)
            .stream()
            .map(g -> {
                long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());
                return toGroup(g, size);
            })
            .collect(Collectors.toList());
        if (withMembers) {
            groups.forEach(g -> {
                Collection<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(realm, g.getGroup());
                List<String> mm = members.stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList());
                g.setMembers(mm);
            });
        }
        return groups;
    }

    @Transactional(readOnly = true)
    public Page<Group> searchGroupsWithSpec(
        String realm,
        Specification<GroupEntity> spec,
        PageRequest pageRequest,
        boolean withMembers
    ) {
        Page<Group> groups = groupRepository
            .findAll(spec, pageRequest)
            .map(g -> {
                long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());
                return toGroup(g, size);
            });
        if (withMembers) {
            groups.forEach(g -> {
                Collection<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(realm, g.getGroup());
                List<String> mm = members.stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList());
                g.setMembers(mm);
            });
        }

        return groups;
    }

    public Group updateGroup(
        String uuid,
        String realm,
        String group,
        String parentGroup,
        String name,
        String description
    ) throws NoSuchGroupException {
        GroupEntity g = groupRepository.findOne(uuid);
        if (g == null) {
            throw new NoSuchGroupException();
        }

        if (StringUtils.hasText(parentGroup)) {
            // check parent is distinct
            if (group.equals(parentGroup)) {
                throw new IllegalArgumentException("parent group must be distinct from this group");
            }

            // check if parent exists in the same realm
            GroupEntity pg = groupRepository.findByRealmAndGroup(realm, parentGroup);
            if (pg == null) {
                throw new IllegalArgumentException("parent group does not exists");
            }
        }

        // update
        g.setParentGroup(parentGroup);
        g.setName(name);
        g.setDescription(description);

        g = groupRepository.save(g);

        long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());

        return toGroup(g, size);
    }

    public Group renameGroup(String uuid, String realm, String group) throws NoSuchGroupException {
        GroupEntity g = groupRepository.findByRealmAndGroup(realm, group);
        if (g != null) {
            throw new IllegalArgumentException("group already exists with the same key");
        }

        g = groupRepository.findOne(uuid);
        if (g == null) {
            throw new NoSuchGroupException();
        }

        // update group requires update to member registrations
        // will break in memory status!
        // TODO handle side effect
        List<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(realm, g.getGroup());
        members.forEach(m -> {
            m.setGroup(group);
            m = groupMemberRepository.save(m);
        });

        // also need to update all children
        List<GroupEntity> subgroups = groupRepository.findByRealmAndParentGroup(realm, g.getGroup());
        subgroups.forEach(s -> {
            s.setParentGroup(group);
            s = groupRepository.save(s);
        });

        // update
        g.setGroup(group);

        g = groupRepository.save(g);

        long size = groupMemberRepository.countByRealmAndGroup(g.getRealm(), g.getGroup());

        return toGroup(g, size);
    }

    public void deleteGroup(String realm, String group) {
        GroupEntity g = groupRepository.findByRealmAndGroup(realm, group);
        if (g != null) {
            deleteGroup(g.getUuid());
        }
    }

    public void deleteGroup(String uuid) {
        GroupEntity g = groupRepository.findOne(uuid);
        if (g == null) {
            return;
        }

        // delete group requires delete to member registrations
        // will break in memory status!
        // TODO handle side effect

        List<GroupMemberEntity> members = groupMemberRepository.findByRealmAndGroup(g.getRealm(), g.getGroup());
        if (members.size() > 0) {
            groupMemberRepository.deleteAllInBatch(members);
        }

        // also need to unlink all children
        List<GroupEntity> subgroups = groupRepository.findByRealmAndParentGroup(g.getRealm(), g.getGroup());
        subgroups.forEach(s -> {
            s.setParentGroup(null);
            s = groupRepository.save(s);
        });

        // delete group
        groupRepository.delete(g);

        // remove subject if exists
        subjectService.deleteSubject(uuid);
    }

    //    @Transactional(readOnly = true)
    //    public List<Group> getRealmGroups(String realm) {
    //        List<Group> groups = groupRepository.findByRealm(realm).stream().map(this::toGroup)
    //                .collect(Collectors.toList());
    //        Map<String, List<GroupMemberEntity>> map = groupMemberRepository
    //                .findByGroupIn(groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet())).stream()
    //                .collect(Collectors.groupingBy(GroupMemberEntity::getGroup));
    //        groups.forEach(g -> g.setMembers(map.getOrDefault(g.getGroupId(), Collections.emptyList()).stream()
    //                .map(GroupMemberEntity::getSubject).collect(Collectors.toList())));
    //        return groups;
    //    }
    //    @Transactional(readOnly = true)
    //    public Page<Group> getRealmGroups(String realm, Pageable pageRequest) {
    //        Page<Group> groups = groupRepository.findByRealm(realm, pageRequest).map(this::toGroup);
    //        Map<String, List<GroupMemberEntity>> map = groupMemberRepository
    //                .findByGroupIn(groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet())).stream()
    //                .collect(Collectors.groupingBy(GroupMemberEntity::getGroup));
    //        groups.forEach(g -> g.setMembers(map.getOrDefault(g.getGroupId(), Collections.emptyList()).stream()
    //                .map(GroupMemberEntity::getSubject).collect(Collectors.toList())));
    //        return groups;
    //    }
    //
    //    @Transactional(readOnly = true)
    //    public Page<Group> getRealmGroups(String slug, Specification<GroupEntity> spec, PageRequest pageRequest) {
    //        Page<Group> groups = groupRepository.findAll(spec, pageRequest).map(this::toGroup);
    //        Map<String, List<GroupMemberEntity>> map = groupMemberRepository
    //                .findByGroupIn(groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet())).stream()
    //                .collect(Collectors.groupingBy(GroupMemberEntity::getGroup));
    //        groups.forEach(g -> g.setMembers(map.getOrDefault(g.getGroupId(), Collections.emptyList()).stream()
    //                .map(GroupMemberEntity::getSubject).collect(Collectors.toList())));
    //        return groups;
    //    }

    //    @Transactional(readOnly = true)
    //    public Group getGroup(String groupId, boolean withMembers) {
    //        Group g = toGroup(groupRepository.findOne(groupId));
    //        if (withMembers)
    //            g.setMembers(getGroupMembers(groupId));
    //        return g;
    //    }

    /*
     * Group members
     */

    public String addGroupMember(String realm, String group, String subjectId) {
        GroupMemberEntity gm = groupMemberRepository.findByRealmAndGroupAndSubject(realm, group, subjectId);
        if (gm == null) {
            gm = new GroupMemberEntity();
            gm.setRealm(realm);
            gm.setGroup(group);
            gm.setSubject(subjectId);
            gm = groupMemberRepository.save(gm);
        }

        return gm.getSubject();
    }

    @Transactional(readOnly = true)
    public long countGroupMembers(String realm, String group) {
        return groupMemberRepository.countByRealmAndGroup(realm, group);
    }

    @Transactional(readOnly = true)
    public Collection<String> getGroupMembers(String realm, String group) {
        return groupMemberRepository
            .findByRealmAndGroup(realm, group)
            .stream()
            .map(m -> m.getSubject())
            .collect(Collectors.toList());
    }

    public Collection<String> setGroupMembers(String realm, String group, Collection<String> subjects) {
        // fetch current
        List<GroupMemberEntity> oldMembers = groupMemberRepository.findByRealmAndGroup(realm, group);

        // unpack and merge
        Set<GroupMemberEntity> newMembers = subjects
            .stream()
            .map(s -> {
                GroupMemberEntity gm = new GroupMemberEntity();
                gm.setRealm(realm);
                gm.setGroup(group);
                gm.setSubject(s);
                return gm;
            })
            .collect(Collectors.toSet());

        Set<GroupMemberEntity> toDelete = oldMembers
            .stream()
            .filter(gm -> !newMembers.contains(gm))
            .collect(Collectors.toSet());
        Set<GroupMemberEntity> toAdd = newMembers
            .stream()
            .filter(gm -> !oldMembers.contains(gm))
            .collect(Collectors.toSet());

        // update
        groupMemberRepository.deleteAllInBatch(toDelete);
        groupMemberRepository.saveAll(toAdd);

        return getGroupMembers(realm, group);
    }

    public void removeGroupMember(String realm, String group, String subjectId) {
        GroupMemberEntity gm = groupMemberRepository.findByRealmAndGroupAndSubject(realm, group, subjectId);
        if (gm != null) {
            groupMemberRepository.delete(gm);
        }
    }

    /*
     * Subject groups
     */

    @Transactional(readOnly = true)
    public Collection<Group> getSubjectGroups(String subject) {
        Set<GroupEntity> groups = groupMemberRepository
            .findBySubject(subject)
            .stream()
            .map(gm -> groupRepository.findByRealmAndGroup(gm.getRealm(), gm.getGroup()))
            .filter(g -> g != null)
            .collect(Collectors.toSet());

        return groups.stream().map(g -> toGroup(g, null)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Collection<Group> getSubjectGroups(String subject, String realm) {
        Set<GroupEntity> groups = groupMemberRepository
            .findBySubjectAndRealm(subject, realm)
            .stream()
            .map(gm -> groupRepository.findByRealmAndGroup(gm.getRealm(), gm.getGroup()))
            .filter(g -> g != null)
            .collect(Collectors.toSet());

        return groups.stream().map(g -> toGroup(g, null)).collect(Collectors.toList());
    }

    public Collection<Group> setSubjectGroups(String subject, String realm, List<String> groups) {
        // fetch current
        List<GroupMemberEntity> oldMemberships = groupMemberRepository.findBySubjectAndRealm(subject, realm);

        // unpack and merge
        Set<GroupMemberEntity> newMemberships = groups
            .stream()
            .map(g -> {
                GroupMemberEntity gm = new GroupMemberEntity();
                gm.setRealm(realm);
                gm.setGroup(g);
                gm.setSubject(subject);
                return gm;
            })
            .collect(Collectors.toSet());

        Set<GroupMemberEntity> toDelete = oldMemberships
            .stream()
            .filter(gm -> !newMemberships.contains(gm))
            .collect(Collectors.toSet());
        Set<GroupMemberEntity> toAdd = newMemberships
            .stream()
            .filter(gm -> !oldMemberships.contains(gm))
            .collect(Collectors.toSet());

        // update
        groupMemberRepository.deleteAllInBatch(toDelete);
        groupMemberRepository.saveAll(toAdd);

        return getSubjectGroups(subject, realm);
    }

    /*
     * update subject memberships with a collection <realm, group>
     */
    public Collection<Group> setSubjectGroups(String subject, Collection<Map.Entry<String, String>> groups) {
        // fetch current
        List<GroupMemberEntity> oldMemberships = groupMemberRepository.findBySubject(subject);

        // unpack and merge
        Set<GroupMemberEntity> newMemberships = groups
            .stream()
            .map(e -> {
                GroupMemberEntity gm = new GroupMemberEntity();
                gm.setRealm(e.getKey());
                gm.setGroup(e.getValue());
                gm.setSubject(subject);
                return gm;
            })
            .collect(Collectors.toSet());

        Set<GroupMemberEntity> toDelete = oldMemberships
            .stream()
            .filter(gm -> !newMemberships.contains(gm))
            .collect(Collectors.toSet());
        Set<GroupMemberEntity> toAdd = newMemberships
            .stream()
            .filter(gm -> !oldMemberships.contains(gm))
            .collect(Collectors.toSet());

        // update
        groupMemberRepository.deleteAllInBatch(toDelete);
        groupMemberRepository.saveAll(toAdd);

        return getSubjectGroups(subject);
    }

    public void deleteSubjectFromGroups(String subject) {
        List<GroupMemberEntity> memberships = groupMemberRepository.findBySubject(subject);
        groupMemberRepository.deleteAll(memberships);
    }

    public void deleteSubjectFromGroups(String subject, String realm) {
        List<GroupMemberEntity> memberships = groupMemberRepository.findBySubjectAndRealm(subject, realm);
        groupMemberRepository.deleteAll(memberships);
    }

    /*
     * Converters
     */
    private Group toGroup(GroupEntity ge, Long size) {
        Group g = new Group();
        g.setGroupId(ge.getUuid());

        g.setRealm(ge.getRealm());
        g.setGroup(ge.getGroup());
        g.setParentGroup(ge.getParentGroup());

        g.setName(ge.getName());
        g.setDescription(ge.getDescription());

        g.setCreateDate(ge.getCreateDate());
        g.setModifiedDate(ge.getModifiedDate());

        g.setSize(size);

        return g;
    }

    private Group toGroup(GroupEntity ge, long size, Collection<GroupMemberEntity> members) {
        Group g = toGroup(ge, size);
        List<String> mm = members.stream().map(GroupMemberEntity::getSubject).collect(Collectors.toList());
        g.setMembers(mm);

        return g;
    }
}
