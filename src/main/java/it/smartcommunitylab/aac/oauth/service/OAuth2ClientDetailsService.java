package it.smartcommunitylab.aac.oauth.service;

import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientAdditionalConfig;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class OAuth2ClientDetailsService implements ClientDetailsService {

    // TODO evaluate direct repo access VS service
    // we lose validation but reduce complexity
    //    private final OAuth2ClientService clientService;
    private final OAuth2ClientEntityRepository clientRepository;

    // we need access to client roles, we use service since we are outside core
    private final ClientEntityService clientService;

    public OAuth2ClientDetailsService(
        ClientEntityService clientService,
        OAuth2ClientEntityRepository clientRepository
    ) {
        Assert.notNull(clientService, "client service is mandatory");
        Assert.notNull(clientRepository, "oauth client repository is mandatory");
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    // TODO add a local cache, client definitions don't change frequently
    // even short window (30s) could cover a whole request
    @Override
    @Transactional(readOnly = true)
    public OAuth2ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = clientRepository.findByClientId(clientId);
        if (client == null || oauth == null) {
            throw new NoSuchClientException("No client with requested id: " + clientId);
        }

        // build details
        OAuth2ClientDetails clientDetails = new OAuth2ClientDetails();
        clientDetails.setRealm(client.getRealm());
        clientDetails.setName(StringUtils.hasText(client.getName()) ? client.getName() : clientId);
        clientDetails.setClientId(clientId);
        clientDetails.setClientSecret(oauth.getClientSecret());
        clientDetails.setScope(StringUtils.commaDelimitedListToSet(client.getScopes()));
        clientDetails.setResourceIds(StringUtils.commaDelimitedListToSet(client.getResourceIds()));

        clientDetails.setAuthorizedGrantTypes(StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes()));
        clientDetails.setRegisteredRedirectUris(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));
        clientDetails.setAuthenticationMethods(StringUtils.commaDelimitedListToSet(oauth.getAuthenticationMethods()));

        clientDetails.setIdTokenClaims(oauth.isIdTokenClaims());
        clientDetails.setFirstParty(oauth.isFirstParty());
        //        clientDetails.setAutoApproveScopes(StringUtils.commaDelimitedListToSet(oauth.getAutoApproveScopes()));

        // token settings
        clientDetails.setApplicationType(oauth.getApplicationType());
        clientDetails.setTokenType(oauth.getTokenType());
        clientDetails.setSubjectType(oauth.getSubjectType());

        clientDetails.setIdTokenValiditySeconds(oauth.getIdTokenValidity());
        clientDetails.setAccessTokenValiditySeconds(oauth.getAccessTokenValidity());
        clientDetails.setRefreshTokenValiditySeconds(oauth.getRefreshTokenValidity());

        // JWT config
        clientDetails.setJwks(oauth.getJwks());
        clientDetails.setJwksUri(oauth.getJwksUri());

        if (oauth.getAdditionalConfiguration() != null) {
            try {
                OAuth2ClientAdditionalConfig config = OAuth2ClientAdditionalConfig.convert(
                    oauth.getAdditionalConfiguration()
                );

                if (config.getResponseTypes() != null) {
                    clientDetails.setResponseTypes(
                        config.getResponseTypes().stream().map(t -> t.getValue()).collect(Collectors.toSet())
                    );
                }

                // TODO handle all response config as per openid DCR
                if (config.getJwtSignAlgorithm() != null) {
                    clientDetails.setJwtSignAlgorithm(config.getJwtSignAlgorithm().getValue());
                }
                if (config.getJwtEncAlgorithm() != null) {
                    clientDetails.setJwtEncMethod(config.getJwtEncAlgorithm().getValue());
                }
                if (config.getJwtEncMethod() != null) {
                    clientDetails.setJwtEncAlgorithm(config.getJwtEncMethod().getValue());
                }

                boolean isRefreshTokenRotation = config.getRefreshTokenRotation() != null
                    ? config.getRefreshTokenRotation().booleanValue()
                    : false;
                clientDetails.setRefreshTokenRotation(isRefreshTokenRotation);
            } catch (Exception e) {
                // ignore additional config
            }
        }

        // map additional info
        if (oauth.getAdditionalInformation() != null) {
            Map<String, Object> additionalInfo = new HashMap<>(oauth.getAdditionalInformation());
            clientDetails.setAdditionalInformation(additionalInfo);
        }

        // map hooks
        clientDetails.setHookFunctions(client.getHookFunctions());
        clientDetails.setHookWebUrls(client.getHookWebUrls());
        clientDetails.setHookUniqueSpaces(client.getHookUniqueSpaces());

        //        // always grant role_client
        //        Set<GrantedAuthority> authorities = new HashSet<>();
        //        authorities.add(new SimpleGrantedAuthority(Config.R_CLIENT));
        //        try {
        //            List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId);
        //
        //            authorities.addAll(clientRoles.stream()
        //                    .filter(r -> !StringUtils.hasText(r.getRealm()))
        //                    .map(r -> new SimpleGrantedAuthority(r.getRole()))
        //                    .collect(Collectors.toSet()));
        //
        //            authorities.addAll(clientRoles.stream()
        //                    .filter(r -> StringUtils.hasText(r.getRealm()))
        //                    .map(r -> new RealmGrantedAuthority(r.getRealm(), r.getRole()))
        //                    .collect(Collectors.toSet()));
        //
        //        } catch (it.smartcommunitylab.aac.common.NoSuchClientException e) {
        ////            throw new NoSuchClientException("No client with requested id: " + clientId);
        //        }

        //        clientDetails.setAuthorities(Collections.unmodifiableCollection(authorities));

        return clientDetails;
    }
}
