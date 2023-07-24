package it.smartcommunitylab.aac.oauth.request;

import com.nimbusds.jwt.SignedJWT;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import org.springframework.util.Assert;

public class ClientRegistrationRequest {

    /*
     * Client metadata
     */
    private final ClientRegistration registration;

    /*
     * Optional software statement as JWT
     */
    private final SignedJWT softwareStatement;

    public ClientRegistrationRequest(ClientRegistration registration) {
        this(registration, null);
    }

    public ClientRegistrationRequest(ClientRegistration registration, SignedJWT softwareStatement) {
        Assert.notNull(registration, "client registration can not be null");
        this.registration = registration;
        this.softwareStatement = softwareStatement;
    }

    public ClientRegistration getRegistration() {
        return registration;
    }

    public SignedJWT getSoftwareStatement() {
        return softwareStatement;
    }
}
