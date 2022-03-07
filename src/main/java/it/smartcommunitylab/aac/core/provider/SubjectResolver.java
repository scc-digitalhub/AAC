package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.model.Subject;

public interface SubjectResolver {

    /*
     * Local id: direct resolve
     * 
     * we expect providers to be able to resolve subjects for persisted accounts
     */

    public Subject resolveByAccountId(String accountId);

    public Subject resolveByPrincipalId(String principalId);

    public Subject resolveByIdentityId(String identityId);

//    /*
//     * Identifying attributes
//     * 
//     * Multiple sets of attributes which can identify a user for a given provider.
//     * Each set, when fully populated, should suffice in finding the user.
//     * 
//     * To identify the provider consumers will need additional information
//     */
//
//    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes);
//
//    // disabled exposure of attribute keys
////    public Collection<Set<String>> getIdentifyingAttributes();
//
//    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal);

    /*
     * Account linking
     * 
     * A set of attributes which, when *all* matched by other resolvers, enables
     * linking to the same subject across providers. Usually a subset of identifying
     * attributes.
     */

    // TODO re-evaluate account linking for 2 scenarios:
    // multi-login and
    // additional-identity-fetch

//    public Subject resolveByLinkingAttributes(Map<String, String> attributes);
    public Subject resolveByAttributes(Map<String, String> attributes);

    // disabled exposure of attribute keys
//    public Collection<String> getLinkingAttributes();

//    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal);
    public Map<String, String> getAttributes(UserAuthenticatedPrincipal principal);

}
