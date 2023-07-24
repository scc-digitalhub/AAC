package it.smartcommunitylab.aac.oauth.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.nimbusds.jwt.JWT;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.util.StringUtils;

/*
 * Serialize oauth2 token according to https://tools.ietf.org/html/rfc6749#section-5.1
 */

public class AACOAuth2AccessTokenSerializer extends StdSerializer<AACOAuth2AccessToken> {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public AACOAuth2AccessTokenSerializer() {
        super(AACOAuth2AccessToken.class);
    }

    @Override
    public void serialize(AACOAuth2AccessToken token, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
        // serialize with jackson
        gen.writeStartObject();

        // token_type is REQUIRED
        gen.writeStringField(AACOAuth2AccessToken.TOKEN_TYPE, token.getTokenType());

        // access token is already encoded in value
        gen.writeStringField(AACOAuth2AccessToken.ACCESS_TOKEN, token.getValue());

        // expires_in is RECOMMENDED (as seconds)
        Date expiration = token.getExpiration();
        if (expiration != null) {
            long now = System.currentTimeMillis();
            gen.writeNumberField(AACOAuth2AccessToken.EXPIRES_IN, (expiration.getTime() - now) / 1000);
        }

        // refresh token OPTIONAL if provided
        OAuth2RefreshToken refreshToken = token.getRefreshToken();
        if (refreshToken != null) {
            gen.writeStringField(AACOAuth2AccessToken.REFRESH_TOKEN, refreshToken.getValue());
        }

        // id token OPTIONAL if provided
        JWT idToken = token.getIdToken();
        if (idToken != null) {
            gen.writeStringField(AACOAuth2AccessToken.ID_TOKEN, idToken.serialize());
        }

        // as per spec, scope is OPTIONAL if identical to request, otherwise REQUIRED.
        // Since we don't have access to request here we always add in response
        Set<String> scope = token.getScope();

        if (scope == null) {
            scope = Collections.emptySet();
        }

        gen.writeStringField(AACOAuth2AccessToken.SCOPE, StringUtils.collectionToDelimitedString(scope, " "));

        // add additional fields as presented in map
        Map<String, Object> additionalInformation = token.getAdditionalInformation();
        for (String key : additionalInformation.keySet()) {
            gen.writeObjectField(key, additionalInformation.get(key));
        }
        gen.writeEndObject();
    }
}
