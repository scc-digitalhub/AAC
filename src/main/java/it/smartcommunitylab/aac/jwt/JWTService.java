package it.smartcommunitylab.aac.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.openid.utils.IdTokenHashUtils;

@Service
public class JWTService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    private final ClientKeyCacheService keyCacheService = new ClientKeyCacheService();

    private JWTSigningAndValidationService defaultSignService;

    public JWTService(JWTSigningAndValidationService signService) {
        Assert.notNull(signService, "a default sign service is mandatory");
        this.defaultSignService = signService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(defaultSignService, "a default signing service is required");

    }

    // use this to change the default signer, for example for key rotation
    public void setDefaultSignService(JWTSigningAndValidationService defaultSignService) {
        this.defaultSignService = defaultSignService;
    }

    /*
     * Exported methods: sign
     */

    public JWT buildAndSignJWT(OAuth2ClientDetails clientDetails, JWTClaimsSet claims) {

        String clientId = clientDetails.getClientId();

        // check if client requested encryption
        String encAlg = clientDetails.getJwtEncAlgorithm();
        String encMethod = clientDetails.getJwtEncMethod();
        if (StringUtils.hasText(encMethod) && StringUtils.hasText(encAlg)) {
            return buildAndEncryptJWT(clientDetails, claims);
        }

        // check if client has custom algo defined
        String signAlg = clientDetails.getJwtSignAlgorithm();
        if (!StringUtils.hasText(signAlg)) {
            // not set, fallback to default signer
            return buildAndSignJWT(defaultSignService, claims);
        }

        // support plain jwt (alg NONE)
        if (Algorithm.NONE.getName().equals(signAlg)) {
            return buildPlainJWT(claims);
        }

        logger.trace("signAlg for client " + clientId + " is " + signAlg);

        // TODO support keyId from client config
        // try to get from service
        JWTSigningAndValidationService signer = keyCacheService.getSigner(
                signAlg,
                clientId, clientDetails.getClientSecret(),
                clientDetails.getJwks(), clientDetails.getJwksUri());

        if (signer == null) {
            logger.error("Couldn't find signer for client: " + clientId);
            return null;
        }

        return buildAndSignJWT(signer, claims);

    }

    /*
     * Exported methods: encrypt
     */

    public JWT buildAndEncryptJWT(OAuth2ClientDetails clientDetails, JWTClaimsSet claims) {
        String clientId = clientDetails.getClientId();

        // check if client has custom algo defined
        String encAlg = clientDetails.getJwtEncAlgorithm();
        String encMethod = clientDetails.getJwtEncMethod();
        if (!StringUtils.hasText(encMethod) || !StringUtils.hasText(encAlg)) {
            // not set, error
            logger.error("Requested JWE without algorithm set");
            return null;
        }

        logger.trace("encAlg for client " + clientId + " is " + encAlg);
        logger.trace("encMethod for client " + clientId + " is " + encMethod);

        // TODO support keyId from client config
        // try to get from service
        JWTEncryptionAndDecryptionService encrypter = keyCacheService.getEncrypter(
                encAlg,
                clientId, clientDetails.getClientSecret(),
                clientDetails.getJwks(), clientDetails.getJwksUri());

        if (encrypter == null) {
            logger.error("Couldn't find encrypter for client: " + clientId);
            return null;
        }

        return buildAndEncryptJWT(encrypter, JWEAlgorithm.parse(encAlg), EncryptionMethod.parse(encMethod), claims);

    }

    /*
     * Exported methods: hash
     */

    public Base64URL hashAccessToken(OAuth2ClientDetails clientDetails, String value) {
        // check if client has custom algo defined
        String signAlg = clientDetails.getJwtSignAlgorithm();
        if (!StringUtils.hasText(signAlg)) {
            // fallback
            signAlg = defaultSignService.getDefaultSigningAlgorithm().getName();
        }

        JWSAlgorithm signingAlg = JWSAlgorithm.parse(signAlg);

        Base64URL at_hash = IdTokenHashUtils.getAccessTokenHash(signingAlg, value);

        return at_hash;
    }

    /*
     * Sign
     */
    private PlainJWT buildPlainJWT(JWTClaimsSet claims) {
        logger.debug("create plain jwt");

        PlainJWT token = new PlainJWT(claims);

        return token;

    }

    private SignedJWT buildAndSignJWT(JWTSigningAndValidationService signer, JWTClaimsSet claims) {

        JWSAlgorithm signingAlg = signer.getDefaultSigningAlgorithm();
        // TODO support keyId from client config
        String signerKeyId = signer.getDefaultSignerKeyId();
        // build a basic header, base64 encoded
        JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
                signerKeyId, true,
                null, null);

        logger.debug("create signed jwt with algo " + signingAlg.getName() + " kid " + signerKeyId);
        SignedJWT token = new SignedJWT(header, claims);

        // sign it with the correct key via signer
        signer.signJwt(token);

        return token;

    }

    /*
     * Encrypt
     */

    private EncryptedJWT buildAndEncryptJWT(JWTEncryptionAndDecryptionService encrypter, JWEAlgorithm encAlg,
            EncryptionMethod encMethod, JWTClaimsSet claims) {

        // TODO support keyId from client config
        String encKeyId = encrypter.getDefaultEncryptionKeyId();
        // build a basic header
        JWEHeader header = new JWEHeader(encAlg, encMethod);
        EncryptedJWT token = new EncryptedJWT(header, claims);

        // sign it with the correct method+key via encrypter
        encrypter.encryptJwt(token);

        return token;
    }

    /*
     * Default config
     */

    public String getDefaultSigningAlgorithm() {
        return defaultSignService.getDefaultSigningAlgorithm().getName();
    }

    public String getDefaultSignerKeyId() {
        return defaultSignService.getDefaultSignerKeyId();
    }

//    /*
//     * ClientDetails properties - TODO make private or protected
//     */
//    public String getJwksUri(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS_URI);
//    }
//
//    public String getJwks(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS);
//    }
//
//    public String getSigningAlgorithm(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_SIGN_ALG);
//    }
//
//    public String getEncryptAlgorithm(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_ALG);
//    }
//
//    public String getEncryptMethod(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_METHOD);
//    }

//    public JWSAlgorithm getSignedResponseAlg(ClientDetailsEntity client) {
//        String signedResponseAlg = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_SIGN_ALG);
//        return signedResponseAlg != null ? JWSAlgorithm.parse(signedResponseAlg) : null;
//
//    }
//
//    public JWEAlgorithm getEncryptedResponseAlg(ClientDetailsEntity client) {
//        String encResponseAlg = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_ALG);
//        return encResponseAlg != null ? JWEAlgorithm.parse(encResponseAlg) : null;
//    }
//
//    public EncryptionMethod getEncryptedResponseEnc(ClientDetailsEntity client) {
//        String encResponseEnc = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_METHOD);
//        return encResponseEnc != null ? EncryptionMethod.parse(encResponseEnc) : null;
//    }

//    /*
//     * ClientApp properties - TODO make private
//     */
//    public String getJwksUri(ClientAppBasic client) {
//        return client.getJwksUri();
//    }
//
//    public String getJwks(ClientAppBasic client) {
//        return client.getJwks();
//    }
//
//    public String getSigningAlgorithm(ClientAppBasic client) {
//        return client.getJwtSignAlgorithm();
//
//    }
//
//    public String getEncryptAlgorithm(ClientAppBasic client) {
//        return client.getJwtEncAlgorithm();
//    }
//
//    public String getEncryptMethod(ClientAppBasic client) {
//        return client.getJwtEncMethod();
//    }

}
