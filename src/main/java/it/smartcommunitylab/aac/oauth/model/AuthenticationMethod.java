package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AuthenticationMethod {

    /**
     * Clients that have received a client secret from the authorization server
     * authenticate with the authorization server in accordance with section 3.2.1
     * of OAuth 2.0 using HTTP Basic authentication. This is the default if no
     * method has been registered for the client.
     */
    CLIENT_SECRET_BASIC("client_secret_basic"),

    /**
     * Clients that have received a client secret from the authorization server
     * authenticate with the authorization server in accordance with section 3.2.1
     * of OAuth 2.0 by including the client credentials in the request body.
     */
    CLIENT_SECRET_POST("client_secret_post"),

    /**
     * Clients that have received a client secret from the authorization server,
     * create a JWT using an HMAC SHA algorithm, such as HMAC SHA-256. The HMAC
     * (Hash-based Message Authentication Code) is calculated using the value of
     * client secret as the shared key. The client authenticates in accordance with
     * section 2.2 of (JWT) Bearer Token Profiles and OAuth 2.0 Assertion Profile.
     */
    CLIENT_SECRET_JWT("client_secret_jwt"),

    /**
     * Clients that have registered a public key sign a JWT using the RSA algorithm
     * if a RSA key was registered or the ECDSA algorithm if an Elliptic Curve key
     * was registered (see JWA for the algorithm identifiers). The client
     * authenticates in accordance with section 2.2 of (JWT) Bearer Token Profiles
     * and OAuth 2.0 Assertion Profile.
     */
    PRIVATE_KEY_JWT("private_key_jwt"),

//    /**
//     * PKI mutual TLS OAuth client authentication. See OAuth 2.0 Mutual TLS Client
//     * Authentication and Certificate Bound Access Tokens (RFC 8705), section 2.1.
//     */
//    TLS_CLIENT_AUTH("tls_client_auth"),
//
//    /**
//     * Self-signed certificate mutual TLS OAuth client authentication. See OAuth 2.0
//     * Mutual TLS Client Authentication and Certificate Bound Access Tokens (RFC
//     * 8705), section 2.2.
//     */
//    SELF_SIGNED_TLS_CLIENT_AUTH("self_signed_tls_client_auth"),
//
//    /**
//     * Client authentication by means of a request object at the authorization or
//     * PAR endpoints. Intended for OpenID Connect Federation 1.0 clients undertaking
//     * automatic registration. See OpenID Connect Federation 1.0.
//     */
//    REQUEST_OBJECT("request_object"),

    /**
     * The client is a public client as defined in OAuth 2.0 and does not have a
     * client secret.
     */
    NONE("none");

    private final String value;

    AuthenticationMethod(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public static AuthenticationMethod parse(String value) {
        for (AuthenticationMethod gt : AuthenticationMethod.values()) {
            if (gt.value.equalsIgnoreCase(value)) {
                return gt;
            }
        }

        return null;
    }
}
