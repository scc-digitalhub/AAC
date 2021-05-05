package it.smartcommunitylab.aac.oauth.common;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.nimbusds.jwt.JWT;

import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;

/*
 * Serialize oauth2 token according to https://tools.ietf.org/html/rfc6749#section-5.1
 */

public class AACOAuth2AccessTokenSerializer extends StdSerializer<AACOAuth2AccessToken> {

    public AACOAuth2AccessTokenSerializer() {
        super(AACOAuth2AccessToken.class);
    }

    @Override
    public void serialize(AACOAuth2AccessToken token, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        // serialize with jackson
        gen.writeStartObject();

        gen.writeStringField(AACOAuth2AccessToken.TOKEN_TYPE, token.getTokenType());

        // access token is already encoded in value
        gen.writeStringField(AACOAuth2AccessToken.ACCESS_TOKEN, token.getValue());

        // refresh token
        OAuth2RefreshToken refreshToken = token.getRefreshToken();
        if (refreshToken != null) {
            gen.writeStringField(AACOAuth2AccessToken.REFRESH_TOKEN, refreshToken.getValue());
        }

        // id token
        JWT idToken = token.getIdToken();
        if (idToken != null) {
            gen.writeStringField(AACOAuth2AccessToken.ID_TOKEN, idToken.serialize());
        }

        // expires in as seconds
        Date expiration = token.getExpiration();
        if (expiration != null) {
            long now = System.currentTimeMillis();
            gen.writeNumberField(AACOAuth2AccessToken.EXPIRES_IN, (expiration.getTime() - now) / 1000);
        }

        // always include scope
        Set<String> scope = token.getScope();
        if (scope != null && !scope.isEmpty()) {
            StringBuffer scopes = new StringBuffer();
            for (String s : scope) {
                Assert.hasLength(s, "Scopes cannot be null or empty. Got " + scope + "");
                scopes.append(s);
                scopes.append(" ");
            }
            gen.writeStringField(AACOAuth2AccessToken.SCOPE, scopes.substring(0, scopes.length() - 1));
        }
        Map<String, Object> additionalInformation = token.getAdditionalInformation();
        for (String key : additionalInformation.keySet()) {
            gen.writeObjectField(key, additionalInformation.get(key));
        }
        gen.writeEndObject();
    }

}
