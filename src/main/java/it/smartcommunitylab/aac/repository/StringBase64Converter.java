package it.smartcommunitylab.aac.repository;

import java.util.Base64;
import javax.persistence.AttributeConverter;

public class StringBase64Converter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String input) {

        String result = null;
        if (input != null) {
            result = Base64.getEncoder().withoutPadding().encodeToString(input.getBytes());
        }
        return result;
    }

    @Override
    public String convertToEntityAttribute(String input) {

        String result = null;
        if (input != null) {
            result = new String(Base64.getDecoder().decode(input.getBytes()));
        }
        return result;

    }

}