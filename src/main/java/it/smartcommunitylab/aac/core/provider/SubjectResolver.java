package it.smartcommunitylab.aac.core.provider;

import java.util.Map;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.model.Subject;

public interface SubjectResolver {

    /*
     * UserId
     * 
     * A globally addressable user identifier should suffices for a given authority
     * to dispatch requests to providers
     * 
     * Providers should be able to translate to internal id
     */

    // TODO return Optional<> instead of null
    public Subject resolveByUserId(String userId);

    /*
     * Identifying attributes
     * 
     * Multiple sets of attributes which can identify a user for a given provider.
     * Each set, when fully populated, should suffice in finding the user.
     * 
     * To identify the provider consumers will need additional information
     */

    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes);

    // disabled exposure of attribute keys
//    public Collection<Set<String>> getIdentifyingAttributes();

    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal);

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

    public Subject resolveByLinkingAttributes(Map<String, String> attributes);

    // disabled exposure of attribute keys
//    public Collection<String> getLinkingAttributes();

    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal);

}
