package it.smartcommunitylab.aac.webauthn.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

public class PublicKeyCredentialCreationOptionsSerializer extends StdSerializer<PublicKeyCredentialCreationOptions> {
    private static final long serialVersionUID = 1L;

    public PublicKeyCredentialCreationOptionsSerializer() {
        this(null);
    }

    public PublicKeyCredentialCreationOptionsSerializer(Class<PublicKeyCredentialCreationOptions> t) {
        super(t);
    }

    @Override
    public void serialize(PublicKeyCredentialCreationOptions value,
            JsonGenerator generator, SerializerProvider arg2) throws IOException {
        generator.writeRawValue(value.toCredentialsCreateJson());
    }
}