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

package it.smartcommunitylab.aac.openid.apple.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.provider.config.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.openid.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

public class AppleIdentityProviderConfig extends AbstractIdentityProviderConfig<AppleIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + AppleIdentityProviderConfigMap.RESOURCE_TYPE;

    public static final String ISSUER_URI = "https://appleid.apple.com";
    public static final String AUTHORIZATION_URL = "https://appleid.apple.com/auth/authorize?response_mode=form_post";

    public static final String DEFAULT_REDIRECT_URL =
        "{baseUrl}" + AppleIdentityAuthority.AUTHORITY_URL + "{action}/{registrationId}";

    private transient ClientRegistration clientRegistration;
    private transient ECPrivateKey privateKey;

    // thread-safe
    private static final JcaPEMKeyConverter pemConverter = new JcaPEMKeyConverter();

    public AppleIdentityProviderConfig(@JsonProperty("provider") String provider, @JsonProperty("realm") String realm) {
        super(SystemKeys.AUTHORITY_APPLE, provider, realm, new AppleIdentityProviderConfigMap());
        this.clientRegistration = null;
    }

    public AppleIdentityProviderConfig(ConfigurableIdentityProvider cp, AppleIdentityProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable, always isolate providers
        return getProvider();
    }

    public boolean trustEmailAddress() {
        // trust email by default
        return configMap.getTrustEmailAddress() != null ? configMap.getTrustEmailAddress().booleanValue() : true;
    }

    @JsonIgnore
    public ClientRegistration getClientRegistration() {
        if (clientRegistration == null) {
            clientRegistration = toClientRegistration();
        }

        return clientRegistration;
    }

    private ClientRegistration toClientRegistration() {
        // via builder,
        // load well known configuration from apple
        ClientRegistration.Builder builder = ClientRegistrations.fromIssuerLocation(ISSUER_URI);

        /*
         * set config as per
         * https://developer.apple.com/documentation/sign_in_with_apple/
         * generate_and_validate_tokens
         */

        // 1. use post with secret
        builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);

        // 2. set client
        builder.clientId(configMap.getClientId());

        // 3. use auth_code
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

        // 4. add our redirect template
        builder.redirectUri(DEFAULT_REDIRECT_URL);

        // 5. set all scopes available and ask for response POST
        builder.scope(getScopes());

        //        // 5. set key as secret and build JWT at request time
        //        builder.clientSecret(this.getClientJWK().toJSONString());

        // ask for response_mode form to receive scopes
        builder.authorizationUri(AUTHORIZATION_URL);

        // re-set registrationId since auto-configuration sets values provided from
        // issuer
        builder.registrationId(getProvider());

        // use email as name
        builder.userNameAttributeName("email");

        // make sure userinfo is not set to load info from JWT
        builder.userInfoUri(null);

        return builder.build();
    }

    public Set<String> getScopes() {
        Set<String> scopes = new HashSet<>();
        if (Boolean.TRUE.equals(configMap.getAskEmailScope())) {
            scopes.add("email");
        }
        if (Boolean.TRUE.equals(configMap.getAskNameScope())) {
            scopes.add("name");
        }

        return scopes;
    }

    public ECPrivateKey getPrivateKey() {
        if (!StringUtils.hasText(configMap.getPrivateKey())) {
            return null;
        }

        // expect a single PEM EC key (pkcs8)
        if (privateKey == null) {
            privateKey = parsePrivateKey();
        }

        if (privateKey == null) {
            throw new IllegalArgumentException("invalid private key");
        }

        return privateKey;
    }

    private ECPrivateKey parsePrivateKey() {
        String k = configMap.getPrivateKey();
        if (k == null) {
            return null;
        }

        try {
            // read PEM format - requires bouncy castle
            PEMParser parser = new PEMParser(new StringReader(k));
            PrivateKeyInfo privateKeyInfo = null;
            Object pemObj;
            do {
                pemObj = parser.readObject();
                if (pemObj instanceof PrivateKeyInfo) {
                    privateKeyInfo = (PrivateKeyInfo) pemObj;
                }
            } while (pemObj != null);

            if (privateKeyInfo == null) {
                return null;
            }

            PrivateKey privateKey = pemConverter.getPrivateKey(privateKeyInfo);
            if (!(privateKey instanceof ECPrivateKey)) {
                throw new IllegalArgumentException(
                    "invalid key, EC type required, found " + String.valueOf(privateKey.getAlgorithm())
                );
            }

            return (ECPrivateKey) privateKey;
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid key: " + e.getMessage());
        }
    }

    @JsonIgnore
    public OIDCIdentityProviderConfig toOidcProviderConfig() {
        OIDCIdentityProviderConfig op = new OIDCIdentityProviderConfig(
            SystemKeys.AUTHORITY_APPLE,
            getProvider(),
            getRealm()
        );
        OIDCIdentityProviderConfigMap cMap = new OIDCIdentityProviderConfigMap();
        cMap.setClientId(configMap.getClientId());
        cMap.setIssuerUri(ISSUER_URI);
        cMap.setClientAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        cMap.setEnablePkce(false);
        cMap.setScope(String.join(",", getScopes()));
        cMap.setUserNameAttributeName("email");
        op.setConfigMap(cMap);

        return op;
    }
}
