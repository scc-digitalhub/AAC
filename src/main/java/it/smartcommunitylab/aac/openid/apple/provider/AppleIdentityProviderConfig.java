package it.smartcommunitylab.aac.openid.apple.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.openid.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

public class AppleIdentityProviderConfig extends AbstractIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public static final String ISSUER_URI = "https://appleid.apple.com";

    private static final String[] SCOPES = { "name", "email" };

    public static final String DEFAULT_REDIRECT_URL = "{baseUrl}" + AppleIdentityAuthority.AUTHORITY_URL
            + "{action}/{registrationId}";

    private AppleIdentityProviderConfigMap configMap;
    private ClientRegistration clientRegistration;

    public AppleIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_APPLE, provider, realm);
        this.clientRegistration = null;
        this.configMap = new AppleIdentityProviderConfigMap();

    }

    public AppleIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(AppleIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new AppleIdentityProviderConfigMap();
        configMap.setConfiguration(props);
    }

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
        builder.scope(SCOPES);

        // 5. set key as secret and build JWT at request time
        builder.clientSecret(this.getClientJWK().toJSONString());

        // re-set registrationId since auto-configuration sets values provided from
        // issuer
        builder.registrationId(getProvider());

        // use email as name
        builder.userNameAttributeName("email");

        // make sure userinfo is not set to load info from JWT
        builder.userInfoUri(null);

        return builder.build();
    }

    public JWK getClientJWK() {
        if (!StringUtils.hasText(configMap.getPrivateKey())) {
            return null;
        }

        try {
            // expect a single PEM private key
            return JWK.parseFromPEMEncodedObjects(configMap.getPrivateKey());
        } catch (JOSEException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("invalid private key");
        }
    }

    public OIDCIdentityProviderConfig toOidcProviderConfig() {
        OIDCIdentityProviderConfig op = new OIDCIdentityProviderConfig(SystemKeys.AUTHORITY_APPLE, getProvider(),
                getRealm());
        OIDCIdentityProviderConfigMap configMap = new OIDCIdentityProviderConfigMap();
        configMap.setClientId(configMap.getClientId());
        configMap.setIssuerUri(ISSUER_URI);
        configMap.setClientAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        configMap.setEnablePkce(false);
        configMap.setScope(String.join(" ", SCOPES));
        configMap.setUserNameAttributeName("email");
        op.setConfigMap(configMap);

        return op;
    }

    public static AppleIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        AppleIdentityProviderConfig ap = new AppleIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ap.configMap = new AppleIdentityProviderConfigMap();
        ap.configMap.setConfiguration(cp.getConfiguration());

        ap.name = cp.getName();
        ap.description = cp.getDescription();
        ap.icon = cp.getIcon();
        ap.displayMode = cp.getDisplayMode();

        ap.persistence = cp.getPersistence();
        ap.linkable = cp.isLinkable();
        ap.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ap;
    }

}
