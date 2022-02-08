package it.smartcommunitylab.aac.group.claims;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.group.scopes.ClientGroupsScope;
import it.smartcommunitylab.aac.group.scopes.GroupsResource;
import it.smartcommunitylab.aac.group.scopes.UserGroupsScope;

@Component
public class GroupsClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private static final Map<String, ScopeClaimsExtractor> extractors;

    static {
        Map<String, ScopeClaimsExtractor> e = new HashMap<>();
        e.put(UserGroupsScope.SCOPE, new UserGroupsClaimsExtractor());
        e.put(ClientGroupsScope.SCOPE, new ClientGroupsClaimsExtractor());

        extractors = e;
    }

    @Override
    public String getResourceId() {
        return GroupsResource.RESOURCE_ID;
    }

    @Override
    public Collection<String> getScopes() {
        return extractors.keySet();
    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        ScopeClaimsExtractor extractor = extractors.get(scope);
        if (extractor == null) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }

}
