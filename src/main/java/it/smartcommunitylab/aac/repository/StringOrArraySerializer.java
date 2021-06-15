package it.smartcommunitylab.aac.repository;

import java.io.IOException;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class StringOrArraySerializer extends StdSerializer<Set<String>> {

    public StringOrArraySerializer() {
        super(Set.class, false);
    }

    @Override
    public void serialize(Set<String> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {

            if (value.size() == 1) {
                gen.writeString(value.iterator().next());
            } else {
                String values = StringUtils.collectionToDelimitedString(value, " ");
                gen.writeString(values);
            }

        }

    }

}
