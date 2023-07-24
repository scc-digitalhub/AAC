package it.smartcommunitylab.aac.claims;

import java.util.Collection;

/*
 * A registry for claims extractors.
 *
 * We don't expect providers to be immutable:
 * the registry implementations are supposed to retrieve extractors from providers at each request
 */
public interface ExtractorsRegistry {
    /*
     * Providers
     */
    public void registerExtractorProvider(ScopeClaimsExtractorProvider provider);

    public void registerExtractorProvider(ResourceClaimsExtractorProvider provider);

    public void unregisterExtractorProvider(ScopeClaimsExtractorProvider provider);

    public void unregisterExtractorProvider(ResourceClaimsExtractorProvider provider);

    /*
     * Extractors
     */

    public Collection<ResourceClaimsExtractor> getResourceExtractors(String resourceId);

    public Collection<ScopeClaimsExtractor> getScopeExtractors(String scope);
}
