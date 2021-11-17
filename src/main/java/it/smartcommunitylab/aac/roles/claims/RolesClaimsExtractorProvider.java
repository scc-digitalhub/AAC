package it.smartcommunitylab.aac.roles.claims;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;

@Component
public class RolesClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private static final Map<String, ScopeClaimsExtractor> extractors;

    static {
        Map<String, ScopeClaimsExtractor> e = new HashMap<>();
        e.put(Config.SCOPE_USER_ROLE, new UserRolesClaimsExtractor());
        e.put(Config.SCOPE_CLIENT_ROLE, new ClientRolesClaimsExtractor());

        extractors = e;
    }

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return extractors.keySet();
    }
//    @Override
//    public Collection<ScopeClaimsExtractor> getExtractors() {
//        return Collections.singleton(extractor);
//    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        ScopeClaimsExtractor extractor = extractors.get(scope);
        if (extractor == null) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }

}
