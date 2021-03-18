package it.smartcommunitylab.aac.oauth.token;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ExtendedAuthenticationManager;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public class ResourceOwnerPasswordTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "password";
    private boolean allowRefresh = true;

    private final AuthenticationManager authenticationManager;

    public ResourceOwnerPasswordTokenGranter(AuthenticationManager authenticationManager,
            AuthorizationServerTokenServices tokenServices, ClientDetailsService clientDetailsService,
            OAuth2RequestFactory requestFactory) {
        this(authenticationManager, tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected ResourceOwnerPasswordTokenGranter(AuthenticationManager authenticationManager,
            AuthorizationServerTokenServices tokenServices,
            ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
        this.authenticationManager = authenticationManager;
    }

    public void setAllowRefresh(boolean allowRefresh) {
        this.allowRefresh = allowRefresh;
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        OAuth2AccessToken token = super.grant(grantType, tokenRequest);

        if (token != null) {
            logger.trace("grant access token for client " + tokenRequest.getClientId() + " request "
                    + tokenRequest.getRequestParameters().toString());

            if (!allowRefresh) {
                AACOAuth2AccessToken norefresh = new AACOAuth2AccessToken(token);
                // The spec says that client credentials should not be allowed to get a refresh
                // token
                norefresh.setRefreshToken(null);
                token = norefresh;
                // TODO we should also remove the refresh token from DB
            }

        }
        return token;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {

        Map<String, String> parameters = new LinkedHashMap<String, String>(tokenRequest.getRequestParameters());
        String username = parameters.get("username");
        String password = parameters.get("password");
        // Protect from downstream leaks of password
        parameters.remove("password");

        AbstractAuthenticationToken passwordAuth = new UsernamePasswordAuthenticationToken(username, password);
        passwordAuth.setDetails(parameters);

        Authentication userAuth = passwordAuth;
        // workaround to select internal provider for realm
        // TODO rewrite and extend to identity proper provider or provide a fallback via
        // authmanager
        if (client instanceof OAuth2ClientDetails && authenticationManager instanceof ExtendedAuthenticationManager) {
            String realm = ((OAuth2ClientDetails) client).getRealm();
            userAuth = ((ExtendedAuthenticationManager) authenticationManager).wrapAuthentication(realm, passwordAuth);
        }

        try {
            userAuth = authenticationManager.authenticate(userAuth);
        } catch (AccountStatusException ase) {
            // covers expired, locked, disabled cases (mentioned in section 5.2, draft 31)
            throw new InvalidGrantException(ase.getMessage());
        } catch (BadCredentialsException e) {
            // If the username/password are wrong the spec says we should send 400/invalid
            // grant
            throw new InvalidGrantException(e.getMessage());
        } catch (AuthenticationException e) {
            // any other auth error we send 400/invalid
            throw new InvalidGrantException(e.getMessage());
        }
        if (userAuth == null || !userAuth.isAuthenticated()) {
            throw new InvalidGrantException("Could not authenticate user: " + username);
        }

        logger.trace("got oauth authentication from security context " + userAuth.toString());

        OAuth2Request storedOAuth2Request = getRequestFactory().createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, userAuth);

    }
}
