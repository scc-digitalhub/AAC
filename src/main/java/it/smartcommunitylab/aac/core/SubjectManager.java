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

package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.Subject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class SubjectManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private RealmService realmService;

    @Transactional(readOnly = true)
    public Subject findSubject(String realm, String id) {
        Subject s = subjectService.findSubject(id);
        if (s == null) {
            return null;
        }

        // check realm match
        if (!s.getRealm().equals(realm)) {
            return null;
        }

        return s;
    }

    @Transactional(readOnly = true)
    public Subject getSubject(String realm, String id) throws NoSuchSubjectException, NoSuchRealmException {
        logger.debug(
            "get subject {} for realm {}",
            StringUtils.trimAllWhitespace(id),
            StringUtils.trimAllWhitespace(realm)
        );
        Realm r = realmService.getRealm(realm);
        Subject s = subjectService.getSubject(id);

        // check realm match
        if (!s.getRealm().equals(r.getSlug())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        return s;
    }

    @Transactional(readOnly = true)
    public List<Subject> listSubjects(String realm) throws NoSuchRealmException {
        logger.debug("list subjects for realm {}", StringUtils.trimAllWhitespace(realm));
        Realm r = realmService.getRealm(realm);
        return subjectService.listSubjects(r.getSlug());
    }

    @Transactional(readOnly = true)
    public List<Subject> searchSubjects(String realm, String keywords, Set<String> types) throws NoSuchRealmException {
        logger.debug(
            "search subjects for realm {} with query {}",
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(keywords)
        );

        String query = StringUtils.trimAllWhitespace(keywords);
        Realm r = realmService.getRealm(realm);

        // enforce min length on query
        if (StringUtils.hasText(query) && query.length() < 3) {
            query = null;
        }

        List<Subject> subjects = subjectService.searchSubjects(r.getSlug(), query);
        if (types != null) {
            return subjects.stream().filter(s -> types.contains(s.getType())).collect(Collectors.toList());
        }

        return subjects;
    }
}
