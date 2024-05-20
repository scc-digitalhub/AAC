/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.base.provider.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ResolvableGenericsTypeProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

@SuppressWarnings("unused")
public abstract class AbstractConfigurableConverter<
    P extends ProviderConfig<S, M>, C extends ConfigurableProvider<S>, S extends ConfigMap, M extends ConfigMap
>
    implements ResolvableGenericsTypeProvider {

    protected static final ObjectMapper mapper = new ObjectMapper()
        .addMixIn(AbstractSettingsMap.class, NoTypes.class)
        .addMixIn(AbstractConfigMap.class, NoTypes.class);

    protected ResolvableType ctype;
    protected JavaType providerType;
    protected JavaType configurableType;
    protected JavaType settingsType;
    protected JavaType configType;

    protected AbstractConfigurableConverter() {
        //always resolve ourself
        this.ctype = ResolvableType.forClass(AbstractConfigurableConverter.class, getClass());

        init();
    }

    protected void init() {
        //extract types
        this.providerType = _extractJavaType(0);
        this.configurableType = _extractJavaType(1);
        this.settingsType = _extractJavaType(2);
        this.configType = _extractJavaType(3);

        Assert.notNull(providerType, "settings type could not be extracted");
        Assert.notNull(configurableType, "settings type could not be extracted");
        Assert.notNull(settingsType, "settings type could not be extracted");
        Assert.notNull(configType, "config type could not be extracted");
    }

    protected JavaType _extractJavaType(int pos) {
        // resolve generics type via subclass trick
        Type superclazz = this.getClass().getGenericSuperclass();
        Type t = ((ParameterizedType) superclazz).getActualTypeArguments()[pos];
        return mapper.getTypeFactory().constructSimpleType((Class<?>) t, null);
    }

    public M getConfigMap(Map<String, Serializable> map) {
        Assert.notNull(configType, "settingsType can not be null");

        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(map, configType);
    }

    public S getSettingsMap(Map<String, Serializable> map) {
        Assert.notNull(settingsType, "settingsType can not be null");

        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(map, settingsType);
    }

    @Override
    @JsonIgnoreProperties
    public ResolvableType getResolvableType() {
        return ctype;
    }

    @Override
    @JsonIgnoreProperties
    public ResolvableType getResolvableType(int pos) {
        return ctype != null ? ctype.getGeneric(pos) : null;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    static class NoTypes {}
}
