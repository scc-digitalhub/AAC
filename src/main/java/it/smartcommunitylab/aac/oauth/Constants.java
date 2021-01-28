package it.smartcommunitylab.aac.oauth;

public class Constants {

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
}
