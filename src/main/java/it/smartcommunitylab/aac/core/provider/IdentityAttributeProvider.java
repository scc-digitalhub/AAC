package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;

public interface IdentityAttributeProvider extends ResourceProvider {

    /*
     * Config
     */
    public String getName();

    public String getDescription();

    /*
     * User attributes
     * 
     * Multiple attribute sets bound to a given identity, authoritatively provided
     */

    public Collection<UserAttributes> convertPrincipalAttributes(UserAuthenticatedPrincipal principal);

    public Collection<UserAttributes> getAccountAttributes(String id);

    public void deleteAccountAttributes(String id);

}
