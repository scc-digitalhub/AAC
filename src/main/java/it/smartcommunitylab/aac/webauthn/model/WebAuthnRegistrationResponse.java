package it.smartcommunitylab.aac.webauthn.model;

import java.io.IOException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnRegistrationResponse {

    @JsonProperty("key")
    @NotNull
    private String key;

    @JsonProperty("options")
    @JsonSerialize(using = PublicKeyCredentialCreationOptionsSerializer.class)
    @NotNull
    private PublicKeyCredentialCreationOptions options;

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public PublicKeyCredentialCreationOptions getOptions() {
        return this.options;
    }

    public void setOptions(PublicKeyCredentialCreationOptions options) {
        this.options = options;
    }

}

class PublicKeyCredentialCreationOptionsSerializer extends StdSerializer<PublicKeyCredentialCreationOptions> {
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