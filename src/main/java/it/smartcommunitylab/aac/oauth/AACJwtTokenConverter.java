package it.smartcommunitylab.aac.oauth;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.Signer;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.jwt.ClientKeyCacheService;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Service
public class AACJwtTokenConverter extends JwtAccessTokenConverter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${security.accesstoken.validity}")
    private int accessTokenValidity;

    @Value("${security.refreshtoken.validity}")
    private int refreshTokenValidity;

    @Autowired
    private JWTSigningAndValidationService jwtService;

    @Autowired
    private ClientKeyCacheService keyService;

    @Autowired
    private ClientDetailsRepository clientRepository;

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private ClaimManager claimManager;

    // re-declare objects from parent
    private Signer signer;
    private AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();
    private JsonParser objectMapper = JsonParserFactory.create();

    public AACJwtTokenConverter() {
        super();
    }

    @PostConstruct
    private void init() {
        logger.info("Initialize JWT tokenConverter");

        // ensure signer is null since superclass initializes it
        this.signer = null;

        // we will use the external jwtService for signin

//        // if set MAC key use it
//        if (!key.isEmpty()) {
//            // default is HMAC
//            this.setSigningKey(key);
//            signer = new MacSigner(key);
//        }
//
//        // if set kid look in keystore for RSA keypair
//        if (!kid.isEmpty() && !jwtKeyStore.getKeys().isEmpty()) {
//            try {
//                // fetch the matching key
//
//                for (JWK jwk : jwtKeyStore.getKeys()) {
//
//                    if (jwk instanceof RSAKey && kid.equals(jwk.getKeyID())) {
//
//                        // derive signer
//                        KeyPair keyPair = ((RSAKey) jwk).toKeyPair();
//                        this.setKeyPair(keyPair);
//                        this.signer = new RsaSigner((RSAPrivateKey) keyPair.getPrivate());
//
//                        // store kid because we need to add it to claims
//                        customHeaders.put("kid", kid);
//                    }
//                }
//            } catch (JOSEException e) {
//                logger.error("Error reading RSA key: " + e.getMessage());
//            }
//
//        }
    }

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug("enhance access token " + accessToken.getTokenType() + " for " + authentication.getName()
                + " value " + accessToken.toString());

        OAuth2Request request = authentication.getOAuth2Request();
        // build a new token with correct claims
        DefaultOAuth2AccessToken result = new DefaultOAuth2AccessToken(accessToken);
        Map<String, Object> info = new LinkedHashMap<String, Object>(accessToken.getAdditionalInformation());

        logger.debug("previous claims in token:" + info.keySet().toString());

        String tokenId = result.getValue();

        // check if value is already a JWT
        if (isJwt(tokenId)) {
            // extract tokenId from jti field
            Jwt old = JwtHelper.decode(tokenId);
            tokenId = (String) this.objectMapper.parseMap(old.getClaims()).get(JTI);
        }

        if (!info.containsKey(TOKEN_ID)) {
            info.put(TOKEN_ID, tokenId);
        } else {
            tokenId = (String) info.get(TOKEN_ID);
        }

        // duplicate list for claims, we will reuse info in result
        Map<String, Object> claims = new LinkedHashMap<String, Object>(info);

        // fetch client and reformat token
        String clientId = request.getClientId();
        ClientDetailsEntity client = clientRepository.findByClientId(clientId);

        // fetch user
        logger.debug("fetch user via authentication");
        User user = null;
        try {
            // fetch from auth
            Object principal = authentication.getPrincipal();
            org.springframework.security.core.userdetails.User auth = (org.springframework.security.core.userdetails.User) principal;

            // fetch user from db
            long userId = Long.parseLong(auth.getUsername());
            user = userManager.findOne(userId);

        } catch (Exception e) {
            // user is not available, thus all user claims will fail
            logger.error("user not found: " + e.getMessage());
        }

        // add claims for user details if requested via scopes
        if (user != null) {
            logger.debug("fetch profile via profilemanager");

            Set<String> scope = new HashSet<>(request.getScope());
//            if (!scope.contains(Config.OPENID_SCOPE)) {
//                scope.add(Config.OPENID_SCOPE);
//            }
            Collection<? extends GrantedAuthority> selectedAuthorities = authentication.getUserAuthentication()
                    .getAuthorities();
//            if (selectedAuthorities != null && !selectedAuthorities.isEmpty())
//                claims.put(AUTHORITIES,
//                        selectedAuthorities.stream().map(a -> a.getAuthority()).collect(Collectors.toSet()));
            Map<String, Object> userClaims = claimManager.createUserClaims(user.getId().toString(), selectedAuthorities,
                    client, scope, null, null);
            // set directly, ignore extracted
            userClaims.remove("sub");
            claims.putAll(userClaims);
        }

        // explicitly set sub to userId
        // ignore claims?
        if (user != null) {
            claims.put("sub", user.getId());
        }

        Set<String> audiences = new HashSet<>();
        audiences.add(clientId);
        audiences.addAll(getServiceIds(accessToken.getScope()));
        claims.put(AUD, audiences);
        claims.put("azp", clientId);

//        // reset additional claims for scopes to a list instead of an array
//        info.put("scope", String.join(" ", request.getScope()));

        // save claims and encode
        result.setAdditionalInformation(claims);
        result.setValue(encode(result, authentication));

        // verify and format refresh token
        OAuth2RefreshToken refreshToken = result.getRefreshToken();
        if (refreshToken != null) {
            DefaultOAuth2AccessToken encodedRefreshToken = new DefaultOAuth2AccessToken(accessToken);
            encodedRefreshToken.setValue(refreshToken.getValue());
            // Refresh tokens do not expire unless explicitly of the right type
            encodedRefreshToken.setExpiration(null);
            try {
                Map<String, Object> map = objectMapper
                        .parseMap(JwtHelper.decode(refreshToken.getValue()).getClaims());
                if (map.containsKey(TOKEN_ID)) {
                    encodedRefreshToken.setValue(map.get(TOKEN_ID).toString());
                }
            } catch (IllegalArgumentException e) {
            }
            Map<String, Object> refreshTokenInfo = new LinkedHashMap<String, Object>(
                    accessToken.getAdditionalInformation());
            refreshTokenInfo.put(TOKEN_ID, encodedRefreshToken.getValue());
            refreshTokenInfo.put(ACCESS_TOKEN_ID, tokenId);
//            // reset additional claims for scopes to a list instead of an array
//            refreshTokenInfo.put("scope", String.join(" ", request.getScope()));
            encodedRefreshToken.setAdditionalInformation(refreshTokenInfo);

            DefaultOAuth2RefreshToken token = new DefaultOAuth2RefreshToken(
                    encode(encodedRefreshToken, authentication));
            if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
                Date expiration = ((ExpiringOAuth2RefreshToken) refreshToken).getExpiration();
                encodedRefreshToken.setExpiration(expiration);
                token = new DefaultExpiringOAuth2RefreshToken(encode(encodedRefreshToken, authentication), expiration);
            }
            result.setRefreshToken(token);
        }

        // reset additional claims to avoid leaking in json response
        // they should appear only in JWT, still we need JTI from tokenId
        result.setAdditionalInformation(info);

        // rewrite token type because we don't want "bearer" lowercase in response...
        if (result.getTokenType().equals(OAuth2AccessToken.BEARER_TYPE.toLowerCase())) {
            result.setTokenType(OAuth2AccessToken.BEARER_TYPE);
        }

        return result;
    }

    @Override
    protected String encode(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug("encode access token " + accessToken.getTokenType() + " for " + authentication.getName()
                + " value " + accessToken.toString());

        // convert base claims to map
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) tokenConverter.convertAccessToken(accessToken,
                authentication);

        logger.debug("dump claims " + claims.keySet().toString());

        OAuth2Request request = authentication.getOAuth2Request();

        // fetch data and reformat token
        String clientId = request.getClientId();
        ClientDetailsEntity client = clientRepository.findByClientId(clientId);
        List<String> audiences = new LinkedList<>();
        audiences.add(clientId);
        audiences.addAll(getServiceIds(accessToken.getScope()));
        claims.put(AUD, audiences);

        // reset additional claims for scopes to a list instead of an array
        claims.put("scope", String.join(" ", request.getScope()));
        // TODO rewrite checks with whitelist/graylist
        // ie refresh == whitelisted
        // access == requested&allowed&prefixed

        // handle authorities claim here since token converter HARDCODES it...
        if (this.isRefreshToken(accessToken)) {
            // clear authorities from refresh token
            claims.remove(AUTHORITIES);
        }

        // clear authorities from access token
        claims.remove(AUTHORITIES);
//        if (!request.getScope().contains("user.roles.me")) {
//            // clear authorities from access token
//            claims.remove(AUTHORITIES);
//        }

//       MOVED to claimManager       
//        // rewrite user_name as sub, if requested by scopes we will already have an
//        // username claim
//        if (claims.containsKey("user_name")) {
//            claims.put("sub", claims.get("user_name").toString());
//            claims.remove("user_name");
//        }
//        
//        //properly populate user_name for spring clients
//        //custom definition to satisfy spring security 
//        //https://github.com/spring-projects/spring-security-oauth/blob/master/spring-security-oauth2/src/main/java/org/springframework/security/oauth2/provider/token/UserAuthenticationConverter.java
//        if (claims.containsKey("username")) {
//            claims.put("user_name", claims.get("username").toString());
//        }       

        // clear id-token from claims to avoid embedding id_token in access token JWT
        if (claims.containsKey("id_token")) {
            claims.remove("id_token");
        }

//        // convert to JSON
//        String content;
//        try {
//            content = this.objectMapper.formatMap(claims);
//        } catch (Exception e) {
//            throw new IllegalStateException("Cannot convert access token to JSON", e);
//        }
//
//        // figure out which signer to use:
//        // if RSA keys provided use the same for all
//        // if global key set use singleton HMAC
//        // otherwise use client secret as HMAC key
//        Signer s = getSigner(client);
//
//        String token = JwtHelper.encode(
//                content,
//                s,
//                this.customHeaders).getEncoded();

        // leverage services for jwt creation
//        JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm();
//
//        // check if client has set custom preferences
//        if (ClientKeyCacheService.getSignedResponseAlg(client) != null) {
//            signingAlg = ClientKeyCacheService.getSignedResponseAlg(client);
//        }

        // build claims
        JWTClaimsSet.Builder jwtClaims = new JWTClaimsSet.Builder();
        // id
        jwtClaims.jwtID(claims.get(TOKEN_ID).toString());
        // base
        jwtClaims.issuer(issuer);
        jwtClaims.subject(authentication.getName());
        jwtClaims.claim("azp", clientId);
        
        jwtClaims.audience(audiences);
        // time
        if (accessToken.getExpiration() != null) {
            Date expiration = accessToken.getExpiration();
            Date iat = new Date();
            // need to subtract validity from expiration to avoid updating at each request
            if (isRefreshToken(accessToken)) {
                int refreshTokenValiditySeconds = ClientKeyCacheService.getRefreshTokenValiditySeconds(client);
                if (refreshTokenValiditySeconds < 0) {
                    // use system default
                    refreshTokenValiditySeconds = refreshTokenValidity;
                }
                iat = new Date(expiration.getTime() - (refreshTokenValiditySeconds * 1000L));

            } else {
                int accessTokenValiditySeconds = ClientKeyCacheService.getAccessTokenValiditySeconds(client);
                if (accessTokenValiditySeconds < 0) {
                    // use system default
                    accessTokenValiditySeconds = accessTokenValidity;
                }
                iat = new Date(expiration.getTime() - (accessTokenValiditySeconds * 1000L));

            }

            jwtClaims.issueTime(iat);
            jwtClaims.notBeforeTime(iat);

            jwtClaims.expirationTime(expiration);
        }

        // add all claims, avoiding registered
        claims.entrySet().forEach(e -> {
            if (!JWTClaimsSet.getRegisteredNames().contains(e.getKey())) {
                jwtClaims.claim(e.getKey(), e.getValue());
            }
        });

        JWT token = null;

        // check client wants also encryption
        if (ClientKeyCacheService.getEncryptedResponseAlg(client) != null
                && !ClientKeyCacheService.getEncryptedResponseAlg(client).equals(Algorithm.NONE)
                && ClientKeyCacheService.getEncryptedResponseEnc(client) != null
                && !ClientKeyCacheService.getEncryptedResponseEnc(client).equals(Algorithm.NONE)
                && (!Strings.isNullOrEmpty(ClientKeyCacheService.getJwksUri(client))
                        || ClientKeyCacheService.getJwks(client) != null)) {

            JWTEncryptionAndDecryptionService encrypter = keyService.getEncrypter(client);

            if (encrypter != null) {
                token = new EncryptedJWT(new JWEHeader(ClientKeyCacheService.getEncryptedResponseAlg(client),
                        ClientKeyCacheService.getEncryptedResponseEnc(client)), jwtClaims.build());

                encrypter.encryptJwt((JWEObject) token);

            } else {
                logger.error("Couldn't find encrypter for client: " + client.getClientId());
            }

        } else {
            // check if custom signed defined
            if (ClientKeyCacheService.getSignedResponseAlg(client) != null) {
                JWTSigningAndValidationService signer = keyService.getSigner(client);

                if (signer != null) {
                    JWSAlgorithm signingAlg = signer.getDefaultSigningAlgorithm();
                    String signerKeyId = signer.getDefaultSignerKeyId();
                    JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
                            signerKeyId,
                            null, null);

                    logger.debug("create signed jwt with algo " + signingAlg.getName() + " kid " + signerKeyId);
                    token = new SignedJWT(header, jwtClaims.build());

                    // sign it with the client key
                    signer.signJwt((SignedJWT) token);
                } else {
                    logger.error("Couldn't find signer for client: " + client.getClientId());
                }

            } else {
                // use system
                JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm();
                String signerKeyId = jwtService.getDefaultSignerKeyId();
                JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
                        signerKeyId,
                        null, null);

                logger.debug("create signed jwt with algo " + signingAlg.getName() + " kid " + signerKeyId);
                token = new SignedJWT(header, jwtClaims.build());

                // sign it with the server's key
                jwtService.signJwt((SignedJWT) token);
            }

        }

        // serialize to string
        String result = token.serialize();
        logger.debug("encoded jwt token " + result);

        return result;
    }

    @Override
    public Map<String, ?> convertAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        logger.debug("convert access token " + token.toString());
        return super.convertAccessToken(token, authentication);
    }

    @Override
    public OAuth2AccessToken extractAccessToken(String value, Map<String, ?> map) {
        logger.debug("extract access token " + value);
        return super.extractAccessToken(value, map);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        logger.debug("extract authentication " + map.toString());
        return super.extractAuthentication(map);
    }

    @Override
    protected Map<String, Object> decode(String token) {
        logger.debug("decode token " + token);
        return super.decode(token);
    }

    private boolean isJwt(String value) {
        // simply check for format header.body.signature
        int firstPeriod = value.indexOf('.');
        int lastPeriod = value.lastIndexOf('.');

        if (firstPeriod <= 0 || lastPeriod <= firstPeriod) {
            return false;
        } else {
            return true;
        }
    }

    
    private Set<String> getServiceIds(Set<String> scopes) {
    	if (scopes != null && !scopes.isEmpty()) {
    		return serviceManager.findServiceIdsByScopes(scopes);
    	}
    	return Collections.emptySet();
    }
//    private Signer getSigner(ClientDetailsEntity client) {
//        if (this.signer != null) {
//            // always use global if defined
//            return this.signer;
//        } else {
//            // can't reuse since client secret keys could change
//            return new MacSigner(client.getClientSecret());
//        }
//    }

}
