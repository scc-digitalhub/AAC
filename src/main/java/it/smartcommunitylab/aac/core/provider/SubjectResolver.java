package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import it.smartcommunitylab.aac.common.NoSuchUserException;
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

    public Subject resolveByUserId(String userId) throws NoSuchUserException;

    /*
     * Identifying attributes
     * 
     * Multiple sets of attributes which can identify a user for a given provider.
     * Each set, when fully populated, should suffice in finding the user.
     * 
     * To identify the provider consumers will need additional information
     */

    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException;

    public Collection<Set<String>> getIdentifyingAttributes();

    /*
     * Account linking
     * 
     * A set of attributes which, when matched by other resolvers, enables linking
     * to the same subject across providers. Usually a subset of identifying
     * attributes.
     */

    public Subject resolveByLinkingAttributes(Map<String, String> attributes) throws NoSuchUserException;

    public Collection<String> getLinkingAttributes();

}
