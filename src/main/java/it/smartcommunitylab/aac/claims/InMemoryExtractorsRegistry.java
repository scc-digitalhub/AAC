package it.smartcommunitylab.aac.claims;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

public class InMemoryExtractorsRegistry implements ExtractorsRegistry {
    // claimExtractors
    // we keep a set for active extractors. Note that a single extractor can
    // respond to multiple scopes or resources
    // TODO export to a service to support clustered env, also use a load cache and
    // db store
    private Set<ScopeClaimsExtractor> scopeExtractors = new HashSet<>();
    private Set<ResourceClaimsExtractor> resourceExtractors = new HashSet<>();

    public InMemoryExtractorsRegistry(Collection<ScopeClaimsExtractor> scopeExtractors,
            Collection<ResourceClaimsExtractor> resourceExtractors) {
        for (ScopeClaimsExtractor se : scopeExtractors) {
            _registerExtractor(se);
        }
        for (ResourceClaimsExtractor re : resourceExtractors) {
            _registerExtractor(re);
        }
    }

    // TODO add locks when modifying extractor lists
    private void _registerExtractor(ScopeClaimsExtractor extractor) {
        scopeExtractors.add(extractor);
    }

    private void _registerExtractor(ResourceClaimsExtractor extractor) {
        resourceExtractors.add(extractor);
    }

    public void registerExtractor(ScopeClaimsExtractor extractor) {
        if (extractor != null && !CollectionUtils.isEmpty(extractor.getScopes())) {
            if (extractor.getResourceId() != null && extractor.getResourceId().startsWith("aac.")) {
                throw new IllegalArgumentException("core resources can not be registered");
            }

            _registerExtractor(extractor);
        }
    }

    public void registerExtractor(ResourceClaimsExtractor extractor) {
        if (extractor != null && !CollectionUtils.isEmpty(extractor.getResourceIds())) {
            _registerExtractor(extractor);
        }
    }

    public void unregisterExtractor(ResourceClaimsExtractor extractor) {
        resourceExtractors.remove(extractor);
    }

    public void unregisterExtractor(ScopeClaimsExtractor extractor) {
        scopeExtractors.remove(extractor);
    }

    @Override
    public List<ResourceClaimsExtractor> getResourceExtractors(String resourceId) {
        return resourceExtractors.stream().filter(e -> e.getResourceIds().contains(resourceId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScopeClaimsExtractor> getScopeExtractors(String scope) {
        return scopeExtractors.stream().filter(e -> e.getScopes().contains(scope)).collect(Collectors.toList());

    }
}
