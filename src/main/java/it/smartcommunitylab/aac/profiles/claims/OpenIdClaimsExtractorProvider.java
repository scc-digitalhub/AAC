package it.smartcommunitylab.aac.profiles.claims;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;

@Component
public class OpenIdClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private static final Map<String, ScopeClaimsExtractor> extractors;

    static {
        Map<String, ScopeClaimsExtractor> e = new HashMap<>();
        e.put(Config.SCOPE_PROFILE, new OpenIdDefaultProfileClaimsExtractor());
        e.put(Config.SCOPE_EMAIL, new OpenIdEmailProfileClaimsExtractor());
        e.put(Config.SCOPE_ADDRESS, new OpenIdAddressProfileClaimsExtractor());
        e.put(Config.SCOPE_PHONE, new OpenIdPhoneProfileClaimsExtractor());

        extractors = e;
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".openid";
    }

    @Override
    public Collection<String> getScopes() {
        return extractors.keySet();
    }

    @Override
    public Collection<ScopeClaimsExtractor> getExtractors() {
        return extractors.values();
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
