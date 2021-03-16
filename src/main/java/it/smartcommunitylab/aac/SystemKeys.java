package it.smartcommunitylab.aac;

public class SystemKeys {
    public static final String REALM_GLOBAL = "";

    public static final String AUTHORITY_INTERNAL = "internal";
    public static final String AUTHORITY_OIDC = "oidc";
    public static final String AUTHORITY_SAML = "saml";

    public static final String CLIENT_TYPE_OAUTH2 = "oauth2";
    public static final String CLIENT_TYPE_SAML = "saml";
    public static final String CLIENT_TYPE_OIDC = "oidc";

    public static final String RESOURCE_USER = "user";
    public static final String RESOURCE_ACCOUNT = "account";
    public static final String RESOURCE_ATTRIBUTES = "attributes";
    public static final String RESOURCE_IDENTITY = "identity";
    public static final String RESOURCE_AUTHENTICATION = "authenticationToken";
    public static final String RESOURCE_SUBJECT = "subject";

    public static final String PATH_SEPARATOR = "/-/";

    public static final String PATH_USER = "/user";
    public static final String PATH_DEV = "/dev";
    public static final String PATH_ADMIN = "/admin";

    public static final String PERSISTENCE_LEVEL_NONE = "none";
    public static final String PERSISTENCE_LEVEL_SESSION = "session";
    public static final String PERSISTENCE_LEVEL_MEMORY = "memory";
    public static final String PERSISTENCE_LEVEL_REPOSITORY = "repository";

    public static final String SLUG_PATTERN = "^[a-zA-Z0-9_-]+$";
}
