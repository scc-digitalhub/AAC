package it.smartcommunitylab.aac.oauth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.crypto.PlaintextPasswordEncoder;
import it.smartcommunitylab.aac.oauth.ClientSecretAuthenticationToken;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientUserDetailsService;

public class OAuth2ClientAuthenticationProvider implements AuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OAuth2ClientUserDetailsService clientUserDetailsService;
    private final DaoAuthenticationProvider authProvider;

    public OAuth2ClientAuthenticationProvider(OAuth2ClientUserDetailsService clientUserDetailsService) {
        Assert.notNull(clientUserDetailsService, "client user details is required");
        this.clientUserDetailsService = clientUserDetailsService;

        // build a plaintext password encoder, secrets are plaintext in oauth2
        PasswordEncoder passwordEncoder = PlaintextPasswordEncoder.getInstance();

        // we leverage DAO authProvider to process requests
        authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(clientUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        // delegate to dao
        return authProvider.authenticate(authentication);

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (ClientSecretAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
