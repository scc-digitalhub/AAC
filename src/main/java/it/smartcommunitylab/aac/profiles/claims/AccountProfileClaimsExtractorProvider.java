package it.smartcommunitylab.aac.profiles.claims;

import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;

@Component
public class AccountProfileClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private final AccountProfileClaimsExtractor extractor;

    public AccountProfileClaimsExtractorProvider() {
        this.extractor = new AccountProfileClaimsExtractor();
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".account";
    }

    @Override
    public Collection<String> getScopes() {
        return extractor.getScopes();
    }

    @Override
    public Collection<ScopeClaimsExtractor> getExtractors() {
        return Collections.singleton(extractor);
    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        if (extractor.getScopes().contains(scope)) {
            return extractor;
        }

        throw new IllegalArgumentException("invalid scope");
    }

}
