package it.smartcommunitylab.aac.profiles.service;

import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

@Service
public class ProfileService {

    private final UserEntityService userService;

    public BasicProfile getBasicProfile(String subjectId) throws NoSuchUserException {
        // resolve subject
        UserEntity user = userService.getUser(subjectId);
        String realm = user.getRealm();
        
        // load identities

        // build profile
    }

}
