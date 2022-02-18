package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.auth.ClientAuthenticationProvider;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import it.smartcommunitylab.aac.crypto.PlaintextPasswordEncoder;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

public class OAuth2ClientSecretAuthenticationProvider extends ClientAuthenticationProvider implements InitializingBean {
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
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientService, "client service is required");
    }

    @Override
    public ClientAuthentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(OAuth2ClientSecretAuthenticationToken.class, authentication,
                "Only ClientSecretAuthenticationToken is supported");

        OAuth2ClientSecretAuthenticationToken authRequest = (OAuth2ClientSecretAuthenticationToken) authentication;
        String clientId = authRequest.getPrincipal();
        String clientSecret = authRequest.getCredentials();
        String authenticationMethod = authRequest.getAuthenticationMethod();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            // load details, we need to check request
            OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

            // check if client can authenticate with this scheme
            if (!client.getAuthenticationMethods().contains(authenticationMethod)) {
                this.logger.debug("Failed to authenticate since client can not use scheme " + authenticationMethod);
                throw new BadCredentialsException("invalid authentication");
            }

            /*
             * We authenticate by comparing clientSecret via plainText encoder
             */

            if (!this.passwordEncoder.matches(clientSecret, client.getClientSecret())) {
                this.logger.debug("Failed to authenticate since secret does not match stored value");
                throw new BadCredentialsException("invalid authentication");
            }

            // load authorities from clientService
            Collection<GrantedAuthority> authorities;
            try {
                ClientDetails clientDetails = clientService.loadClient(clientId);
                authorities = clientDetails.getAuthorities();
            } catch (NoSuchClientException e) {
                throw new ClientRegistrationException("invalid client");
            }

            // result contains credentials, someone later on will need to call
            // eraseCredentials
            OAuth2ClientSecretAuthenticationToken result = new OAuth2ClientSecretAuthenticationToken(
                    clientId, clientSecret,
                    authenticationMethod,
                    authorities);

            // save details
            // TODO add ClientDetails in addition to oauth2ClientDetails
            result.setOAuth2ClientDetails(client);
            result.setWebAuthenticationDetails(authRequest.getWebAuthenticationDetails());

            return result;
        } catch (ClientRegistrationException e) {
            throw new BadCredentialsException("invalid authentication");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2ClientSecretAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
