package it.smartcommunitylab.aac.webauthn.model;


import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.yubico.webauthn.AssertionRequest;

public class AssertionRequestSerializer
        extends StdSerializer<AssertionRequest> {
    private static final long serialVersionUID = 1L;

    public AssertionRequestSerializer() {
        this(null);
    }

    public AssertionRequestSerializer(Class<AssertionRequest> t) {
        super(t);
    }

    @Override
    public void serialize(AssertionRequest value,
            JsonGenerator generator, SerializerProvider arg2) throws IOException {
        generator.writeRawValue(value.toCredentialsGetJson());
    }
}