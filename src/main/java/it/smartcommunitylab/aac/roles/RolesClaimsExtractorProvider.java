package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;

@Component
public class RolesClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private final RolesClaimsExtractor extractor;

    public RolesClaimsExtractorProvider() {
        this.extractor = new RolesClaimsExtractor();
    }

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_ROLE);
    }

//    @Override
//    public Collection<ScopeClaimsExtractor> getExtractors() {
//        return Collections.singleton(extractor);
//    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        if (!Config.SCOPE_ROLE.equals(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }

}
