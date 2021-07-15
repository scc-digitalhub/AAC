package it.smartcommunitylab.aac.profiles.claims;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.attributes.persistence.AttributeEntity;
import it.smartcommunitylab.aac.attributes.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.profiles.extractor.AttributesProfileExtractor;

@Component
public class CustomProfileClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private final AttributeService attributeService;

    public CustomProfileClaimsExtractorProvider(AttributeService attributeService) {
        Assert.notNull(attributeService, "attribute service is required");
        this.attributeService = attributeService;
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public Collection<String> getScopes() {
        Set<String> res = new HashSet<>();

        attributeService.listAttributeSets().stream().forEach(a -> {
            res.add(buildScope(a.getIdentifier()));
        });

        return res;
    }

//    @Override
//    public Collection<ScopeClaimsExtractor> getExtractors() {
//        return Collections.singleton(extractor);
//    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        String id = extractId(scope);
        try {
            AttributeSetEntity set = attributeService.getAttributeSet(id);

            // TODO lookup custom extractors
            // TODO loading cache

            List<AttributeEntity> attributes = attributeService.listAttributes(id);

            // build a attribute extractor with keys matching the given set
            Map<String, Collection<String>> mapping = new HashMap<>();
            attributes.forEach(a -> {
                mapping.put(a.getKey(), Collections.singleton(id));
            });

            return new CustomProfileClaimsExtractor(new AttributesProfileExtractor(id, mapping));
        } catch (NoSuchAttributeSetException e) {
        }

        throw new IllegalArgumentException("invalid scope");
    }

    private String extractId(String scope) {
        if (scope.startsWith("profile.") && scope.endsWith(".me")) {
            return scope.substring(8, scope.length() - 3);
        }
        return scope;
    }

    private String buildScope(String id) {
        return "profile." + id + ".me";
    }

}
