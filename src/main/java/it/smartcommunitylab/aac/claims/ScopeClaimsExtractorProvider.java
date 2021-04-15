package it.smartcommunitylab.aac.claims;

import java.util.Collection;

public interface ScopeClaimsExtractorProvider {

    public String getResourceId();

    public Collection<String> getScopes();

    @Deprecated
    public Collection<ScopeClaimsExtractor> getExtractors();

    public ScopeClaimsExtractor getExtractor(String scope);

}
