package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

@Service
public class ProfileService {

    private final UserService userService;

    // converters
    private BasicProfileExtractor basicProfileExtractor;
    private OpenIdProfileExtractor openidProfileExtractor;
    private AccountProfileExtractor accountProfileExtractor;

    public ProfileService(UserService userService) {
        Assert.notNull(userService, "user service is required");
        this.userService = userService;

        // build extractors
        this.basicProfileExtractor = new BasicProfileExtractor();
        this.openidProfileExtractor = new OpenIdProfileExtractor();
        this.accountProfileExtractor = new AccountProfileExtractor();
    }

    /*
     * Profiles from userDetails
     */

    public BasicProfile getBasicProfile(UserDetails userDetails) throws InvalidDefinitionException {
        return getBasicProfile(userService.getUser(userDetails));
    }

    public OpenIdProfile getOpenIdProfile(UserDetails userDetails) throws InvalidDefinitionException {
        return getOpenIdProfile(userService.getUser(userDetails));

    }

    public Collection<AccountProfile> getAccountProfiles(UserDetails userDetails) throws InvalidDefinitionException {
        return getAccountProfiles(userService.getUser(userDetails));
    }

    /*
     * Profiles from user
     */
    public BasicProfile getBasicProfile(User user, String realm) throws InvalidDefinitionException {
        return getBasicProfile(userService.getUser(user, realm));
    }

    public OpenIdProfile getOpenIdProfile(User user, String realm) throws InvalidDefinitionException {
        return getOpenIdProfile(userService.getUser(user, realm));

    }

    public Collection<AccountProfile> getAccountProfiles(User user, String realm) throws InvalidDefinitionException {
        return getAccountProfiles(userService.getUser(user, realm));
    }

    private BasicProfile getBasicProfile(User user) throws InvalidDefinitionException {
        BasicProfile profile = basicProfileExtractor.extractUserProfile(user);

        if (profile == null) {
            throw new InvalidDefinitionException("invalid profile");
        }

        return profile;
    }

    private OpenIdProfile getOpenIdProfile(User user) throws InvalidDefinitionException {
        OpenIdProfile profile = openidProfileExtractor.extractUserProfile(user);

        if (profile == null) {
            throw new InvalidDefinitionException("invalid profile");
        }

        return profile;
    }

    private Collection<AccountProfile> getAccountProfiles(User user) throws InvalidDefinitionException {
        return accountProfileExtractor.extractUserProfiles(user);
    }

    /*
     * Profiles from db
     */

    public BasicProfile getBasicProfile(String realm, String subjectId)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getBasicProfile(user);
    }

    public OpenIdProfile getOpenIdProfile(String realm, String subjectId)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getOpenIdProfile(user);
    }

    public Collection<AccountProfile> getAccountProfiles(String realm, String subjectId)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getAccountProfiles(user);
    }
}
