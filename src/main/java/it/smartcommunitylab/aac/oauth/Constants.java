package it.smartcommunitylab.aac.oauth;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;

public class Constants {

    public static final String TOKEN_TYPE_OPAQUE = "opaque";
    public static final String TOKEN_TYPE_JWT = "jwt";
    
    public static final String GRANT_TYPE_IMPLICIT = "implicit";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String GRANT_TYPE_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";

    public static final String RESPONSE_TYPE_NONE = "none";
    public static final String RESPONSE_TYPE_CODE = "code";
    public static final String RESPONSE_TYPE_TOKEN = "token";
    public static final String RESPONSE_TYPE_ID_TOKEN = "id_token";

    public static final String CLIENT_INFO_JWT_SIGN_ALG = "jwtSignAlgorithm";
    public static final String CLIENT_INFO_JWT_ENC_ALG = "jwtEncAlgorithm";
    public static final String CLIENT_INFO_JWT_ENC_METHOD = "jwtEncMethod";
    public static final String CLIENT_INFO_JWKS = "jwks";
    public static final String CLIENT_INFO_JWKS_URI = "jwksUri";
    public static final String CLIENT_INFO_WEBHOOK_AFTER_APPROVAL = "onAfterApprovalWebhook";

    public static final String[] GRANT_TYPES = {
            GRANT_TYPE_IMPLICIT,
            GRANT_TYPE_AUTHORIZATION_CODE,
            GRANT_TYPE_PASSWORD,
            GRANT_TYPE_CLIENT_CREDENTIALS,
            GRANT_TYPE_REFRESH_TOKEN,
            GRANT_TYPE_DEVICE_CODE
    };

    public static final String[] JWT_SIGN_ALGOS = {
            JWSAlgorithm.NONE.getName(),
            JWSAlgorithm.RS256.getName(),
            JWSAlgorithm.RS384.getName(),
            JWSAlgorithm.RS512.getName(),
            JWSAlgorithm.ES256.getName(),
            JWSAlgorithm.ES384.getName(),
            JWSAlgorithm.ES512.getName(),
            JWSAlgorithm.PS256.getName(),
            JWSAlgorithm.PS384.getName(),
            JWSAlgorithm.PS512.getName(),
            JWSAlgorithm.HS256.getName(),
            JWSAlgorithm.HS384.getName(),
            JWSAlgorithm.HS512.getName()
    };

    public static final String[] JWT_ENC_ALGOS = {
            JWEAlgorithm.DIR.getName(),
            JWEAlgorithm.A128GCMKW.getName(),
            JWEAlgorithm.A192GCMKW.getName(),
            JWEAlgorithm.A256GCMKW.getName(),

    };

}
