package it.smartcommunitylab.aac.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeConverter;

public class HashMapSerializableConverter implements AttributeConverter<Map<String, Serializable>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Serializable> map) {
        String json = null;
        if (map != null) {
            try {
                json = objectMapper.writeValueAsString(map);
            } catch (final JsonProcessingException e) {}
        }
        return json;
    }

    @Override
    public Map<String, Serializable> convertToEntityAttribute(String json) {
        Map<String, Serializable> map = null;
        if (json != null) {
            try {
                map = objectMapper.readValue(json, typeRef);
            } catch (final IOException e) {}
        }
        return map;
    }
}
