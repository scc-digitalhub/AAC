package it.smartcommunitylab.aac.openid.apple.provider;

import java.io.Serializable;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Collections;
import java.util.Map;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

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
    public static final String AUTHORIZATION_URL = "https://appleid.apple.com/auth/authorize?response_mode=form_post";

    private static final String[] SCOPES = { "name", "email" };

    public static final String DEFAULT_REDIRECT_URL = "{baseUrl}" + AppleIdentityAuthority.AUTHORITY_URL
            + "{action}/{registrationId}";

    private AppleIdentityProviderConfigMap configMap;
    private ClientRegistration clientRegistration;
//    private JWK jwk;
    private ECPrivateKey privateKey;

    // thread-safe
    private static final JcaPEMKeyConverter pemConverter = new JcaPEMKeyConverter();

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
                        "invalid key, EC type required, found " + String.valueOf(privateKey.getAlgorithm()));
            }

            return (ECPrivateKey) privateKey;

        } catch (Exception e) {
            throw new IllegalArgumentException("invalid key: " + e.getMessage());
        }
    }
//    public JWK getClientJWK() {
//        if (!StringUtils.hasText(configMap.getPrivateKey())) {
//            return null;
//        }
//
//        // expect a single PEM EC keypair
//        // note: apple does not export public key
//        // we can derive it, or derive externally with openssl
//        // openssl ec -in private.pem -pubout -out public.pem
//
//        try {
////            return JWK.parseFromPEMEncodedObjects(configMap.getPrivateKey());
//            if (jwk == null) {
//                buildClientJWK();
//            }
//
//            if (jwk == null) {
//                throw new IllegalArgumentException("invalid private key");
//            }
//
//            return jwk;
//        } catch (IOException e) {
//            throw new IllegalArgumentException("invalid private key");
//        }
//    }

//    private void buildClientJWK() throws IOException {
//        String k = configMap.getPrivateKey();
//        if (k == null) {
//            return;
//        }
//
//        try {
//            // first read and validate PEM as EC private key
////            PemReader pemReader = new PemReader(new StringReader(k));
////            List<PemObject> pems = new ArrayList<>();
////
////            PemObject pemObj;
////            do {
////                pemObj = pemReader.readPemObject();
////                if (pemObj != null) {
////                    pems.add(pemObj);
////                }
////            } while (pemObj != null);
////
////            if (pems.isEmpty()) {
////                throw new IllegalArgumentException("invalid private key");
////            }
////
////            // if we get 2 keys expect public/private keypair
////            // use standard loader with validation
////            if (pems.size() > 1) {
////                this.jwk = ECKey.parseFromPEMEncodedObjects(k);
////                return;
////            }
//
////            // 1 key, check if defined as PRIVATE or EC PRIVATE
////            PemObject pemPrivate = pems.get(0);
////            if (PEMParser.TYPE_PRIVATE_KEY.equals(pemPrivate.getType())) {
////                // rewrite as EC PRIVATE key
////                pemPrivate = new PemObject(PEMParser.TYPE_EC_PRIVATE_KEY, pemPrivate.getContent());
////            }
////            StringWriter sw = new StringWriter();
////            JcaPEMWriter pemWriter = new JcaPEMWriter(sw);
////            pemWriter.writeObject(pemPrivate);
////            pemWriter.close();
////            String pemKey = sw.toString();
//
//            // read PEM format - requires bouncy castle
//            PEMParser parser = new PEMParser(new StringReader(k));
//            SubjectPublicKeyInfo publicKeyInfo = null;
//            PrivateKeyInfo privateKeyInfo = null;
//            Object pemObj;
//            do {
//                pemObj = parser.readObject();
//                if (pemObj instanceof SubjectPublicKeyInfo) {
//                    publicKeyInfo = (SubjectPublicKeyInfo) pemObj;
//                }
//                if (pemObj instanceof PrivateKeyInfo) {
//                    privateKeyInfo = (PrivateKeyInfo) pemObj;
//                }
//            } while (pemObj != null);
//
//            PublicKey publicKey = pemConverter.getPublicKey(publicKeyInfo);
//            PrivateKey privateKey = pemConverter.getPrivateKey(privateKeyInfo);
//
//            final ECPublicKey ecPubKey = (ECPublicKey) publicKey;
//            final ECParameterSpec pubParams = ecPubKey.getParams();
//
//            final Curve curve = Curve.forECParameterSpec(pubParams);
//            final ECKey.Builder builder = new ECKey.Builder(curve, (ECPublicKey) publicKey);
//            builder.privateKey((ECPrivateKey) privateKey);
//            builder.algorithm(JWSAlgorithm.ES256);
//            builder.keyID(configMap.getKeyId());
//
//            this.jwk = builder.build();
//
////          PemObject pemObj;
////          do {
////              pemObj = pemReader.readPemObject();
////              if (pemObj != null) {
////                  pems.add(pemObj);
////              }
////          } while (pemObj != null);
////
////          if (pems.isEmpty()) {
////              throw new IllegalArgumentException("invalid private key");
////          }
////            
////
////            // read pem key
////            Object obj = parser.readObject();
////
////            // we support only EC keys
////            if (!(obj instanceof PrivateKeyInfo)) {
////                throw new IllegalArgumentException("invalid private key");
////            }
////
////            // build private key
//////            PEMKeyPair keyPair = (PEMKeyPair) obj;
//////            ECPrivateKey privateKey = (ECPrivateKey) pemConverter.getPrivateKey(keyPair.getPrivateKeyInfo());
////            ECPrivateKey privateKey = (ECPrivateKey) pemConverter.getPrivateKey((PrivateKeyInfo) obj);
////            if (privateKey == null) {
////                throw new IllegalArgumentException("invalid private key");
////            }
////
////            // derive public key
////            // NOTE: hardcoded, requires bouncy castle
////            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
////            org.bouncycastle.math.ec.ECPoint q = spec.getG().multiply(privateKey.getS());
////
////////            ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
//////            BigInteger d = definingKey.getD();
//////            org.bouncycastle.jce.spec.ECParameterSpec ecSpec = definingKey.getParameters();
//////            ECPoint Q = definingKey.getParameters().getG().multiply(d);
////            org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(q, spec);
//////        PublicKey publicKeyGenerated = keyFactory.generatePublic(pubSpec);  
////
//////            ECParameterSpec paramsSpec = privateKey.getParams();
//////            
//////            ECPublicKeySpec keySpec = new ECPublicKeySpec(paramsSpec.getG()
//////                    params);
////            ECPublicKey publicKey = new BCECPublicKey("ECDSA", pubSpec, BouncyCastleProvider.CONFIGURATION);
////
////            Curve curve = Curve.forECParameterSpec(privateKey.getParams());
////            ECKey.Builder builder = new ECKey.Builder(curve, publicKey);
////            builder.privateKey((ECPrivateKey) privateKey);
////
////            this.jwk = builder.build();
//        } catch (Exception e) {
//            this.jwk = null;
//        }
//    }

//    private KeyPair readECKeys(String k) throws IOException {
//
//        final Reader pemReader = new StringReader(k);
//        final PEMParser parser = new PEMParser(pemReader);
//
//        // read pem key
//        Object pemObj = parser.readObject();
//
//        // we support only EC keys
//        if (!(pemObj instanceof PEMKeyPair)) {
//            throw new IllegalArgumentException("invalid private key");
//        }
//
//        // this will read keys, both as key pair or as private key only
//        KeyPair keyPair = pemConverter.getKeyPair((PEMKeyPair) pemObj);
//        PrivateKey privateKey = keyPair.getPrivate();
//        if (privateKey == null) {
//            throw new IllegalArgumentException("invalid private key");
//        }
//
//        PublicKey publicKey = keyPair.getPublic();
//        if (publicKey == null) {
//            // derive from private
//
//        }
//
//        // validate
//
//        return keyPair;
//    }

//    public PublicKey derivePublickey(BCECPrivateKey definingKey, Provider provider) {
//
//        KeyFactory keyFactory = KeyFactory.getInstance("EC", provider);
//
//        BigInteger d = definingKey.getD();
//        org.bouncycastle.jce.spec.ECParameterSpec ecSpec = definingKey.getParameters();
//        ECPoint Q = definingKey.getParameters().getG().multiply(d);
//
//        org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(Q, ecSpec);
//        PublicKey publicKeyGenerated = keyFactory.generatePublic(pubSpec);
//        return publicKeyGenerated;
//    }

    public OIDCIdentityProviderConfig toOidcProviderConfig() {
        OIDCIdentityProviderConfig op = new OIDCIdentityProviderConfig(SystemKeys.AUTHORITY_APPLE, getProvider(),
                getRealm());
        OIDCIdentityProviderConfigMap cMap = new OIDCIdentityProviderConfigMap();
        cMap.setClientId(configMap.getClientId());
        cMap.setIssuerUri(ISSUER_URI);
        cMap.setClientAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        cMap.setEnablePkce(false);
        cMap.setScope(String.join(",", SCOPES));
        cMap.setUserNameAttributeName("email");
        op.setConfigMap(cMap);

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
