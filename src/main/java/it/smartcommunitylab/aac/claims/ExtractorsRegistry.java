package it.smartcommunitylab.aac.claims;

import java.util.List;

public interface ExtractorsRegistry {
    public void registerExtractor(ScopeClaimsExtractor extractor);

    public void registerExtractor(ResourceClaimsExtractor extractor);

    public void unregisterExtractor(ScopeClaimsExtractor extractor);

    public void unregisterExtractor(ResourceClaimsExtractor extractor);

    public List<ResourceClaimsExtractor> getResourceExtractors(String resourceId);

    public List<ScopeClaimsExtractor> getScopeExtractors(String scope);

}
