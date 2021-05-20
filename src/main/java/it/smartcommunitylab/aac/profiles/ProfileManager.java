package it.smartcommunitylab.aac.profiles;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import it.smartcommunitylab.aac.profiles.service.ProfileService;

@Service
public class ProfileManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthenticationHelper authHelper;

    /*
     * Current user, from context
     */

    public BasicProfile curBasicProfile() throws InvalidDefinitionException {
        return profileService.getBasicProfile(curUserDetails());
    }

    public OpenIdProfile curOpenIdProfile() throws InvalidDefinitionException {
        return profileService.getOpenIdProfile(curUserDetails());
    }

    public Collection<AccountProfile> curAccountProfiles() throws InvalidDefinitionException {
        return profileService.getAccountProfiles(curUserDetails());
    }

    /*
     * Users from db
     * 
     * TODO add authorization
     */

    public BasicProfile getBasicProfile(String realm, String subjectId)
            throws NoSuchUserException, InvalidDefinitionException {
        return profileService.getBasicProfile(realm, subjectId);
    }

    public OpenIdProfile getOpenIdProfile(String realm, String subjectId)
            throws NoSuchUserException, InvalidDefinitionException {
        return profileService.getOpenIdProfile(realm, subjectId);

    }

    public Collection<AccountProfile> getAccountProfiles(String realm, String subjectId)
            throws NoSuchUserException, InvalidDefinitionException {
        return profileService.getAccountProfiles(realm, subjectId);

    }

    /*
     * Helpers
     */

    private UserDetails curUserDetails() {
        UserDetails details = authHelper.getUserDetails();
        if (details == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        return details;
    }
}
