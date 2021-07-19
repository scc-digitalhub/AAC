package it.smartcommunitylab.aac.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.auth.ClientAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;

/*
 * Authentication manager for client
 * 
 * handles authentication process via authentication providers resolving authTokens, and 
 * enhances the result with a complete ClientAuthenticationToken with client details
 */
public class ClientAuthenticationManager implements AuthenticationManager, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClientDetailsService clientService;
    private List<ClientAuthenticationProvider> providers = Collections.emptyList();

    public ClientAuthenticationManager(ClientAuthenticationProvider... providers) {
        this(Arrays.asList(providers));
    }

    public ClientAuthenticationManager(List<ClientAuthenticationProvider> providers) {
        this.providers = providers;
    }

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("process authentication for " + authentication.getName());

        if (!(authentication instanceof ClientAuthentication)) {
            logger.error("invalid authentication class: " + authentication.getClass().getName());
            throw new AuthenticationServiceException("invalid request");
        }

        // fetch first non null response
        ClientAuthentication auth = null;
        for (ClientAuthenticationProvider provider : providers) {
            if (!provider.supports(authentication.getClass())) {
                continue;
            }

            // we let authExceptions propagate
            auth = provider.authenticate(authentication);
            if (auth != null) {
                break;
            }
        }

        if (auth == null) {
            throw new ProviderNotFoundException("provider not found");
        }

        // fetch client details
        String clientId = auth.getClientId();
        try {
            ClientDetails clientDetails = clientService.loadClient(clientId);
            auth.setDetails(clientDetails);

            return auth;
        } catch (NoSuchClientException e) {
            throw new BadCredentialsException("invalid authentication request");
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientService, "client details service is required");
    }

}
