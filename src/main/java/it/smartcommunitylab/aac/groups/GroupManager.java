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

package it.smartcommunitylab.aac.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.groups.model.Group;
import it.smartcommunitylab.aac.groups.persistence.GroupEntity;
import it.smartcommunitylab.aac.groups.service.GroupService;
import it.smartcommunitylab.aac.model.Subject;

/**
 * @author raman
 *
 */
@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class GroupManager {
//    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private GroupService groupService;

//    @Autowired
//    private RealmService realmService;

    @Autowired
    private SubjectService subjectService;

    /*
     * Realm groups
     */
    @Transactional(readOnly = true)
    public Collection<Group> getGroups(String realm)
            throws NoSuchRealmException {
        return groupService.listGroups(realm);

    }

    @Transactional(readOnly = true)
    public Group getGroup(String realm, String groupId, boolean withMembers)
            throws NoSuchRealmException, NoSuchGroupException {
        Group g = groupService.getGroup(groupId, withMembers);
        if (!realm.equals(g.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        return g;
    }

    public Group addGroup(String realm, Group g) throws NoSuchRealmException {
        String group = g.getGroup();
        if (!StringUtils.hasText(group)) {
            throw new IllegalArgumentException("group can not be null or empty");
        }

        group = Jsoup.clean(group, Safelist.none());

        String groupId = g.getGroupId();
        String parentGroup = g.getParentGroup();
        String name = g.getName();
        String description = g.getDescription();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        Group res = groupService.addGroup(groupId, realm, group, parentGroup, name, description);

        // add memberships if defined
        if (g.getMembers() != null) {
            Collection<String> members = groupService.setGroupMembers(realm, group, g.getMembers());
            res.setMembers(new ArrayList<>(members));
        }

        return res;
    }

    public Group updateGroup(String realm, String groupId, Group g)
            throws NoSuchRealmException, NoSuchGroupException {

        Group gl = groupService.getGroup(groupId);
        if (!realm.equals(gl.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        String group = g.getGroup();
        String parentGroup = g.getParentGroup();
        String name = g.getName();
        String description = g.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        Group res = groupService.updateGroup(groupId, realm, group, parentGroup, name, description);

        // update memberships if defined
        if (g.getMembers() != null) {
            Collection<String> members = groupService.setGroupMembers(realm, group, g.getMembers());
            res.setMembers(new ArrayList<>(members));
        }

        return res;
    }

    public Group renameGroup(String realm, String groupId, String group)
            throws NoSuchRealmException, NoSuchGroupException {

        Group gl = groupService.getGroup(groupId);
        if (!realm.equals(gl.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        group = Jsoup.clean(group, Safelist.none());

        return groupService.renameGroup(groupId, realm, group);
    }

    public void deleteGroup(String realm, String groupId) throws NoSuchRealmException, NoSuchGroupException {
        Group gl = groupService.getGroup(groupId);
        if (gl != null) {
            if (!realm.equals(gl.getRealm())) {
                throw new IllegalArgumentException("realm mismatch");
            }

            groupService.deleteGroup(groupId);
        }
    }

    @Transactional(readOnly = true)
    public Page<Group> getGroups(String realm, Pageable pageRequest) throws NoSuchRealmException {
        return groupService.listGroups(realm, pageRequest);
    }

    @Transactional(readOnly = true)
    public Collection<Group> getGroupsByParent(String realm, String parentGroup) throws NoSuchRealmException {
        return groupService.listGroupsByParentGroup(realm, parentGroup);
    }

    @Transactional(readOnly = true)
    public Page<Group> searchGroupsWithSpec(String realm, Specification<GroupEntity> spec, PageRequest pageRequest)
            throws NoSuchRealmException {
        // TODO accept query spec for group not entity!
        return groupService.searchGroupsWithSpec(realm, spec, pageRequest, false);
    }

    /*
     * Group membership
     */
    public Collection<String> getGroupMembers(String realm, String group)
            throws NoSuchRealmException, NoSuchGroupException {
        return groupService.getGroupMembers(realm, group);
    }

    public String addGroupMember(String realm, String group, String subject)
            throws NoSuchRealmException, NoSuchGroupException, NoSuchSubjectException {
        // check if subject exists
        Subject s = subjectService.getSubject(subject);

        // TODO evaluate checking if subject matches the realm
        return groupService.addGroupMember(realm, group, s.getSubjectId());
    }

    public Collection<String> addGroupMembers(String realm, String group, Collection<String> subjects)
            throws NoSuchRealmException, NoSuchGroupException, NoSuchSubjectException {

        // check if all subjects exists
        if (subjects.stream().anyMatch(s -> (subjectService.findSubject(s) == null))) {
            throw new NoSuchSubjectException();
        }

        // TODO evaluate checking if subjects match the realm
        subjects.stream()
                .map(s -> groupService.addGroupMember(realm, group, s))
                .collect(Collectors.toList());

        return groupService.getGroupMembers(realm, group);
    }

    public Collection<String> setGroupMembers(String realm, String group, Collection<String> subjects)
            throws NoSuchRealmException, NoSuchGroupException, NoSuchSubjectException {

        // check if all subjects exists
        if (subjects.stream().anyMatch(s -> (subjectService.findSubject(s) == null))) {
            throw new NoSuchSubjectException();
        }

        // TODO evaluate checking if subjects match the realm
        return groupService.setGroupMembers(realm, group, subjects);
    }

    public void removeGroupMember(String realm, String group, String subject)
            throws NoSuchRealmException, NoSuchGroupException {
        groupService.removeGroupMember(realm, group, subject);
    }

    public Collection<String> removeGroupMembers(String realm, String group, Collection<String> subjects)
            throws NoSuchRealmException, NoSuchGroupException {
        subjects.stream()
                .forEach(s -> groupService.removeGroupMember(realm, group, s));

        return groupService.getGroupMembers(realm, group);
    }

    /*
     * Subject groups
     */
    public Collection<Group> curSubjectGroups(String realm) {
        Authentication auth = authHelper.getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("invalid or missing authentication");
        }

        String subjectId = auth.getName();
        return groupService.getSubjectGroups(subjectId, realm);
    }

    @Transactional(readOnly = true)
    public Collection<Group> getSubjectGroups(String realm, String subject)
            throws NoSuchSubjectException, NoSuchRealmException {
        return groupService.getSubjectGroups(subject, realm);
    }

    public Collection<Group> setSubjectGroups(String realm, String subject, Collection<Group> groups)
            throws NoSuchSubjectException, NoSuchRealmException, NoSuchGroupException {

        // check if subject exists
        Subject s = subjectService.getSubject(subject);

        // unpack
        List<String> toSet = groups.stream()
                .filter(r -> realm.equals(r.getRealm()) || r.getRealm() == null)
                .filter(r -> groupService.findGroup(realm, r.getGroup()) != null)
                .map(r -> r.getGroup()).collect(Collectors.toList());

        return groupService.setSubjectGroups(s.getSubjectId(), realm, toSet);

    }

    public void deleteSubjectFromGroups(String realm, String subject) throws NoSuchSubjectException {
        groupService.deleteSubjectFromGroups(subject, realm);
    }

}
