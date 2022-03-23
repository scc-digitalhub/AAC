package it.smartcommunitylab.aac.core;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.Subject;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class SubjectManager {

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
        Realm r = realmService.getRealm(realm);
        return subjectService.listSubjects(r.getSlug());
    }

    @Transactional(readOnly = true)
    public List<Subject> searchSubjects(String realm, String query, Set<String> types) throws NoSuchRealmException {
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
