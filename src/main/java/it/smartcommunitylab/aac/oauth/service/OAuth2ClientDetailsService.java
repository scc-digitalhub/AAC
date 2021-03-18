package it.smartcommunitylab.aac.oauth.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;

public class OAuth2ClientDetailsService implements ClientDetailsService {

    // TODO evaluate direct repo access VS service
    // we lose validation but reduce complexity
//    private final OAuth2ClientService clientService;
    private final OAuth2ClientEntityRepository clientRepository;

    // we need access to client roles, we use service since we are outside core
    private final ClientEntityService clientService;

    public OAuth2ClientDetailsService(ClientEntityService clientService,
            OAuth2ClientEntityRepository clientRepository) {
        Assert.notNull(clientService, "client service is mandatory");
        Assert.notNull(clientRepository, "oauth client repository is mandatory");
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = clientRepository.findByClientId(clientId);
        if (client == null || oauth == null) {
            throw new NoSuchClientException("No client with requested id: " + clientId);
        }

        // build details
        OAuth2ClientDetails clientDetails = new OAuth2ClientDetails();
        clientDetails.setRealm(client.getRealm());
        clientDetails.setClientId(clientId);
        clientDetails.setClientSecret(oauth.getClientSecret());
        clientDetails.setScope(StringUtils.commaDelimitedListToSet(client.getScopes()));

        clientDetails.setAuthorizedGrantTypes(StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes()));
        clientDetails.setRegisteredRedirectUri(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));

        try {
            List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId);
            Set<GrantedAuthority> authorities = clientRoles.stream()
                    .map(r -> new RealmGrantedAuthority(r.getRealm(), r.getRole()))
                    .collect(Collectors.toSet());

            clientDetails.setAuthorities(authorities);

        } catch (it.smartcommunitylab.aac.common.NoSuchClientException e) {
            throw new NoSuchClientException("No client with requested id: " + clientId);
        }
        return clientDetails;
    }

}
