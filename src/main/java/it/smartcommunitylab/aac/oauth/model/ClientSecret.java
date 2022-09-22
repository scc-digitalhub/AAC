package it.smartcommunitylab.aac.oauth.model;

import javax.validation.Valid;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.oauth.persistence.AbstractOAuth2ClientResource;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientSecret extends AbstractOAuth2ClientResource implements ClientCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String secret;

    public ClientSecret(String realm, String clientId, String secret) {
        super(realm, clientId);
        Assert.notNull(secret, "secret can not be null");
        this.secret = secret;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS_SECRET;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return secret;
    }

    public String getClientSecret() {
        return secret;
    }

    @Override
    public String getId() {
        return getClientId() + "." + getType();
    }

    @Override
    public void eraseCredentials() {
        this.secret = null;
    }

}
