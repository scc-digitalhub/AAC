package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.model.Subject;

public interface SubjectResolver<A extends UserAccount> {

    /*
     * Local id: direct resolve
     * 
     * we expect providers to be able to resolve subjects for persisted accounts
     */

    public Subject resolveByAccountId(String accountId);

    public Subject resolveByPrincipalId(String principalId);

    public Subject resolveByIdentityId(String identityId);

    /*
     * Identifying attributes
     * 
     * an account identifier (outside local id) which is valid only for the same
     * provider to identify multiple accounts as belonging to the same user.
     * 
     * (ex. transient ids on responses, persistent value as attribute)
     *
     * we require idps to set identifying attribute as username
     */
    public Subject resolveByUsername(String accountId);

    /*
     * Account linking
     * 
     * A set of attributes which, when matched by other resolvers, enables linking
     * to the same subject across providers.
     * 
     * we require idps to provide resolution at least via email
     */
    public Subject resolveByEmailAddress(String accountId);

    /*
     * Attributes resolution
     * 
     * dynamic resolution via configurable attributes
     * 
     * DISABLED
     */
//    // TODO re-evaluate account linking for 2 scenarios:
//    // multi-login and
//    // additional-identity-fetch
//
////    public Subject resolveByLinkingAttributes(Map<String, String> attributes);
//    public Subject resolveByAttributes(Map<String, String> attributes);
//
//    // disabled exposure of attribute keys
////    public Collection<String> getLinkingAttributes();
//
////    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal);
//    public Map<String, String> getAttributes(UserAuthenticatedPrincipal principal);

}
