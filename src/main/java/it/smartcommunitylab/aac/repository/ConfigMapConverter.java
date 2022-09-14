package it.smartcommunitylab.aac.repository;

import java.io.Serializable;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public class ConfigMapConverter<T extends ConfigMap> implements Converter<Map<String, Serializable>, T> {
    protected static ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<T> classRef = new TypeReference<T>() {
    };

    @Override
    public T convert(Map<String, Serializable> source) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(source, classRef);
    }

}
