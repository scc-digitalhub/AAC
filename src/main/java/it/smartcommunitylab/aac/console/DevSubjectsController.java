/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.console;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.SubjectManager;
import it.smartcommunitylab.aac.groups.GroupManager;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.realms.RealmManager;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.templates.TemplatesManager;
import it.smartcommunitylab.aac.users.UserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Hidden
@RequestMapping("/console/dev")
public class DevSubjectsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RealmManager realmManager;

 

    @Autowired
    private SubjectManager subjectManager;

    @Autowired
    private TemplatesManager templatesManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private GroupManager groupManager;
    
    @Autowired
    private RealmRoleManager roleManager;

  

    

    /*
     * Realm subjects
     */
    @GetMapping("/subjects/{realm}")
    public Page<Subject> getRealmSubjects(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String t,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String role,
        Pageable pageRequest
    ) throws NoSuchRealmException {
        Set<String> types = StringUtils.hasText(t) ? StringUtils.commaDelimitedListToSet(t) : null;

        if(group != null) {
            //list members for group
            List<Subject> subjects = new ArrayList<>();
            long total = 0;
            try {
                String[] ids = groupManager.getGroupMembers(realm, group).toArray(new String[0]);
                total = ids.length;
                
                for(int i = 0; i< ids.length; i++) {
                    if(pageRequest == null || (i >= pageRequest.getOffset() && i <=pageRequest.getOffset()+pageRequest.getPageSize())) {
                        try {
                            subjects.add(subjectManager.getSubject(realm, ids[i]));
                        } catch (NoSuchSubjectException nue) {
                            //skip, registration could be stale
                        }                
                    }
                }

                if (types != null) {                
                    //filter
                    subjects = subjects.stream().filter(s -> types.contains(s.getType())).collect(Collectors.toList());
                }
            } catch (NoSuchGroupException e) {
                //ignore
            }
            return new PageImpl<>(subjects, pageRequest, total);

        }

        List<Subject> subjects = subjectManager.searchSubjects(realm, q, types);
        if(pageRequest == null ) {
            return new PageImpl<>(subjects, pageRequest, subjects.size());
        } else {
            int start = (int) Math.min(pageRequest.getOffset(),Integer.MAX_VALUE);
            int end = (int) Math.min(pageRequest.getOffset()+pageRequest.getPageSize(), subjects.size());
            return new PageImpl<>(subjects.subList(start,end), pageRequest, subjects.size());
        }




    }

    @GetMapping("/subjects/{realm}/{subjectId}")
    public Subject getRealmSubject(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchSubjectException {
        return subjectManager.getSubject(realm, subjectId);
    }

}
