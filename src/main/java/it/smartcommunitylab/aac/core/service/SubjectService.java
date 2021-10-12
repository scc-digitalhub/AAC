package it.smartcommunitylab.aac.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.persistence.SubjectEntity;
import it.smartcommunitylab.aac.core.persistence.SubjectEntityRepository;
import it.smartcommunitylab.aac.model.Subject;

@Service
public class SubjectService {

    @Autowired
    private SubjectEntityRepository subjectRepository;

    public Subject getSubject(String uuid) throws NoSuchSubjectException {
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException();
        }

        return s;
    }

    public Subject findSubject(String uuid) {
        SubjectEntity s = subjectRepository.findBySubjectId(uuid);
        if (s == null) {
            return null;
        }

        return new Subject(s.getSubjectId(), s.getRealm(), s.getName());
    }

    public Subject getSubjectByClientId(String clientId) throws NoSuchSubjectException {
        SubjectEntity s = subjectRepository.findByClientId(clientId);
        if (s == null) {
            return null;
        }

        return new Subject(s.getSubjectId(), s.getRealm(), s.getName());
    }

    public Subject getSubjectByUserId(String userId) throws NoSuchSubjectException {
        SubjectEntity s = subjectRepository.findByUserId(userId);
        if (s == null) {
            return null;
        }

        return new Subject(s.getSubjectId(), s.getRealm(), s.getName());
    }
}
