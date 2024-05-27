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
import it.smartcommunitylab.aac.core.provider.ResolvableGenericsTypeProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

public class DefaultConfigMapConverter<M extends ConfigMap>
    implements Converter<Map<String, Serializable>, M>, ResolvableGenericsTypeProvider {

    protected static final ObjectMapper mapper = new ObjectMapper()
        .addMixIn(AbstractSettingsMap.class, NoTypes.class)
        .addMixIn(AbstractConfigMap.class, NoTypes.class);

    protected final JavaType configType;
    private ResolvableType ctype;
    private ResolvableType btype;

    public DefaultConfigMapConverter() {
        //extract type
        // resolve generics type via subclass trick
        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.configType = mapper.getTypeFactory().constructSimpleType((Class<?>) t, null);

        Assert.notNull(configType, "config type could not be extracted");
    }

    @Override
    public M convert(Map<String, Serializable> props) {
        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(props, configType);
    }

    public JavaType getConfigType() {
        return configType;
    }

    @Override
    @JsonIgnoreProperties
    public ResolvableType getResolvableType() {
        if (this.ctype == null) {
            try {
                this.ctype = ResolvableType.forClass(getClass());
            } catch (Exception e) {
                //ignore
            }
        }

        return ctype;
    }

    @Override
    @JsonIgnoreProperties
    public ResolvableType getResolvableType(int pos) {
        if (this.btype == null) {
            try {
                this.btype = ResolvableType.forClass(DefaultConfigMapConverter.class, getClass());
            } catch (Exception e) {
                //ignore
            }
        }

        return btype != null ? btype.getGeneric(pos) : null;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    static class NoTypes {}
}
