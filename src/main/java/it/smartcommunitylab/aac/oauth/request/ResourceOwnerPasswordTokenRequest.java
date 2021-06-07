package it.smartcommunitylab.aac.oauth.request;

import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class ResourceOwnerPasswordTokenRequest extends TokenRequest {
    private final static String GRANT_TYPE = AuthorizationGrantType.PASSWORD.getValue();

    private String username;
    private String password;

    public ResourceOwnerPasswordTokenRequest(
            Map<String, String> requestParameters,
            String clientId,
            String username, String password,
            Collection<String> scope,
            Collection<String> resourceIds, Collection<String> audience) {
        super(requestParameters,
                clientId, GRANT_TYPE, scope,
                resourceIds, audience);
        Assert.hasText(username, "username is required");

        this.username = username;
        this.password = password;

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
