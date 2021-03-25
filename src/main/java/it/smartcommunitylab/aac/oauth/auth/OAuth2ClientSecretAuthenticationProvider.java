package it.smartcommunitylab.aac.oauth.auth;

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
import it.smartcommunitylab.aac.oauth.model.AuthenticationScheme;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientUserDetailsService;

public class OAuth2ClientSecretAuthenticationProvider implements AuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OAuth2ClientDetailsService clientDetailsService;
    private final PasswordEncoder passwordEncoder;

    public OAuth2ClientSecretAuthenticationProvider(OAuth2ClientDetailsService clientDetailsService) {
        Assert.notNull(clientDetailsService, "client details service is required");
        this.clientDetailsService = clientDetailsService;

        // build a plaintext password encoder, secrets are plaintext in oauth2
        passwordEncoder = PlaintextPasswordEncoder.getInstance();

    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(OAuth2ClientSecretAuthenticationToken.class, authentication,
                "Only ClientSecretAuthenticationToken is supported");

        OAuth2ClientSecretAuthenticationToken authRequest = (OAuth2ClientSecretAuthenticationToken) authentication;
        String clientId = authRequest.getPrincipal();
        String clientSecret = authRequest.getCredentials();
        String authenticationScheme = authRequest.getAuthenticationScheme();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        // load details, we need to check request
        OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

        // check if client can authenticate with this scheme
        if (!client.getAuthenticationScheme().contains(authenticationScheme)) {
            this.logger.debug("Failed to authenticate since client can not use basic scheme");
            throw new BadCredentialsException("invalid authentication");
        }

        /*
         * We authenticate by comparing clientSecret via plainText encoder
         */

        if (!this.passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            this.logger.debug("Failed to authenticate since secret does not match stored value");
            throw new BadCredentialsException("invalid authentication");
        }

        // result contains credentials, someone later on will need to call
        // eraseCredentials
        OAuth2ClientSecretAuthenticationToken result = new OAuth2ClientSecretAuthenticationToken(
                clientId, clientSecret,
                authenticationScheme,
                client.getAuthorities());

        // save details
        result.setDetails(client);
        result.setWebAuthenticationDetails(authRequest.getWebAuthenticationDetails());

        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2ClientSecretAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
