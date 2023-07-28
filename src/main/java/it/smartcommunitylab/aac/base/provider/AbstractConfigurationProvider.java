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

package it.smartcommunitylab.aac.base.provider;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.config.AbstractConfigurableProvider;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;

public abstract class AbstractConfigurationProvider<
    M extends AbstractConfigMap, T extends AbstractConfigurableProvider, C extends AbstractProviderConfig<M, T>
>
    implements ConfigurationProvider<M, T, C> {

    protected static final ObjectMapper mapper = new ObjectMapper().addMixIn(AbstractConfigMap.class, NoTypes.class);
    private final JavaType type;
    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    protected final String authority;
    protected M defaultConfigMap;

    protected AbstractConfigurationProvider(String authority) {
        Assert.hasText(authority, "authority id is mandatory");
        this.authority = authority;

        this.type = extractType();
        Assert.notNull(type, "type could not be extracted");
    }

    private JavaType extractType() {
        // resolve generics type via subclass trick
        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return mapper.getTypeFactory().constructSimpleType((Class<?>) t, null);
    }

    protected void setDefaultConfigMap(M defaultConfigMap) {
        this.defaultConfigMap = defaultConfigMap;
    }

    protected abstract C buildConfig(T cp);

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public C getConfig(T cp, boolean mergeDefault) {
        if (mergeDefault && defaultConfigMap != null) {
            // merge configMap with default on missing values
            Map<String, Serializable> map = new HashMap<>();
            map.putAll(cp.getConfiguration());

            Map<String, Serializable> defaultMap = defaultConfigMap.getConfiguration();
            defaultMap
                .entrySet()
                .forEach(e -> {
                    map.putIfAbsent(e.getKey(), e.getValue());
                });

            cp.setConfiguration(map);
        }

        return buildConfig(cp);
    }

    @Override
    public C getConfig(T cp) {
        return getConfig(cp, true);
    }

    @Override
    public M getDefaultConfigMap() {
        return defaultConfigMap;
    }

    @Override
    public M getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        M m = mapper.convertValue(map, type);
        return m;
    }

    protected Map<String, Serializable> getConfiguration(M configMap) {
        if (configMap == null) {
            return Collections.emptyMap();
        }

        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(configMap, typeRef);
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return defaultConfigMap.getSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    static class NoTypes {}
}
