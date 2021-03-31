package it.smartcommunitylab.aac.profiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserTranslatorService;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import it.smartcommunitylab.aac.profiles.service.ProfileService;

@Service
public class ProfileManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserTranslatorService translator;

    @Autowired
    private AuthenticationHelper authHelper;

    /*
     * Current user, from context
     */

    public BasicProfile curBasicProfile() throws InvalidDefinitionException {
        User user = curUser();
        return profileService.getBasicProfile(user);
    }

    public OpenIdProfile curOpenIdProfile() throws InvalidDefinitionException {
        User user = curUser();
        return profileService.getOpenIdProfile(user);
    }

    /*
     * Helpers
     */

    private User curUser() {
        UserDetails details = authHelper.getUserDetails();
        if (details == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        String subjectId = details.getSubjectId();
        String source = details.getRealm();

        User user = new User(subjectId, details.getRealm());
        for (UserIdentity identity : details.getIdentities()) {
            user.addIdentity(identity);
        }
        for (UserAttributes attr : details.getAttributeSets()) {
            user.addAttributes(attr);
        }

        // TODO
//        user.setAuthorities();
//        user.setRoles(roles);

        return user;
    }
}
