package it.smartcommunitylab.aac.oauth.service;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;

/**
 * Implementation of the {@link UserDetailsService} based on the Client Details
 * model. Note that in order to be used for basicAuth providers will need to
 * employ a plaintext password encoder, since client secrets are stored
 * plaintext
 * 
 * TODO add symmetric key encryption to secrets at rest
 * 
 * @author raman
 *
 */
public class OAuth2ClientUserDetailsService implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OAuth2ClientEntityRepository clientRepository;

    public OAuth2ClientUserDetailsService(OAuth2ClientEntityRepository clientRepository) {
        Assert.notNull(clientRepository, "oauth2 client repository is mandatory");
        this.clientRepository = clientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String clientId) throws UsernameNotFoundException {
        logger.debug("load by client id " + clientId);

        OAuth2ClientEntity client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new UsernameNotFoundException("no such client found");
        }
        // always grant client role
        // TODO load roles from repo
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_CLIENT));

        return new User(client.getClientId(), client.getClientSecret(), authorities);
    }

}
