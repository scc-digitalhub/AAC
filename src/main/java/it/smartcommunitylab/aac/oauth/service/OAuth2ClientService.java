package it.smartcommunitylab.aac.oauth.service;

import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientEntityRepository;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.ClientService;
import it.smartcommunitylab.aac.oauth.Constants;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientInfo;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;

/*
 * Client service for internal usage
 * 
 * Serves the OAuth2 authorization server components
 */

@Service
public class OAuth2ClientService implements ClientService {

    // we use service since we are outside core
    private final ClientEntityService clientService;

    // our client repo
    private final OAuth2ClientEntityRepository oauthClientRepository;

    public OAuth2ClientService(ClientEntityService clientService,
            OAuth2ClientEntityRepository oauthClientRepository) {
        Assert.notNull(clientService, "client service is mandatory");
        Assert.notNull(oauthClientRepository, "oauth client repository is mandatory");
        this.clientService = clientService;
        this.oauthClientRepository = oauthClientRepository;

    }

    @Override
    public OAuth2Client getClient(String clientId) throws NoSuchClientException {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            throw new NoSuchClientException();
        }

        return OAuth2Client.from(client, oauth);

    }

    @Override
    public String getClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        return oauth.getClientSecret();
    }

    @Override
    public String resetClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        String secret = generateClientSecret();
        oauth.setClientSecret(secret);

        oauth = oauthClientRepository.save(oauth);

        return oauth.getClientSecret();

    }

    /*
     * Client management
     */
    public OAuth2Client addClient(String realm, String name) {
        ClientEntity client = clientService.createClient();
        String clientId = client.getClientId();

        return this.addClient(clientId, realm, name,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    public OAuth2Client addClient(
            String clientId, String realm,
            String name, String description,
            Collection<String> scopes,
            Collection<String> providers,
            Collection<String> authorizedGrantTypes,
            Collection<String> redirectUris,
            String tokenType,
            Integer accessTokenValidity, Integer refreshTokenValidity,
            String jwtSignAlgorithm,
            String jwtEncMethod, String jwtEncAlgorithm,
            String jwks, String jwksUri,
            OAuth2ClientInfo additionalInfo) throws IllegalArgumentException {

        // TODO add custom validator for class
        // manual validation for now
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("clientId cannot be empty");
        }

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("realm cannot be empty");
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream().anyMatch(gt -> !ArrayUtils.contains(Constants.GRANT_TYPES, gt))) {
                throw new IllegalArgumentException("Invalid grant type");
            }
        }

        if (StringUtils.hasText(tokenType)) {
            if (!Constants.TOKEN_TYPE_OPAQUE.equals(tokenType) && !Constants.TOKEN_TYPE_JWT.equals(tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }
        } else {
            // default is null, will use system default
            tokenType = null;
        }

        ClientEntity client = clientService.addClient(clientId, realm, SystemKeys.CLIENT_TYPE_OAUTH2, name, description,
                scopes, providers);
        OAuth2ClientEntity oauth = new OAuth2ClientEntity();
        oauth.setClientId(clientId);
        oauth.setClientSecret(generateClientSecret());
        oauth.setAuthorizedGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizedGrantTypes));
        oauth.setRedirectUris(StringUtils.collectionToCommaDelimitedString(redirectUris));
        oauth.setTokenType(tokenType);
        oauth.setAccessTokenValidity(accessTokenValidity);
        oauth.setRefreshTokenValidity(refreshTokenValidity);
        oauth.setJwtSignAlgorithm(jwtSignAlgorithm);
        oauth.setJwtEncMethod(jwtEncMethod);
        oauth.setJwtEncAlgorithm(jwtEncAlgorithm);
        oauth.setJwks(jwks);
        oauth.setJwksUri(jwksUri);
        if (additionalInfo != null) {
            oauth.setAdditionalInformation(additionalInfo.toJson());
        }
        oauth = oauthClientRepository.save(oauth);

        return OAuth2Client.from(client, oauth);
    }

    /*
     * Helpers
     */

    /**
     * Generate new value to be used as client secret (String)
     * 
     * @return
     */
    private synchronized String generateClientSecret() {
        return UUID.randomUUID().toString();
    }

}
