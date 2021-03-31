package it.smartcommunitylab.aac.profiles.service;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

@Service
public class ProfileService {

    private final UserService userService;

    // converters
    private BasicProfileExtractor basicProfileExtractor;
    private OpenIdProfileExtractor openidProfileExtractor;

    public ProfileService(UserService userService) {
        Assert.notNull(userService, "user service is required");
        this.userService = userService;

        // build extractors
        this.basicProfileExtractor = new BasicProfileExtractor();
        this.openidProfileExtractor = new OpenIdProfileExtractor();
    }

    public BasicProfile getBasicProfile(User user) throws InvalidDefinitionException {
        BasicProfile profile = basicProfileExtractor.extractUserProfile(user);

        if (profile == null) {
            throw new InvalidDefinitionException("invalid profile");
        }

        return profile;
    }

    public OpenIdProfile getOpenIdProfile(User user) throws InvalidDefinitionException {
        OpenIdProfile profile = openidProfileExtractor.extractUserProfile(user);

        if (profile == null) {
            throw new InvalidDefinitionException("invalid profile");
        }

        return profile;
    }

    public BasicProfile getBasicProfile(String subjectId, String realm)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getBasicProfile(user);
    }

    public OpenIdProfile getOpenIdProfile(String subjectId, String realm)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getOpenIdProfile(user);
    }

}
