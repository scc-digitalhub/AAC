package it.smartcommunitylab.aac.oauth.auth;

import javax.servlet.http.HttpServletRequest;

import it.smartcommunitylab.aac.core.auth.ClientAuthenticationConverter;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;

public abstract class OAuth2ClientAuthenticationConverter implements ClientAuthenticationConverter {
    @Override
    public OAuth2ClientAuthenticationToken convert(HttpServletRequest request) {
        OAuth2ClientAuthenticationToken token = attemptConvert(request);
        if (token == null) {
            return null;
        }

        // collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        token.setDetails(webAuthenticationDetails);

        return token;

    }

    protected abstract OAuth2ClientAuthenticationToken attemptConvert(HttpServletRequest request);
}
