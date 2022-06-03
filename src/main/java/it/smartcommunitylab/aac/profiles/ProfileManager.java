package it.smartcommunitylab.aac.profiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserProfile;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;
import it.smartcommunitylab.aac.profiles.model.MultiProfile;
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
     * Current user, from context - shortcuts
     * 
     * TODO rework with SPEL expression
     */

    public BasicProfile curBasicProfile() throws InvalidDefinitionException {
        return (BasicProfile) profileService.getProfile(curUserDetails(), BasicProfile.IDENTIFIER);
    }

    public OpenIdProfile curOpenIdProfile() throws InvalidDefinitionException {
        return (OpenIdProfile) profileService.getProfile(curUserDetails(), OpenIdProfile.IDENTIFIER);
    }

    public EmailProfile curEmailProfile() throws InvalidDefinitionException {
        return (EmailProfile) profileService.getProfile(curUserDetails(), EmailProfile.IDENTIFIER);
    }

    public MultiProfile<AccountProfile> curAccountProfiles() throws InvalidDefinitionException {
        return (MultiProfile<AccountProfile>) profileService.getProfile(curUserDetails(), AccountProfile.IDENTIFIER);
    }

    /*
     * Users from db
     */

    public UserProfile getProfile(String realm, String userId, String identifier)
            throws NoSuchUserException, InvalidDefinitionException {
        return profileService.getProfile(realm, userId, identifier);
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
