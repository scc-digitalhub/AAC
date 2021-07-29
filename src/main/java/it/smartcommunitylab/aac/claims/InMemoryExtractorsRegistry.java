package it.smartcommunitylab.aac.claims;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class InMemoryExtractorsRegistry implements ExtractorsRegistry {
    // claimExtractors
    // we keep a set for active extractors. Note that a single extractor can
    // respond to multiple scopes or resources
    // TODO export to a service to support clustered env, also use a load cache and
    // db store
    private Set<ScopeClaimsExtractorProvider> scopeExtractorsProviders = new HashSet<>();
    private Set<ResourceClaimsExtractorProvider> resourceExtractorsProviders = new HashSet<>();

    public InMemoryExtractorsRegistry(Collection<ScopeClaimsExtractorProvider> scopeExtractorsProviders,
            Collection<ResourceClaimsExtractorProvider> resourceExtractorsProviders) {
        for (ScopeClaimsExtractorProvider se : scopeExtractorsProviders) {
            _registerProvider(se);
        }
        for (ResourceClaimsExtractorProvider re : resourceExtractorsProviders) {
            _registerProvider(re);
        }
    }

    // TODO add locks when modifying extractor lists
    private void _registerProvider(ScopeClaimsExtractorProvider extractor) {
        scopeExtractorsProviders.add(extractor);
    }

    private void _registerProvider(ResourceClaimsExtractorProvider extractor) {
        resourceExtractorsProviders.add(extractor);
    }

    /*
     * Providers
     */
    @Override
    public void registerExtractorProvider(ScopeClaimsExtractorProvider provider) {
        if (provider != null) {

            if (provider.getResourceId() == null || provider.getResourceId().startsWith("aac.")) {
                throw new IllegalArgumentException("core resources can not be registered");
            }

            _registerProvider(provider);
        }
    }

    @Override
    public void registerExtractorProvider(ResourceClaimsExtractorProvider provider) {
        if (provider != null) {

            if (provider.getResourceIds() == null
                    || provider.getResourceIds().stream().anyMatch(r -> r.startsWith("aac."))) {
                throw new IllegalArgumentException("core resources can not be registered");
            }

            _registerProvider(provider);
        }
    }

    @Override
    public void unregisterExtractorProvider(ResourceClaimsExtractorProvider extractor) {
        resourceExtractorsProviders.remove(extractor);
    }

    @Override
    public void unregisterExtractorProvider(ScopeClaimsExtractorProvider extractor) {
        scopeExtractorsProviders.remove(extractor);
    }

    /*
     * Extractors
     */

    @Override
    public Set<ResourceClaimsExtractor> getResourceExtractors(String resourceId) {
        Set<ResourceClaimsExtractor> extractors = new HashSet<>();
        resourceExtractorsProviders.stream()
                .forEach(p -> {
                    if (p.getResourceIds().contains(resourceId)) {
                        ResourceClaimsExtractor r = p.getExtractor(resourceId);
                        if (r != null) {
                            extractors.add(r);
                        }
                    }
                });

        return extractors;

    }

    @Override
    public Set<ScopeClaimsExtractor> getScopeExtractors(String scope) {
        Set<ScopeClaimsExtractor> extractors = new HashSet<>();
        scopeExtractorsProviders.stream()
                .forEach(p -> {
                    if (p.getScopes().contains(scope)) {
                        ScopeClaimsExtractor s = p.getExtractor(scope);
                        if (s != null) {
                            extractors.add(s);
                        }
                    }
                });

        return extractors;

    }
}
