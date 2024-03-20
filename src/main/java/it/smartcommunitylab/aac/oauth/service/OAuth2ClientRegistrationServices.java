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

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientAdditionalConfig;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientConfigMap;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientInfo;
import it.smartcommunitylab.aac.oauth.common.HumanStringKeyGenerator;
import it.smartcommunitylab.aac.oauth.model.AcrValues;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;
import it.smartcommunitylab.aac.oauth.model.ResponseType;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.oauth.provider.ClientRegistrationServices;
import it.smartcommunitylab.aac.oauth.request.ClientRegistrationRequest;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientRegistrationServices implements ClientRegistrationServices, InitializingBean {

    private static final StringKeyGenerator NAME_GENERATOR = new HumanStringKeyGenerator(4);
    private static final StringKeyGenerator SECRET_GENERATOR = new HumanStringKeyGenerator(32);

    private final OAuth2ClientService clientService;
    private IdentityProviderService providerService;
    private StringKeyGenerator nameGenerator;
    private StringKeyGenerator secretGenerator;

    public OAuth2ClientRegistrationServices(OAuth2ClientService clientService) {
        Assert.notNull(clientService, "client service is mandatory");
        this.clientService = clientService;
        this.nameGenerator = NAME_GENERATOR;
        this.secretGenerator = SECRET_GENERATOR;
    }

    public void setProviderService(IdentityProviderService providerService) {
        this.providerService = providerService;
    }

    public void setNameGenerator(StringKeyGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }

    public void setSecretGenerator(StringKeyGenerator secretGenerator) {
        this.secretGenerator = secretGenerator;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(providerService, "provider service is required");
        Assert.notNull(nameGenerator, "name generator is required");
        Assert.notNull(secretGenerator, "secret generator is required");
    }

    @Override
    public ClientRegistration loadRegistrationByClientId(String clientId) throws ClientRegistrationException {
        try {
            OAuth2Client client = clientService.getClient(clientId);
            return toRegistration(client);
        } catch (NoSuchClientException e) {
            throw new ClientRegistrationException("No client with requested id: " + clientId);
        }
    }

    @Override
    public ClientRegistration addRegistration(String realm, ClientRegistrationRequest request)
        throws ClientRegistrationException {
        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("missing or invalid realm");
        }

        ClientRegistration registration = request.getRegistration();
        String name = registration.getName();
        String description = registration.getName();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        } else {
            // generate random
            name = nameGenerator.generateKey();
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        // enable all providers for the given realm
        // we lack a way to transmit providers via client registration
        Set<String> providers = providerService
            .listProviders(realm)
            .stream()
            .map(p -> p.getProvider())
            .collect(Collectors.toSet());

        // build a client config
        OAuth2ClientConfigMap configMap = toConfigMap(registration);

        // TODO read software_statement and update config
        //

        // create credentials when required
        String clientSecret = null;
        if (
            registration.getAuthenticationMethods().contains(AuthenticationMethod.CLIENT_SECRET_BASIC.getValue()) ||
            registration.getAuthenticationMethods().contains(AuthenticationMethod.CLIENT_SECRET_POST.getValue()) ||
            registration.getAuthenticationMethods().contains(AuthenticationMethod.CLIENT_SECRET_JWT.getValue())
        ) {
            clientSecret = secretGenerator.generateKey();
        }

        // register with autogenerated clientId
        // add as new
        OAuth2Client client = clientService.addClient(
            realm,
            null,
            name,
            description,
            registration.getScope(),
            registration.getResourceIds(),
            providers,
            null,
            null,
            null,
            clientSecret,
            configMap.getAuthorizedGrantTypes(),
            configMap.getRedirectUris(),
            configMap.getApplicationType(),
            configMap.getTokenType(),
            configMap.getSubjectType(),
            configMap.getAuthenticationMethods(),
            configMap.getIdTokenClaims(),
            configMap.getFirstParty(),
            configMap.getAccessTokenValidity(),
            configMap.getRefreshTokenValidity(),
            configMap.getIdTokenValidity(),
            null,
            configMap.getJwksUri(),
            configMap.getAdditionalConfig(),
            configMap.getAdditionalInformation()
        );

        if (client == null) {
            throw new ClientRegistrationException("registration error");
        }

        ClientRegistration result = toRegistration(client);
        // set issuedAt to now
        result.setClientIdIssuedAt(Instant.now().getEpochSecond());
        // client secret does not expire if set
        if (result.getClientSecret() != null) {
            result.setClientSecretExpiresAt(0);
        }

        return result;
    }

    @Override
    public ClientRegistration updateRegistration(String clientId, ClientRegistrationRequest request)
        throws ClientRegistrationException {
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("missing or invalid clientId");
        }

        if (!clientId.equals(request.getRegistration().getClientId())) {
            throw new IllegalArgumentException("missing or invalid clientId");
        }

        // fetch client and then replace config
        try {
            OAuth2Client client = clientService.getClient(clientId);
            String realm = client.getRealm();

            ClientRegistration registration = request.getRegistration();
            String name = registration.getName();
            String description = registration.getName();

            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            } else {
                // keep existing or generate
                name = StringUtils.hasText(client.getName()) ? client.getName() : nameGenerator.generateKey();
            }

            if (StringUtils.hasText(description)) {
                description = Jsoup.clean(description, Safelist.none());
            }

            // enable all providers for the given realm
            // we lack a way to transmit providers via client registration
            Set<String> providers = providerService
                .listProviders(realm)
                .stream()
                .map(p -> p.getProvider())
                .collect(Collectors.toSet());

            // build a client config
            OAuth2ClientConfigMap configMap = toConfigMap(registration);

            // TODO read software_statement and update config
            //
            // update selectively
            client =
                clientService.updateClient(
                    clientId,
                    name,
                    description,
                    registration.getScope(),
                    registration.getResourceIds(),
                    providers,
                    client.getHookFunctions(),
                    client.getHookWebUrls(),
                    client.getHookUniqueSpaces(),
                    configMap.getAuthorizedGrantTypes(),
                    configMap.getRedirectUris(),
                    configMap.getApplicationType(),
                    configMap.getTokenType(),
                    configMap.getSubjectType(),
                    configMap.getAuthenticationMethods(),
                    configMap.getIdTokenClaims(),
                    configMap.getFirstParty(),
                    configMap.getAccessTokenValidity(),
                    configMap.getRefreshTokenValidity(),
                    configMap.getIdTokenValidity(),
                    configMap.getJwksUri(),
                    configMap.getAdditionalConfig(),
                    configMap.getAdditionalInformation()
                );

            ClientRegistration result = toRegistration(client);

            // client secret does not expire if set
            if (result.getClientSecret() != null) {
                result.setClientSecretExpiresAt(0);
            }

            return result;
        } catch (NoSuchClientException e) {
            throw new ClientRegistrationException("No client with requested id: " + clientId);
        }
    }

    @Override
    public void removeRegistration(String clientId) {
        clientService.deleteClient(clientId);
    }

    private OAuth2ClientConfigMap toConfigMap(ClientRegistration reg) {
        OAuth2ClientConfigMap configMap = new OAuth2ClientConfigMap();

        if (reg.getGrantType() != null) {
            Set<AuthorizationGrantType> grantTypes = reg
                .getGrantType()
                .stream()
                .map(t -> AuthorizationGrantType.parse(t))
                .filter(t -> t != null)
                .collect(Collectors.toSet());
            configMap.setAuthorizedGrantTypes(grantTypes);
        }

        if (reg.getRedirectUris() != null) {
            configMap.setRedirectUris(reg.getRedirectUris());
        }

        if (reg.getAuthenticationMethods() != null) {
            Set<AuthenticationMethod> authMethods = reg
                .getAuthenticationMethods()
                .stream()
                .map(t -> AuthenticationMethod.parse(t))
                .filter(t -> t != null)
                .collect(Collectors.toSet());
            configMap.setAuthenticationMethods(authMethods);
        }

        if (reg.getApplicationType() != null) {
            ApplicationType type = ApplicationType.parse(reg.getApplicationType());
            if (type != null) {
                configMap.setApplicationType(type);
            }
        }

        if (reg.getSubjectType() != null) {
            SubjectType type = SubjectType.parse(reg.getSubjectType());
            if (type != null) {
                configMap.setSubjectType(type);
            }
        }

        if (reg.getIdTokenValiditySeconds() != null) {
            configMap.setIdTokenValidity(reg.getIdTokenValiditySeconds());
        }
        if (reg.getAccessTokenValiditySeconds() != null) {
            configMap.setAccessTokenValidity(reg.getAccessTokenValiditySeconds());
        }
        if (reg.getRefreshTokenValiditySeconds() != null) {
            configMap.setRefreshTokenValidity(reg.getRefreshTokenValiditySeconds());
        }

        // TODO enable when configMap includes jwks (client refactor)
        //        if (reg.getJwks() != null) {
        //
        //            try {
        //                JWKSet jwks = JWKSet.parse(reg.getJwks());
        //                configMap.setJwks(jwks.toString(false));
        //            } catch (ParseException e) {
        //                // ignore invalid jwks
        //            }
        //        }

        if (reg.getJwksUri() != null) {
            configMap.setJwksUri(reg.getJwksUri());
        }

        OAuth2ClientAdditionalConfig additional = new OAuth2ClientAdditionalConfig();
        OAuth2ClientInfo info = new OAuth2ClientInfo();

        if (reg.getResponseTypes() != null) {
            Set<ResponseType> responseTypes = reg
                .getResponseTypes()
                .stream()
                .map(t -> ResponseType.parse(t))
                .filter(t -> t != null)
                .collect(Collectors.toSet());
            additional.setResponseTypes(responseTypes);
        }

        if (reg.getAcrValues() != null) {
            Set<AcrValues> acrValues = reg
                .getAcrValues()
                .stream()
                .map(t -> AcrValues.parse(t))
                .filter(t -> t != null)
                .collect(Collectors.toSet());
            additional.setAcrValues(acrValues);
        }

        // jwt
        if (reg.getJwtSignAlgorithm() != null) {
            additional.setJwtSignAlgorithm(JWSAlgorithm.parse(reg.getJwtSignAlgorithm()));
        }
        if (reg.getJwtEncAlgorithm() != null) {
            additional.setJwtEncAlgorithm(JWEAlgorithm.parse(reg.getJwtEncAlgorithm()));
        }
        if (reg.getJwtEncMethod() != null) {
            additional.setJwtEncMethod(EncryptionMethod.parse(reg.getJwtEncMethod()));
        }

        // idToken
        if (reg.getIdTokenSignAlgorithm() != null) {
            additional.setIdTokenSignAlgorithm(JWSAlgorithm.parse(reg.getIdTokenSignAlgorithm()));
        }
        if (reg.getIdTokenEncAlgorithm() != null) {
            additional.setIdTokenEncAlgorithm(JWEAlgorithm.parse(reg.getIdTokenEncAlgorithm()));
        }
        if (reg.getIdTokenEncMethod() != null) {
            additional.setIdTokenEncMethod(EncryptionMethod.parse(reg.getIdTokenEncMethod()));
        }

        // userinfo
        if (reg.getUserinfoSignAlgorithm() != null) {
            additional.setUserinfoSignAlgorithm(JWSAlgorithm.parse(reg.getUserinfoSignAlgorithm()));
        }
        if (reg.getUserinfoEncAlgorithm() != null) {
            additional.setUserinfoEncAlgorithm(JWEAlgorithm.parse(reg.getUserinfoEncAlgorithm()));
        }
        if (reg.getUserinfoEncMethod() != null) {
            additional.setUserinfoEncMethod(EncryptionMethod.parse(reg.getUserinfoEncMethod()));
        }

        // requestObj
        if (reg.getRequestobjSignAlgorithm() != null) {
            additional.setRequestobjSignAlgorithm(JWSAlgorithm.parse(reg.getRequestobjSignAlgorithm()));
        }
        if (reg.getRequestobjEncAlgorithm() != null) {
            additional.setRequestobjEncAlgorithm(JWEAlgorithm.parse(reg.getRequestobjEncAlgorithm()));
        }
        if (reg.getRequestobjEncMethod() != null) {
            additional.setRequestobjEncMethod(EncryptionMethod.parse(reg.getRequestobjEncMethod()));
        }

        configMap.setAdditionalConfig(additional);
        configMap.setAdditionalInformation(info);

        return configMap;
    }

    private ClientRegistration toRegistration(OAuth2Client client) {
        ClientRegistration reg = new ClientRegistration();
        reg.setName(client.getName());
        reg.setClientId(client.getClientId());
        reg.setClientSecret(client.getClientSecret() != null ? client.getClientSecret().getClientSecret() : null);
        reg.setScope(client.getScopes());
        reg.setResourceIds(client.getResourceIds());

        OAuth2ClientConfigMap configMap = client.getConfigMap();

        reg.setGrantType(
            configMap.getAuthorizedGrantTypes().stream().map(gt -> gt.getValue()).collect(Collectors.toSet())
        );
        reg.setRedirectUris(configMap.getRedirectUris());
        reg.setAuthenticationMethods(
            configMap.getAuthenticationMethods().stream().map(gt -> gt.getValue()).collect(Collectors.toSet())
        );

        reg.setApplicationType(
            configMap.getApplicationType() != null ? configMap.getApplicationType().getValue() : null
        );
        reg.setSubjectType(configMap.getSubjectType() != null ? configMap.getSubjectType().getValue() : null);

        reg.setIdTokenValiditySeconds(configMap.getIdTokenValidity());
        reg.setAccessTokenValiditySeconds(configMap.getAccessTokenValidity());
        reg.setRefreshTokenValiditySeconds(configMap.getRefreshTokenValidity());

        // JWT config
        reg.setJwks(client.getJwks() != null ? client.getJwks() : null);
        reg.setJwksUri(configMap.getJwksUri());

        if (configMap.getAdditionalConfig() != null) {
            OAuth2ClientAdditionalConfig config = configMap.getAdditionalConfig();

            if (config.getResponseTypes() != null) {
                reg.setResponseTypes(
                    config.getResponseTypes().stream().map(t -> t.getValue()).collect(Collectors.toSet())
                );
            }

            if (config.getAcrValues() != null) {
                reg.setAcrValues(config.getAcrValues().stream().map(t -> t.getValue()).collect(Collectors.toSet()));
            }

            // TODO handle all response config as per openid DCR
            // jwt
            if (config.getJwtSignAlgorithm() != null) {
                reg.setJwtSignAlgorithm(config.getJwtSignAlgorithm().getValue());
            }
            if (config.getJwtEncAlgorithm() != null) {
                reg.setJwtEncAlgorithm(config.getJwtEncAlgorithm().getValue());
            }
            if (config.getJwtEncMethod() != null) {
                reg.setJwtEncMethod(config.getJwtEncMethod().getValue());
            }

            // idToken
            if (config.getIdTokenSignAlgorithm() != null) {
                reg.setIdTokenSignAlgorithm(config.getIdTokenSignAlgorithm().getValue());
            }
            if (config.getIdTokenEncAlgorithm() != null) {
                reg.setIdTokenEncAlgorithm(config.getIdTokenEncAlgorithm().getValue());
            }
            if (config.getIdTokenEncMethod() != null) {
                reg.setIdTokenEncMethod(config.getIdTokenEncMethod().getValue());
            }

            // userinfo
            if (config.getUserinfoSignAlgorithm() != null) {
                reg.setUserinfoSignAlgorithm(config.getUserinfoSignAlgorithm().getValue());
            }
            if (config.getUserinfoEncAlgorithm() != null) {
                reg.setUserinfoEncAlgorithm(config.getUserinfoEncAlgorithm().getValue());
            }
            if (config.getUserinfoEncMethod() != null) {
                reg.setUserinfoEncMethod(config.getUserinfoEncMethod().getValue());
            }

            // requestObj
            if (config.getRequestobjSignAlgorithm() != null) {
                reg.setRequestobjSignAlgorithm(config.getRequestobjSignAlgorithm().getValue());
            }
            if (config.getRequestobjEncAlgorithm() != null) {
                reg.setRequestobjEncAlgorithm(config.getRequestobjEncAlgorithm().getValue());
            }
            if (config.getRequestobjEncMethod() != null) {
                reg.setRequestobjEncMethod(config.getRequestobjEncMethod().getValue());
            }
            // ignore additional config
        }

        // map additional info
        if (configMap.getAdditionalInformation() != null) {
            OAuth2ClientInfo info = configMap.getAdditionalInformation();
        }

        return reg;
    }
}
