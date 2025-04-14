/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.base.BaseClient;
import it.smartcommunitylab.aac.clients.model.ClientCredentials;
import it.smartcommunitylab.aac.clients.persistence.ClientEntity;
import it.smartcommunitylab.aac.clients.service.ClientEntityService;
import it.smartcommunitylab.aac.clients.service.ClientService;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.jwt.JWKUtils;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientAdditionalConfig;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientInfo;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientJwks;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

    //TODO remove
    private static final String ID_SEPARATOR = "_";

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
        s.add(AuthenticationMethod.CLIENT_SECRET_JWT);
        s.add(AuthenticationMethod.PRIVATE_KEY_JWT);
        s.add(AuthenticationMethod.NONE);
        VALID_AUTH_METHODS = Collections.unmodifiableSet(s);
    }

    // we use service since we are outside core
    private final ClientEntityService clientService;

    // our client repo
    private final OAuth2ClientEntityRepository oauthClientRepository;

    public OAuth2ClientService(ClientEntityService clientService, OAuth2ClientEntityRepository oauthClientRepository) {
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
    public List<OAuth2Client> findClient(String realm, String name) {
        List<OAuth2Client> result = new ArrayList<>();
        Collection<ClientEntity> clients = clientService.findClientsByName(realm, name);

        for (ClientEntity client : clients) {
            OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(client.getClientId());
            if (oauth != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return result;
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
        Collection<ClientEntity> clients = clientService.findClientsByType(realm, OAuth2Client.CLIENT_TYPE);

        for (ClientEntity client : clients) {
            OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(client.getClientId());
            if (oauth != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Page<OAuth2Client> searchClients(String realm, String keywords, Pageable pageRequest) {
        List<OAuth2Client> result = new ArrayList<>();
        Page<ClientEntity> page = clientService.searchClients(realm, keywords, pageRequest);

        // wrong way, totalCount will be off but we have only oauth2 now..
        List<ClientEntity> clients = page
            .getContent()
            .stream()
            .filter(c -> OAuth2Client.CLIENT_TYPE.equals(c.getType()))
            .collect(Collectors.toList());

        for (ClientEntity client : clients) {
            OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(client.getClientId());
            if (oauth != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return PageableExecutionUtils.getPage(result, pageRequest, () -> page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ClientCredentials> getClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);
        ClientEntity client = clientService.findClient(clientId);

        if (oauth == null || client == null) {
            throw new NoSuchClientException();
        }

        Set<ClientCredentials> credentials = new HashSet<>();
        if (StringUtils.hasText(oauth.getClientSecret())) {
            credentials.add(new ClientSecret(client.getRealm(), clientId, oauth.getClientSecret()));
        }
        if (StringUtils.hasText(oauth.getJwks())) {
            credentials.add(new ClientJwks(client.getRealm(), clientId, oauth.getJwks()));
        }

        return credentials;
    }

    @Override
    @Transactional(readOnly = true)
    public ClientCredentials getClientCredentials(String clientId, String credentialsId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);
        ClientEntity client = clientService.findClient(clientId);

        if (oauth == null || client == null) {
            throw new NoSuchClientException();
        }

        // get credentials type from id
        String prefix = clientId + ID_SEPARATOR;
        if (credentialsId == null || !credentialsId.startsWith(prefix)) {
            return null;
        }

        String type = credentialsId.substring(prefix.length());
        if (SystemKeys.RESOURCE_CREDENTIALS_SECRET.equals(type) && StringUtils.hasText(oauth.getClientSecret())) {
            return new ClientSecret(client.getRealm(), clientId, oauth.getClientSecret());
        }
        if (SystemKeys.RESOURCE_CREDENTIALS_JWKS.equals(type) && StringUtils.hasText(oauth.getJwks())) {
            return new ClientJwks(client.getRealm(), clientId, oauth.getJwks());
        }

        return null;
    }

    /*
     * Reset client credentials, autogenerated
     */
    @Override
    public ClientCredentials resetClientCredentials(String clientId, String credentialsId)
        throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);
        ClientEntity client = clientService.findClient(clientId);

        if (oauth == null || client == null) {
            throw new NoSuchClientException();
        }

        // get credentials type from id
        String prefix = clientId + ID_SEPARATOR;
        if (credentialsId == null || !credentialsId.startsWith(prefix)) {
            return null;
        }

        String type = credentialsId.substring(prefix.length());
        if (SystemKeys.RESOURCE_CREDENTIALS_SECRET.equals(type)) {
            String secret = generateClientSecret();

            oauth.setClientSecret(secret);
            oauth = oauthClientRepository.save(oauth);

            return new ClientSecret(client.getRealm(), clientId, oauth.getClientSecret());
        }
        if (SystemKeys.RESOURCE_CREDENTIALS_JWKS.equals(type)) {
            String jwks = generateClientJwks();

            oauth.setJwks(jwks);
            oauth = oauthClientRepository.save(oauth);

            return new ClientJwks(client.getRealm(), clientId, oauth.getJwks());
        }

        return null;
    }

    /*
     * remove credentials
     */
    @Override
    public void removeClientCredentials(String clientId, String credentialsId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);
        ClientEntity client = clientService.findClient(clientId);

        if (oauth == null || client == null) {
            throw new NoSuchClientException();
        }

        // get credentials type from id
        String prefix = clientId + ID_SEPARATOR;
        if (credentialsId == null || !credentialsId.startsWith(prefix)) {
            return;
        }

        String type = credentialsId.substring(prefix.length());
        if (SystemKeys.RESOURCE_CREDENTIALS_SECRET.equals(type) && StringUtils.hasText(oauth.getClientSecret())) {
            oauth.setClientSecret(null);
            oauth = oauthClientRepository.save(oauth);
        }

        if (SystemKeys.RESOURCE_CREDENTIALS_JWKS.equals(type) && StringUtils.hasText(oauth.getJwks())) {
            oauth.setJwks(null);
            oauth = oauthClientRepository.save(oauth);
        }
    }

    /*
     * Set client secret to a given value
     *
     * to be used internally for bootstrap/import etc
     */
    @Override
    public ClientCredentials setClientCredentials(String clientId, String credentialsId, ClientCredentials credentials)
        throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);
        ClientEntity client = clientService.findClient(clientId);

        if (oauth == null || client == null) {
            throw new NoSuchClientException();
        }

        // get credentials type from id
        String prefix = clientId + ID_SEPARATOR;
        if (credentialsId == null || !credentialsId.startsWith(prefix)) {
            return null;
        }

        String type = credentialsId.substring(prefix.length());
        if (SystemKeys.RESOURCE_CREDENTIALS_SECRET.equals(type) && StringUtils.hasText(oauth.getClientSecret())) {
            // we expect a string as secret
            if (!(credentials instanceof ClientSecret)) {
                throw new IllegalArgumentException("invalid credentials");
            }

            String secret = ((ClientSecret) credentials).getClientSecret();

            // TODO validate secret: length, complexity, policies etc
            if (!StringUtils.hasText(secret)) {
                throw new IllegalArgumentException("invalid secret");
            }

            oauth.setClientSecret(secret);
            oauth = oauthClientRepository.save(oauth);

            return new ClientSecret(client.getRealm(), clientId, oauth.getClientSecret());
        }
        if (SystemKeys.RESOURCE_CREDENTIALS_JWKS.equals(type) && StringUtils.hasText(oauth.getClientSecret())) {
            // we expect a jwks as secret
            if (!(credentials instanceof ClientJwks)) {
                throw new IllegalArgumentException("invalid credentials");
            }

            String jwks = ((ClientJwks) credentials).getJwks();

            // validate
            JWKSet set = ((ClientJwks) credentials).getJwkSet();
            if (set == null) {
                throw new IllegalArgumentException("invalid jwks");
            }

            oauth.setJwks(jwks);
            oauth = oauthClientRepository.save(oauth);

            return new ClientJwks(client.getRealm(), clientId, oauth.getJwks());
        }

        return null;
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
        return this.addClient(
                realm,
                clientId,
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
    }

    public OAuth2Client addClient(
        String realm,
        String name,
        String description,
        Collection<String> scopes,
        Collection<String> resourceIds,
        Collection<String> providers,
        Map<String, String> hookFunctions,
        Map<String, String> hookWebUrls,
        String hookUniqueSpaces,
        String clientSecret,
        Collection<AuthorizationGrantType> authorizedGrantTypes,
        Collection<String> redirectUris,
        ApplicationType applicationType,
        TokenType tokenType,
        SubjectType subjectType,
        Collection<AuthenticationMethod> authenticationMethods,
        Boolean idTokenClaims,
        Boolean firstParty,
        Integer accessTokenValidity,
        Integer refreshTokenValidity,
        Integer idTokenValidity,
        JWKSet jwks,
        String jwksUri,
        OAuth2ClientAdditionalConfig additionalConfig,
        OAuth2ClientInfo additionalInfo
    ) throws IllegalArgumentException {
        // generate a clientId and then add
        ClientEntity client = clientService.createClient();
        String clientId = client.getClientId();

        return addClient(
            realm,
            clientId,
            name,
            description,
            scopes,
            resourceIds,
            providers,
            hookFunctions,
            hookWebUrls,
            hookUniqueSpaces,
            clientSecret,
            authorizedGrantTypes,
            redirectUris,
            applicationType,
            tokenType,
            subjectType,
            authenticationMethods,
            idTokenClaims,
            firstParty,
            accessTokenValidity,
            refreshTokenValidity,
            idTokenValidity,
            jwks,
            jwksUri,
            additionalConfig,
            additionalInfo
        );
    }

    public OAuth2Client addClient(
        String realm,
        String clientId,
        String name,
        String description,
        Collection<String> scopes,
        Collection<String> resourceIds,
        Collection<String> providers,
        Map<String, String> hookFunctions,
        Map<String, String> hookWebUrls,
        String hookUniqueSpaces,
        String clientSecret,
        Collection<AuthorizationGrantType> authorizedGrantTypes,
        Collection<String> redirectUris,
        ApplicationType applicationType,
        TokenType tokenType,
        SubjectType subjectType,
        Collection<AuthenticationMethod> authenticationMethods,
        Boolean idTokenClaims,
        Boolean firstParty,
        Integer accessTokenValidity,
        Integer refreshTokenValidity,
        Integer idTokenValidity,
        JWKSet jwks,
        String jwksUri,
        OAuth2ClientAdditionalConfig additionalConfig,
        OAuth2ClientInfo additionalInfo
    ) throws IllegalArgumentException {
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
            //            clientSecret = generateClientSecret();
            clientSecret = null;
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream().anyMatch(gt -> !VALID_GRANT_TYPES.contains(gt))) {
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
            // default is spa
            applicationTypeValue = ApplicationType.SPA.getValue();
        }

        // validate types
        if (ApplicationType.INTROSPECTION.getValue().equals(applicationTypeValue)) {
            if (authorizedGrantTypes != null && !authorizedGrantTypes.isEmpty()) {
                throw new IllegalArgumentException("Invalid grant type");
            }
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
            subjectTypeValue = SubjectType.PUBLIC.getValue();
        }

        if (authenticationMethods != null) {
            // validate
            if (authenticationMethods.stream().anyMatch(a -> !VALID_AUTH_METHODS.contains(a))) {
                throw new IllegalArgumentException("Invalid authentication scheme");
            }
        }

        if (authenticationMethods == null || authenticationMethods.isEmpty()) {
            // enable none
            authenticationMethods = new HashSet<>();
            authenticationMethods.add(AuthenticationMethod.NONE);

            // if authGrant also enable form for PKCE
            if (authorizedGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE)) {
                authenticationMethods.add(AuthenticationMethod.CLIENT_SECRET_POST);
            }
        }

        boolean copyIdTokenClaims = idTokenClaims != null ? idTokenClaims.booleanValue() : false;
        boolean isFirstParty = firstParty != null ? firstParty.booleanValue() : false;

        String jwksSet = null;
        if (jwks != null) {
            jwksSet = jwks.toString(false);
        }

        ClientEntity client = clientService.addClient(
            clientId,
            realm,
            OAuth2Client.CLIENT_TYPE,
            name,
            description,
            scopes,
            resourceIds,
            providers,
            hookFunctions,
            hookWebUrls,
            hookUniqueSpaces
        );

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
        String name,
        String description,
        String notes,
        Collection<String> scopes,
        Collection<String> resourceIds,
        Collection<String> providers,
        Map<String, String> hookFunctions,
        Map<String, String> hookWebUrls,
        String hookUniqueSpaces,
        Collection<AuthorizationGrantType> authorizedGrantTypes,
        Collection<String> redirectUris,
        ApplicationType applicationType,
        TokenType tokenType,
        SubjectType subjectType,
        Collection<AuthenticationMethod> authenticationMethods,
        Boolean idTokenClaims,
        Boolean firstParty,
        Integer accessTokenValidity,
        Integer refreshTokenValidity,
        Integer idTokenValidity,
        String jwksUri,
        OAuth2ClientAdditionalConfig additionalConfig,
        OAuth2ClientInfo additionalInfo
    ) throws NoSuchClientException, IllegalArgumentException {
        // TODO add custom validator for class
        // manual validation for now
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("clientId cannot be empty");
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream().anyMatch(gt -> !VALID_GRANT_TYPES.contains(gt))) {
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
            applicationTypeValue = ApplicationType.WEB.getValue();
        }

        // validate types
        if (ApplicationType.INTROSPECTION.getValue().equals(applicationTypeValue)) {
            if (authorizedGrantTypes != null && !authorizedGrantTypes.isEmpty()) {
                throw new IllegalArgumentException("Invalid grant type");
            }
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
            subjectTypeValue = SubjectType.PUBLIC.getValue();
        }

        boolean copyIdTokenClaims = idTokenClaims != null ? idTokenClaims.booleanValue() : false;
        boolean isFirstParty = firstParty != null ? firstParty.booleanValue() : false;

        //        String jwksSet = null;
        //        if (jwks != null) {
        //            jwksSet = jwks.toString(false);
        //        }

        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            throw new NoSuchClientException();
        }

        client =
            clientService.updateClient(
                clientId,
                name,
                description,
                notes,
                scopes,
                resourceIds,
                providers,
                hookFunctions,
                hookWebUrls,
                hookUniqueSpaces
            );

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

        //        oauth.setJwks(jwksSet);
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
        return new String(Base64.getUrlEncoder().encode(tokenGenerator.generateKey()), ENCODE_CHARSET);
    }

    private synchronized String generateClientJwks() {
        try {
            // build a default RSA2048 key for sign
            JWK jwk = JWKUtils.generateRsaJWK(UUID.randomUUID().toString(), "sig", "RS256", 2048);
            JWKSet jwks = new JWKSet(jwk);
            return jwks.toString(false);
        } catch (IllegalArgumentException | JOSEException e) {
            // ignore, will return an empty key
            return null;
        }
    }

    private static final BytesKeyGenerator tokenGenerator = KeyGenerators.secureRandom(32);
    private static final Charset ENCODE_CHARSET = Charset.forName("UTF-8");
}
