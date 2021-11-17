package it.smartcommunitylab.aac.roles.claims;

import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;

@Component
public class SpacesClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private final SpacesClaimsExtractor extractor;

    public SpacesClaimsExtractorProvider() {
        this.extractor = new SpacesClaimsExtractor();
    }

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_USER_SPACES);
    }

//    @Override
//    public Collection<ScopeClaimsExtractor> getExtractors() {
//        return Collections.singleton(extractor);
//    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        if (!Config.SCOPE_USER_SPACES.equals(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }

}
