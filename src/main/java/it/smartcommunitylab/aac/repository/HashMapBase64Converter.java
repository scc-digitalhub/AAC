package it.smartcommunitylab.aac.repository;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HashMapBase64Converter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> map) {

        String json = null;
        if (map != null) {
            // build a map with encoded values
            Map<String, String> encoded = map.entrySet().stream()
                    .filter(e -> StringUtils.hasText(e.getValue()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> {
                        return Base64.getEncoder().encodeToString(e.getValue().getBytes());
                    }));

            try {
                json = objectMapper.writeValueAsString(encoded);
            } catch (final JsonProcessingException e) {
            }
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> convertToEntityAttribute(String json) {

        Map<String, String> map = null;
        if (json != null) {
            try {
                Map<String, String> encoded = objectMapper.readValue(json, Map.class);

                // decode values
                map = encoded.entrySet().stream()
                        .filter(e -> StringUtils.hasText(e.getValue()))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> {
                            return new String(Base64.getDecoder().decode(e.getValue()));
                        }));

            } catch (final IOException e) {
            }

        }
        return map;
    }

}