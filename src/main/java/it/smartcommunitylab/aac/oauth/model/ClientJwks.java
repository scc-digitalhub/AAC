package it.smartcommunitylab.aac.oauth.model;

import java.text.ParseException;

import javax.validation.Valid;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nimbusds.jose.jwk.JWKSet;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.oauth.persistence.AbstractOAuth2ClientResource;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientJwks extends AbstractOAuth2ClientResource implements ClientCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String jwks;

    public ClientJwks(String realm, String clientId, String jwks) {
        super(realm, clientId);
        Assert.notNull(jwks, "jwks can not be null");
        this.jwks = jwks;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS_JWKS;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return jwks;
    }

    public String getJwks() {
        return jwks;
    }

    @JsonIgnore
    public JWKSet getJwkSet() {
        // read from string
        if (!StringUtils.hasText(jwks)) {
            return null;
        }

        try {
            return JWKSet.parse(jwks);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public String getId() {
        return getClientId() + "." + getType();
    }

    @Override
    public void eraseCredentials() {
        this.jwks = null;
    }

}
