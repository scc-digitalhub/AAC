package it.smartcommunitylab.aac.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Component
public class JWTService {
    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Autowired
    private JWTSigningAndValidationService systemService;

    @Autowired
    private ClientKeyCacheService keyService;

    @Autowired
    private ClientDetailsRepository clientRepository;

    /*
     * Exported methods: sign
     */
    public JWT buildAndSignJWT(String clientId, JWTClaimsSet claims) {

        ClientDetailsEntity client = clientRepository.findByClientId(clientId);
        if (client == null) {
            logger.error("No client with id: " + clientId);
            return null;
        }

        return buildAndSignJWT(client, claims);
    }

    public JWT buildAndSignJWT(ClientDetailsEntity client, JWTClaimsSet claims) {
        // check if client requested encryption
        String encAlg = getEncryptAlgorithm(client);
        if (encAlg != null) {
            return buildAndEncryptJWT(client, claims);
        }

        // check if client has custom algo defined
        String signAlg = getSigningAlgorithm(client);
        if (signAlg == null) {
            // not set, fallback to default
            return buildAndSignJWT(systemService, claims);
        }

        // support plain jwt (alg NONE)
        if (Algorithm.NONE.getName().equals(signAlg)) {
            return buildPlainJWT(claims);
        }

        logger.trace("signAlg for client " + client.getClientId() + " is " + signAlg);

        // TODO support keyId from client config
        // try to get from service
        JWTSigningAndValidationService signer = keyService.getSigner(
                signAlg,
                client.getClientId(), client.getClientSecret(),
                getJwks(client), getJwksUri(client));

        if (signer == null) {
            logger.error("Couldn't find signer for client: " + client.getClientId());
            return null;
        }

        return buildAndSignJWT(signer, claims);

    }

    public JWT buildAndSignJWT(ClientAppBasic client, JWTClaimsSet claims) {
        // check if client requested encryption
        String encAlg = getEncryptAlgorithm(client);
        if (encAlg != null) {
            return buildAndEncryptJWT(client, claims);
        }

        // check if client has custom algo defined
        String signAlg = getSigningAlgorithm(client);
        if (signAlg == null) {
            // not set, fallback to default
            return buildAndSignJWT(systemService, claims);
        }

        // support plain jwt (alg NONE)
        if (Algorithm.NONE.getName().equals(signAlg)) {
            return buildPlainJWT(claims);
        }

        logger.trace("signAlg for client " + client.getClientId() + " is " + signAlg);

        // TODO support keyId from client config
        // try to get from service
        JWTSigningAndValidationService signer = keyService.getSigner(
                signAlg,
                client.getClientId(), client.getClientSecret(),
                getJwks(client), getJwksUri(client));

        if (signer == null) {
            logger.error("Couldn't find signer for client: " + client.getClientId());
            return null;
        }

        return buildAndSignJWT(signer, claims);
    }

    /*
     * Exported methods: encrypt
     */
    public JWT buildAndEncryptJWT(String clientId, JWTClaimsSet claims) {

        ClientDetailsEntity client = clientRepository.findByClientId(clientId);
        if (client == null) {
            logger.error("No client with id: " + clientId);
            return null;
        }

        return buildAndEncryptJWT(client, claims);
    }

    public JWT buildAndEncryptJWT(ClientDetailsEntity client, JWTClaimsSet claims) {
        // check if client has custom algo defined
        String encAlg = getEncryptAlgorithm(client);
        String encMethod = getEncryptMethod(client);

        if (encAlg == null || encMethod == null) {
            // not set, error
            logger.error("Requested JWE without algorithm set");
            return null;
        }

        logger.trace("encAlg for client " + client.getClientId() + " is " + encAlg);
        logger.trace("encMethod for client " + client.getClientId() + " is " + encMethod);

        // TODO support keyId from client config
        // try to get from service
        JWTEncryptionAndDecryptionService encrypter = keyService.getEncrypter(
                encAlg,
                client.getClientId(), client.getClientSecret(),
                getJwks(client), getJwksUri(client));

        if (encrypter == null) {
            logger.error("Couldn't find encrypter for client: " + client.getClientId());
            return null;
        }

        return buildAndEncryptJWT(encrypter, JWEAlgorithm.parse(encAlg), EncryptionMethod.parse(encMethod), claims);

    }

    public JWT buildAndEncryptJWT(ClientAppBasic client, JWTClaimsSet claims) {
        // check if client has custom algo defined
        String encAlg = getEncryptAlgorithm(client);
        String encMethod = getEncryptMethod(client);

        if (encAlg == null || encMethod == null) {
            // not set, error
            logger.error("Requested JWE without algorithm set");
            return null;
        }

        logger.trace("encAlg for client " + client.getClientId() + " is " + encAlg);
        logger.trace("encMethod for client " + client.getClientId() + " is " + encMethod);

        // TODO support keyId from client config
        // try to get from service
        JWTEncryptionAndDecryptionService encrypter = keyService.getEncrypter(
                encAlg,
                client.getClientId(), client.getClientSecret(),
                getJwks(client), getJwksUri(client));

        if (encrypter == null) {
            logger.error("Couldn't find encrypter for client: " + client.getClientId());
            return null;
        }

        return buildAndEncryptJWT(encrypter, JWEAlgorithm.parse(encAlg), EncryptionMethod.parse(encMethod), claims);

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
        // build a basic header
        JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
                signerKeyId,
                null, null);

        logger.debug("create signed jwt with algo " + signingAlg.getName() + " kid " + signerKeyId);
        SignedJWT token = new SignedJWT(header, claims);

        // sign it with the client key
        signer.signJwt((SignedJWT) token);

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

        encrypter.encryptJwt((JWEObject) token);

        return token;
    }

    /*
     * Default config
     */

    public String getDefaultSigningAlgorithm() {
        return systemService.getDefaultSigningAlgorithm().getName();
    }

    public String getDefaultSignerKeyId() {
        return systemService.getDefaultSignerKeyId();
    }

    /*
     * ClientDetails properties - TODO make private or protected
     */
    public String getJwksUri(ClientDetailsEntity client) {
        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS_URI);
    }

    public String getJwks(ClientDetailsEntity client) {
        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS);
    }

    public String getSigningAlgorithm(ClientDetailsEntity client) {
        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_SIGN_ALG);
    }

    public String getEncryptAlgorithm(ClientDetailsEntity client) {
        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_ALG);
    }

    public String getEncryptMethod(ClientDetailsEntity client) {
        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_METHOD);
    }

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

    /*
     * ClientApp properties - TODO make private
     */
    public String getJwksUri(ClientAppBasic client) {
        return client.getJwksUri();
    }

    public String getJwks(ClientAppBasic client) {
        return client.getJwks();
    }

    public String getSigningAlgorithm(ClientAppBasic client) {
        return client.getJwtSignAlgorithm();

    }

    public String getEncryptAlgorithm(ClientAppBasic client) {
        return client.getJwtEncAlgorithm();
    }

    public String getEncryptMethod(ClientAppBasic client) {
        return client.getJwtEncMethod();
    }

}
