package it.smartcommunitylab.aac.repository;

import java.io.IOException;
import java.util.Map;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HashMapConverter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> map) {

        String json = null;
        try {
            json = objectMapper.writeValueAsString(map);
        } catch (final JsonProcessingException e) {
        }

        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> convertToEntityAttribute(String json) {

        Map<String, String> map = null;
        try {
            map = objectMapper.readValue(json, Map.class);
        } catch (final IOException e) {
        }

        return map;
    }

}