package it.smartcommunitylab.aac.oauth.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.JWKSet;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.ClientService;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientAdditionalConfig;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientInfo;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;

/*
 * Client service for internal usage
 * 
 * Serves the OAuth2 authorization server components
 */

@Service
@Transactional
public class OAuth2ClientService implements ClientService {

    // keep a list of supported grant types, should match tokenGranters
    private static final Set<AuthorizationGrantType> VALID_GRANT_TYPES;
    // keep supported client auth schemes, match clientAuthFilters
    private static final Set<AuthenticationMethod> VALID_AUTH_METHODS;

    static {
        Set<AuthorizationGrantType> n = new HashSet<>();
        n.add(AuthorizationGrantType.AUTHORIZATION_CODE);
        n.add(AuthorizationGrantType.IMPLICIT);
        n.add(AuthorizationGrantType.PASSWORD);
        n.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
        n.add(AuthorizationGrantType.REFRESH_TOKEN);
        VALID_GRANT_TYPES = Collections.unmodifiableSet(n);

        Set<AuthenticationMethod> s = new HashSet<>();
        s.add(AuthenticationMethod.CLIENT_SECRET_BASIC);
        s.add(AuthenticationMethod.CLIENT_SECRET_POST);
        s.add(AuthenticationMethod.NONE);
        VALID_AUTH_METHODS = Collections.unmodifiableSet(s);

    }

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

    @Transactional(readOnly = true)
    public OAuth2Client findClient(String clientId) {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            return null;
        }

        return OAuth2Client.from(client, oauth);

    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Client getClient(String clientId) throws NoSuchClientException {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            throw new NoSuchClientException();
        }

        return OAuth2Client.from(client, oauth);

    }

    @Transactional(readOnly = true)
    public OAuth2Client findClient(String realm, String name) {
        ClientEntity client = clientService.findClient(realm, name);
        if (client == null) {
            return null;
        }

        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(client.getClientId());
        if (oauth == null) {
            return null;
        }

        return OAuth2Client.from(client, oauth);

    }

//    @Override
    @Transactional(readOnly = true)
    public Collection<BaseClient> listClients() {
        List<BaseClient> result = new ArrayList<>();
        List<OAuth2ClientEntity> oauths = oauthClientRepository.findAll();

        for (OAuth2ClientEntity oauth : oauths) {
            ClientEntity client = clientService.findClient(oauth.getClientId());
            if (client != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<OAuth2Client> listClients(String realm) {
        List<OAuth2Client> result = new ArrayList<>();
        Collection<ClientEntity> clients = clientService.listClients(realm, OAuth2Client.CLIENT_TYPE);

        for (ClientEntity client : clients) {
            OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(client.getClientId());
            if (oauth != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ClientSecret getClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        return (oauth.getClientSecret() == null ? null : new ClientSecret(clientId, oauth.getClientSecret()));
    }

    /*
     * Reset client secret, autogenerated
     */
    @Override
    public ClientSecret resetClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        String secret = generateClientSecret();
        oauth.setClientSecret(secret);

        oauth = oauthClientRepository.save(oauth);

        return new ClientSecret(clientId, oauth.getClientSecret());

    }

    /*
     * Set client secret to a given value
     * 
     * to be used internally for bootstrap/import etc
     */
    @Override
    public ClientSecret setClientCredentials(String clientId, ClientCredentials credentials)
            throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        // we expect a string as secret
        if (!(credentials instanceof ClientSecret)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String secret = ((ClientSecret) credentials).getCredentials();

        // TODO validate secret: length, complexity, policies etc
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("invalid secret");
        }

        oauth.setClientSecret(secret);

        oauth = oauthClientRepository.save(oauth);

        return new ClientSecret(clientId, oauth.getClientSecret());

    }

    /*
     * Client management
     */
    public OAuth2Client addClient(String realm, String name) {
        ClientEntity client = clientService.createClient();
        String clientId = client.getClientId();

        return this.addClient(realm, clientId, name);
    }

    public OAuth2Client addClient(String realm, String clientId, String name) {
        return this.addClient(realm, clientId, name,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null, null, null, null);
    }

    public OAuth2Client addClient(
            String realm,
            String name, String description,
            Collection<String> scopes, Collection<String> resourceIds,
            Collection<String> providers,
            Map<String, String> hookFunctions, Map<String, String> hookWebUrls, String hookUniqueSpaces,
            String clientSecret,
            Collection<AuthorizationGrantType> authorizedGrantTypes,
            Collection<String> redirectUris,
            ApplicationType applicationType, TokenType tokenType, SubjectType subjectType,
            Collection<AuthenticationMethod> authenticationMethods,
            Boolean idTokenClaims, Boolean firstParty,
            Integer accessTokenValidity, Integer refreshTokenValidity, Integer idTokenValidity,
            JWKSet jwks, String jwksUri,
            OAuth2ClientAdditionalConfig additionalConfig,
            OAuth2ClientInfo additionalInfo)
            throws IllegalArgumentException {

        // generate a clientId and then add
        ClientEntity client = clientService.createClient();
        String clientId = client.getClientId();

        return addClient(
                realm, clientId,
                name, description,
                scopes, resourceIds,
                providers,
                hookFunctions, hookWebUrls, hookUniqueSpaces,
                clientSecret,
                authorizedGrantTypes,
                redirectUris,
                applicationType, tokenType, subjectType,
                authenticationMethods,
                idTokenClaims, firstParty,
                accessTokenValidity, refreshTokenValidity, idTokenValidity,
                jwks, jwksUri,
                additionalConfig,
                additionalInfo);

    }

    public OAuth2Client addClient(
            String realm, String clientId,
            String name, String description,
            Collection<String> scopes, Collection<String> resourceIds,
            Collection<String> providers,
            Map<String, String> hookFunctions, Map<String, String> hookWebUrls, String hookUniqueSpaces,
            String clientSecret,
            Collection<AuthorizationGrantType> authorizedGrantTypes,
            Collection<String> redirectUris,
            ApplicationType applicationType, TokenType tokenType, SubjectType subjectType,
            Collection<AuthenticationMethod> authenticationMethods,
            Boolean idTokenClaims, Boolean firstParty,
            Integer accessTokenValidity, Integer refreshTokenValidity, Integer idTokenValidity,
            JWKSet jwks, String jwksUri,
            OAuth2ClientAdditionalConfig additionalConfig,
            OAuth2ClientInfo additionalInfo) throws IllegalArgumentException {

        // TODO add custom validator for class
        // manual validation for now
        if (StringUtils.hasText(clientId)) {
            if (clientId.length() < 8 || !Pattern.matches(SystemKeys.SLUG_PATTERN, clientId)) {
                throw new IllegalArgumentException("invalid client id");
            }
        } else {
            // regenerate clientId
            clientId = clientService.createClient().getClientId();
        }

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("realm cannot be empty");
        }

        // TODO validate secret
        if (!StringUtils.hasText(clientSecret)) {
            clientSecret = generateClientSecret();
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream()
                    .anyMatch(gt -> !VALID_GRANT_TYPES.contains(gt))) {
                throw new IllegalArgumentException("Invalid grant type");
            }
        } else {
            authorizedGrantTypes = Collections.emptySet();
        }

        String applicationTypeValue = null;
        if (applicationType != null) {
            if (!ArrayUtils.contains(ApplicationType.values(), applicationType)) {
                throw new IllegalArgumentException("Invalid application type");
            }

            applicationTypeValue = applicationType.getValue();
        } else {
            // default is web
            applicationType = ApplicationType.WEB;
        }

        String tokenTypeValue = null;
        if (tokenType != null) {
            if (!ArrayUtils.contains(TokenType.values(), tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            tokenTypeValue = tokenType.getValue();
        } else {
            // default is null, will use system default
            tokenType = null;
        }

        String subjectTypeValue = null;
        if (subjectType != null) {
            if (!ArrayUtils.contains(SubjectType.values(), subjectType)) {
                throw new IllegalArgumentException("Invalid subject type");
            }

            subjectTypeValue = subjectType.getValue();
        } else {
            // default is public
            // TODO switch to pairwise when supported
            subjectType = SubjectType.PUBLIC;
        }

        if (authenticationMethods != null) {
            // validate
            if (authenticationMethods.stream().anyMatch(a -> !VALID_AUTH_METHODS.contains(a))) {
                throw new IllegalArgumentException("Invalid authentication scheme");
            }
        }

        if (authenticationMethods == null || authenticationMethods.isEmpty()) {
            // enable basic
            authenticationMethods = new HashSet<>();
            authenticationMethods.add(AuthenticationMethod.CLIENT_SECRET_BASIC);

            // if authGrant also enable form for PKCE
            if (authorizedGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE)) {
                authenticationMethods.add(AuthenticationMethod.CLIENT_SECRET_POST);
            }
        }

        boolean copyIdTokenClaims = idTokenClaims != null ? idTokenClaims.booleanValue() : false;
        boolean isFirstParty = firstParty != null ? firstParty.booleanValue() : false;

        String jwksSet = null;
        if (jwks != null) {
            jwksSet = jwks.toString();
        }

        ClientEntity client = clientService.addClient(
                clientId, realm, OAuth2Client.CLIENT_TYPE,
                name, description,
                scopes, resourceIds,
                providers,
                hookFunctions,
                hookWebUrls, hookUniqueSpaces);

        OAuth2ClientEntity oauth = new OAuth2ClientEntity();
        oauth.setClientId(clientId);
        oauth.setClientSecret(clientSecret);
        oauth.setAuthorizedGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizedGrantTypes));
        oauth.setRedirectUris(StringUtils.collectionToCommaDelimitedString(redirectUris));

        oauth.setApplicationType(applicationTypeValue);
        oauth.setTokenType(tokenTypeValue);
        oauth.setSubjectType(subjectTypeValue);

        oauth.setAuthenticationMethods(StringUtils.collectionToCommaDelimitedString(authenticationMethods));
        oauth.setIdTokenClaims(copyIdTokenClaims);
        oauth.setFirstParty(isFirstParty);

        oauth.setIdTokenValidity(idTokenValidity);
        oauth.setAccessTokenValidity(accessTokenValidity);
        oauth.setRefreshTokenValidity(refreshTokenValidity);

        oauth.setJwks(jwksSet);
        oauth.setJwksUri(jwksUri);
        if (additionalConfig != null) {
            oauth.setAdditionalConfiguration(additionalConfig.toMap());
        }
        if (additionalInfo != null) {
            oauth.setAdditionalInformation(additionalInfo.toMap());
        }
        oauth = oauthClientRepository.save(oauth);

        return OAuth2Client.from(client, oauth);
    }

    public OAuth2Client updateClient(
            String clientId,
            String name, String description,
            Collection<String> scopes, Collection<String> resourceIds,
            Collection<String> providers,
            Map<String, String> hookFunctions, Map<String, String> hookWebUrls, String hookUniqueSpaces,
            Collection<AuthorizationGrantType> authorizedGrantTypes,
            Collection<String> redirectUris,
            ApplicationType applicationType, TokenType tokenType, SubjectType subjectType,
            Collection<AuthenticationMethod> authenticationMethods,
            Boolean idTokenClaims, Boolean firstParty,
            Integer accessTokenValidity, Integer refreshTokenValidity, Integer idTokenValidity,
            JWKSet jwks, String jwksUri,
            OAuth2ClientAdditionalConfig additionalConfig,
            OAuth2ClientInfo additionalInfo) throws NoSuchClientException, IllegalArgumentException {

        // TODO add custom validator for class
        // manual validation for now
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("clientId cannot be empty");
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream()
                    .anyMatch(gt -> !VALID_GRANT_TYPES.contains(gt))) {
                throw new IllegalArgumentException("Invalid grant type");
            }
        }

        if (authenticationMethods != null) {
            // validate
            if (authenticationMethods.stream().anyMatch(a -> !VALID_AUTH_METHODS.contains(a))) {
                throw new IllegalArgumentException("Invalid authentication method");
            }
        }

        String applicationTypeValue = null;
        if (applicationType != null) {
            if (!ArrayUtils.contains(ApplicationType.values(), applicationType)) {
                throw new IllegalArgumentException("Invalid application type");
            }

            applicationTypeValue = applicationType.getValue();
        } else {
            // default is web
            applicationType = ApplicationType.WEB;
        }

        String tokenTypeValue = null;
        if (tokenType != null) {
            if (!ArrayUtils.contains(TokenType.values(), tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            tokenTypeValue = tokenType.getValue();
        } else {
            // default is null, will use system default
            tokenType = null;
        }

        String subjectTypeValue = null;
        if (subjectType != null) {
            if (!ArrayUtils.contains(SubjectType.values(), subjectType)) {
                throw new IllegalArgumentException("Invalid subject type");
            }

            subjectTypeValue = subjectType.getValue();
        } else {
            // default is public
            // TODO switch to pairwise when supported
            subjectType = SubjectType.PUBLIC;
        }

        boolean copyIdTokenClaims = idTokenClaims != null ? idTokenClaims.booleanValue() : false;
        boolean isFirstParty = firstParty != null ? firstParty.booleanValue() : false;

        String jwksSet = null;
        if (jwks != null) {
            jwksSet = jwks.toString();
        }

        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            throw new NoSuchClientException();
        }

        client = clientService.updateClient(clientId, name, description, scopes, resourceIds, providers, hookFunctions,
                hookWebUrls, hookUniqueSpaces);

        oauth.setAuthorizedGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizedGrantTypes));
        oauth.setRedirectUris(StringUtils.collectionToCommaDelimitedString(redirectUris));

        oauth.setApplicationType(applicationTypeValue);
        oauth.setTokenType(tokenTypeValue);
        oauth.setSubjectType(subjectTypeValue);

        oauth.setAuthenticationMethods(StringUtils.collectionToCommaDelimitedString(authenticationMethods));
        oauth.setIdTokenClaims(copyIdTokenClaims);
        oauth.setFirstParty(isFirstParty);

        oauth.setIdTokenValidity(idTokenValidity);
        oauth.setAccessTokenValidity(accessTokenValidity);
        oauth.setRefreshTokenValidity(refreshTokenValidity);

        oauth.setJwks(jwksSet);
        oauth.setJwksUri(jwksUri);
        if (additionalConfig != null) {
            oauth.setAdditionalConfiguration(additionalConfig.toMap());
        }
        if (additionalInfo != null) {
            oauth.setAdditionalInformation(additionalInfo.toMap());
        }
        oauth = oauthClientRepository.save(oauth);

        return OAuth2Client.from(client, oauth);
    }

    public void deleteClient(String clientId) {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth != null) {
            oauthClientRepository.delete(oauth);
        }

        if (client != null) {
            clientService.deleteClient(clientId);
        }
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
        return new String(Base64.encodeBase64URLSafe(tokenGenerator.generateKey()), ENCODE_CHARSET);
    }

    private static final BytesKeyGenerator tokenGenerator = KeyGenerators.secureRandom(20);
    private static final Charset ENCODE_CHARSET = Charset.forName("US-ASCII");

}
