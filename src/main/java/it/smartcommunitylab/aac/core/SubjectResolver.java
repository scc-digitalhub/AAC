package it.smartcommunitylab.aac.core;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import it.smartcommunitylab.aac.common.NoSuchUserException;

public interface SubjectResolver {

    // user id suffices for a given authority
    public Subject resolveByUserId(String userId) throws NoSuchUserException;

    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException;

    public Collection<Set<String>> getIdentifyingAttributes();

}
