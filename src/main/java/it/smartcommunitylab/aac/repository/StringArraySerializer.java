package it.smartcommunitylab.aac.repository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.IOException;
import java.util.Set;
import org.springframework.util.StringUtils;

public class StringArraySerializer extends StdSerializer<Set<String>> {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public StringArraySerializer() {
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
