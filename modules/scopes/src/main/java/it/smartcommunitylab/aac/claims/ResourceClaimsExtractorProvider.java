package it.smartcommunitylab.aac.claims;

import java.util.Collection;

public interface ResourceClaimsExtractorProvider {
    public Collection<String> getResourceIds();

    @Deprecated
    public Collection<ResourceClaimsExtractor> getExtractors();

    public ResourceClaimsExtractor getExtractor(String resourceId);
}
