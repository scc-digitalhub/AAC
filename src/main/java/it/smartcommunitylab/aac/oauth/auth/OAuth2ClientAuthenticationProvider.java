package it.smartcommunitylab.aac.oauth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.crypto.PlaintextPasswordEncoder;
import it.smartcommunitylab.aac.oauth.ClientPKCEAuthenticationToken;
import it.smartcommunitylab.aac.oauth.ClientSecretAuthenticationToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientUserDetailsService;

public class OAuth2ClientAuthenticationProvider implements AuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OAuth2ClientDetailsService clientDetailsService;
    private final PasswordEncoder passwordEncoder;

    public OAuth2ClientAuthenticationProvider(OAuth2ClientDetailsService clientDetailsService) {
        Assert.notNull(clientDetailsService, "client details service is required");
        this.clientDetailsService = clientDetailsService;

        // build a plaintext password encoder, secrets are plaintext in oauth2
        passwordEncoder = PlaintextPasswordEncoder.getInstance();

    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(ClientSecretAuthenticationToken.class, authentication,
                "Only ClientSecretAuthenticationToken is supported");

        ClientSecretAuthenticationToken authRequest = (ClientSecretAuthenticationToken) authentication;
        String clientId = authRequest.getPrincipal();
        String clientSecret = authRequest.getCredentials();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        /*
         * We authenticate by comparing clientSecret via plainText encoder
         */

        // load details
        OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

        if (!this.passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            this.logger.debug("Failed to authenticate since secret does not match stored value");
            throw new BadCredentialsException("invalid authentication");
        }

        // result contains credentials, someone later on will need to call
        // eraseCredentials
        ClientSecretAuthenticationToken result = new ClientSecretAuthenticationToken(clientId, clientSecret,
                client.getAuthorities());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (ClientSecretAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
