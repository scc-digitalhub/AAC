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

package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttributeProvider;
import it.smartcommunitylab.aac.attributes.mapper.BaseAttributesMapper;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

public class MapperAttributeProvider
    extends AbstractAttributeProvider<DefaultUserAttributesImpl, MapperAttributeProviderConfig, MapperAttributeProviderConfigMap> {

    // services
    private final AttributeService attributeService;
    private final AttributeStore attributeStore;

    public MapperAttributeProvider(
        String providerId,
        AttributeService attributeService,
        AttributeStore attributeStore,
        MapperAttributeProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_MAPPER, providerId, attributeService, providerConfig, realm);
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");

        this.attributeService = attributeService;
        this.attributeStore = attributeStore;

        // validate mapper type
        String type = providerConfig.getMapperType();
        if (!DefaultAttributesMapper.TYPE.equals(type) && !ExactAttributesMapper.TYPE.equals(type)) {
            throw new IllegalArgumentException("invalid mapper type");
        }

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

        List<UserAttributes> result = new ArrayList<>();
        Map<String, Serializable> principalAttributes = new HashMap<>();
        // get all attributes from principal
        Map<String, String> attributes = principal
            .getAttributes()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

        // TODO handle all attributes not only strings.
        principalAttributes.putAll(
            attributes.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
        );

        // we use also name from principal
        String name = principal.getName();
        principalAttributes.put("name", name);

        // add auth info
        principalAttributes.put("authority", principal.getAuthority());
        principalAttributes.put("provider", principal.getProvider());
        principalAttributes.put("realm", principal.getRealm());

        // fetch attribute sets
        for (String setId : config.getAttributeSets()) {
            try {
                AttributeSet as = attributeService.getAttributeSet(setId);

                // build mapper as per config
                BaseAttributesMapper mapper = getAttributeMapper(config.getMapperType(), as);
                AttributeSet set = mapper.mapAttributes(principalAttributes);
                if (set.getAttributes() != null && !set.getAttributes().isEmpty()) {
                    // build result
                    result.add(
                        new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set)
                    );
                }
            } catch (NoSuchAttributeSetException | RuntimeException e) {}
        }

        // store attributes as flat map from all sets
        Set<Entry<String, Serializable>> storeAttributes = new HashSet<>();
        for (UserAttributes ua : result) {
            for (Attribute a : ua.getAttributes()) {
                // TODO handle repeatable attributes by enum
                String key = ua.getIdentifier() + "|" + a.getKey();
                Entry<String, Serializable> es = new AbstractMap.SimpleEntry<>(key, a.getValue());
                storeAttributes.add(es);
            }
        }

        attributeStore.setAttributes(subjectId, storeAttributes);

        return result;
    }

    @Override
    public Collection<UserAttributes> getUserAttributes(String userId) {
        // fetch from store
        Map<String, Serializable> attributes = attributeStore.findAttributes(userId);
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserAttributes> result = new ArrayList<>();

        // build sets from stored values
        for (String setId : config.getAttributeSets()) {
            try {
                AttributeSet set = readAttributes(setId, attributes);
                if (set.getAttributes() != null && !set.getAttributes().isEmpty()) {
                    // build result
                    result.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, set));
                }
            } catch (NoSuchAttributeSetException | RuntimeException e) {}
        }

        return result;
    }

    @Override
    public UserAttributes getUserAttributes(String userId, String setId) throws NoSuchAttributeSetException {
        if (!config.getAttributeSets().contains(setId)) {
            return null;
        }

        // fetch from store
        Map<String, Serializable> attributes = attributeStore.findAttributes(userId);
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        AttributeSet set = readAttributes(setId, attributes);
        return new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, set);
    }

    @Override
    public void deleteUserAttributes(String subjectId) {
        // cleanup from store
        attributeStore.deleteAttributes(subjectId);
    }

    @Override
    public void deleteUserAttributes(String userId, String setId) {
        // nothing to do for mapper
    }

    private AttributeSet readAttributes(String setId, Map<String, Serializable> attributes)
        throws NoSuchAttributeSetException {
        AttributeSet as = attributeService.getAttributeSet(setId);
        String prefix = setId + "|";
        // TODO handle repeatable attributes by enum
        Map<String, Serializable> principalAttributes = attributes
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .collect(Collectors.toMap(e -> e.getKey().substring(prefix.length()), e -> e.getValue()));

        // use exact mapper
        ExactAttributesMapper mapper = new ExactAttributesMapper(as);
        AttributeSet set = mapper.mapAttributes(principalAttributes);
        return set;
    }

    private BaseAttributesMapper getAttributeMapper(String type, AttributeSet as) {
        if (DefaultAttributesMapper.TYPE.equals(type)) {
            return new DefaultAttributesMapper(as);
        } else if (ExactAttributesMapper.TYPE.equals(type)) {
            return new ExactAttributesMapper(as);
        }

        throw new IllegalArgumentException("invalid mapper type");
    }
}
