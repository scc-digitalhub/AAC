/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalAttributeEntity;
import it.smartcommunitylab.aac.internal.service.InternalAttributeEntityService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

public class InternalAttributeService
    extends AbstractConfigurableResourceProvider<UserAttributes, ConfigurableAttributeProvider, InternalAttributeProviderConfigMap, InternalAttributeProviderConfig>
    implements
        it.smartcommunitylab.aac.core.provider.AttributeService<InternalAttributeProviderConfigMap, InternalAttributeProviderConfig> {

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

    // services
    private final AttributeService attributeService;
    private final InternalAttributeEntityService attributeEntityService;

    public InternalAttributeService(
        String providerId,
        AttributeService attributeService,
        InternalAttributeEntityService attributeEntityService,
        InternalAttributeProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm, providerConfig);
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(attributeEntityService, "attribute entity service is mandatory");

        this.attributeService = attributeService;
        this.attributeEntityService = attributeEntityService;

        // validate attribute sets, if empty nothing to do
        if (providerConfig.getAttributeSets().isEmpty()) {
            throw new IllegalArgumentException("no attribute sets enabled");
        }
    }

    @Override
    public Collection<UserAttributes> convertPrincipalAttributes(
        UserAuthenticatedPrincipal principal,
        String subjectId
    ) {
        if (config.getAttributeSets().isEmpty()) {
            return Collections.emptyList();
        }

        // nothing to process, just fetch attributes already in store
        return getUserAttributes(subjectId);
    }

    @Override
    public Collection<UserAttributes> getUserAttributes(String subjectId) {
        List<UserAttributes> result = new ArrayList<>();

        // build sets from stored values
        for (String setId : config.getAttributeSets()) {
            try {
                AttributeSet as = attributeService.getAttributeSet(setId);
                // fetch from store
                List<InternalAttributeEntity> attributes = attributeEntityService.findAttributes(
                    getProvider(),
                    subjectId,
                    setId
                );

                // translate to set
                if (!attributes.isEmpty()) {
                    // TODO handle repeatable as enum
                    Map<String, Serializable> principalAttributes = attributes
                        .stream()
                        .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

                    // use exact mapper
                    ExactAttributesMapper mapper = new ExactAttributesMapper(as);
                    AttributeSet set = mapper.mapAttributes(principalAttributes);

                    // build result
                    result.add(
                        new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set)
                    );
                }
            } catch (NoSuchAttributeSetException | RuntimeException e) {}
        }

        return result;
    }

    @Override
    public UserAttributes getUserAttributes(String subjectId, String setId) throws NoSuchAttributeSetException {
        if (!config.getAttributeSets().contains(setId)) {
            throw new IllegalArgumentException("set not enabled for this provider " + setId);
        }

        // build set from stored values

        AttributeSet as = attributeService.getAttributeSet(setId);
        // fetch from store
        List<InternalAttributeEntity> attributes = attributeEntityService.findAttributes(
            getProvider(),
            subjectId,
            setId
        );

        // translate to set
        // TODO handle repeatable as enum
        Map<String, Serializable> principalAttributes = attributes
            .stream()
            .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

        // use exact mapper
        ExactAttributesMapper mapper = new ExactAttributesMapper(as);
        AttributeSet set = mapper.mapAttributes(principalAttributes);

        // build result
        return new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set);
    }

    @Override
    public void deleteUserAttributes(String subjectId) {
        // cleanup from store
        attributeEntityService.deleteAttributes(getProvider(), subjectId);
    }

    @Override
    public void deleteUserAttributes(String subjectId, String setId) {
        // cleanup matching from store
        attributeEntityService.deleteAttribute(getProvider(), subjectId, setId);
    }

    @Override
    public Collection<UserAttributes> putUserAttributes(String subjectId, Collection<AttributeSet> attributeSets) {
        List<UserAttributes> result = new ArrayList<>();

        // fetch sets and validate
        for (AttributeSet as : attributeSets) {
            AttributeSet set = setAttributes(subjectId, as);
            // build result
            result.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set));
        }

        return result;
    }

    @Override
    public UserAttributes putUserAttributes(String subjectId, String setId, AttributeSet attributeSet) {
        // check match
        if (!attributeSet.getIdentifier().equals(setId)) {
            throw new IllegalArgumentException("set id mismatch");
        }

        AttributeSet set = setAttributes(subjectId, attributeSet);

        // build result
        return new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set);
    }

    private AttributeSet setAttributes(String subjectId, AttributeSet as) {
        if (!config.getAttributeSets().contains(as.getIdentifier())) {
            throw new IllegalArgumentException("set not enabled for this provider " + as.getIdentifier());
        }

        // unpack and save to store
        Collection<Attribute> attrs = as.getAttributes();

        // each set will overwrite previously stored values
        List<InternalAttributeEntity> attributes = attributeEntityService.setAttributes(
            getProvider(),
            subjectId,
            as.getIdentifier(),
            attrs
        );
        // translate to set
        // TODO handle repeatable as enum
        Map<String, Serializable> principalAttributes = attributes
            .stream()
            .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

        // use exact mapper
        ExactAttributesMapper mapper = new ExactAttributesMapper(as);
        AttributeSet set = mapper.mapAttributes(principalAttributes);
        return set;
    }
}
