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
import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;

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
import org.springframework.util.StringUtils;

public class ScriptAttributeProvider
    extends AbstractConfigurableResourceProvider<UserAttributes, ConfigurableAttributeProvider, ScriptAttributeProviderConfigMap, ScriptAttributeProviderConfig>
    implements AttributeProvider<ScriptAttributeProviderConfigMap, ScriptAttributeProviderConfig> {

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

    // services
    private final AttributeService attributeService;
    private final AttributeStore attributeStore;

    private ScriptExecutionService executionService;

    public ScriptAttributeProvider(
        String providerId,
        AttributeService attributeService,
        AttributeStore attributeStore,
        ScriptAttributeProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_SCRIPT, providerId, realm, providerConfig);
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");

        this.attributeService = attributeService;
        this.attributeStore = attributeStore;

        // validate code type
        String code = providerConfig.getConfigMap().getPlaintext();
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("empty function code");
        }

        // validate attribute sets, if empty nothing to do
        if (providerConfig.getAttributeSets().isEmpty()) {
            throw new IllegalArgumentException("no attribute sets enabled");
        }
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public Collection<UserAttributes> convertPrincipalAttributes(
        UserAuthenticatedPrincipal principal,
        String subjectId
    ) {
        if (config.getAttributeSets().isEmpty()) {
            return Collections.emptyList();
        }

        ScriptAttributeProviderConfigMap configMap = config.getConfigMap();

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

        Map<String, ExactAttributesMapper> mappers = new HashMap<>();

        // fetch attribute sets
        for (String setId : config.getAttributeSets()) {
            try {
                AttributeSet as = attributeService.getAttributeSet(setId);

                // build exact mapper
                mappers.put(as.getIdentifier(), new ExactAttributesMapper(as));
            } catch (NoSuchAttributeSetException | RuntimeException e) {}
        }

        // execute script and parse result
        try {
            // execute script
            String functionCode = configMap.getPlaintext();
            Map<String, Serializable> customAttributes = executionService.executeFunction(
                ATTRIBUTE_MAPPING_FUNCTION,
                functionCode,
                principalAttributes
            );

            if (customAttributes != null) {
                // we'll let each mapper parse the result set if present
                for (String id : customAttributes.keySet()) {
                    if (mappers.containsKey(id)) {
                        ExactAttributesMapper mapper = mappers.get(id);
                        AttributeSet set = mapper.mapAttributes((Map<String, Serializable>) customAttributes.get(id));
                        if (set.getAttributes() != null && !set.getAttributes().isEmpty()) {
                            // build result
                            result.add(
                                new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set)
                            );
                        }
                    }
                }
            }
        } catch (InvalidDefinitionException ex) {
            throw new RuntimeException(ex.getMessage());
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
    public Collection<UserAttributes> getUserAttributes(String subjectId) {
        // fetch from store
        Map<String, Serializable> attributes = attributeStore.findAttributes(subjectId);
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
                    result.add(
                        new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), subjectId, set)
                    );
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
        // nothing to do for script
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
}
