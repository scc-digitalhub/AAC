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

package it.smartcommunitylab.aac.repository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;

public class ConfigMapConverter<T extends ConfigMap> implements Converter<Map<String, Serializable>, T> {

    protected static final ObjectMapper mapper = new ObjectMapper();
    private final JavaType type;

    protected ConfigMapConverter() {
        // resolve generics type via subclass trick
        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.type = mapper.getTypeFactory().constructSimpleType((Class<?>) t, null);
    }

    public ConfigMapConverter(Class<T> c) {
        this.type = mapper.getTypeFactory().constructSimpleType(c, null);
    }

    @Override
    public T convert(Map<String, Serializable> source) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(source, type);
    }
}
