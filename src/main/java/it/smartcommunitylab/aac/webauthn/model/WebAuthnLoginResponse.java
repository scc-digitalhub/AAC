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
import com.yubico.webauthn.AssertionRequest;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnLoginResponse {

    @JsonProperty("key")
    @NotNull
    private String key;

    @JsonProperty("assertionRequest")
    @JsonSerialize(using = AssertionRequestSerializer.class)
    @NotNull
    AssertionRequest assertionrequest;

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AssertionRequest getAssertionRequest() {
        return this.assertionrequest;
    }

    public void setAssertionRequest(AssertionRequest assertionrequest) {
        this.assertionrequest = assertionrequest;
    }

}

class AssertionRequestSerializer
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