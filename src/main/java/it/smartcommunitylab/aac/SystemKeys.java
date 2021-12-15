package it.smartcommunitylab.aac;

import org.springframework.http.MediaType;

public class SystemKeys {

    // to be upgraded when serialized objects are updated, breaks db
    // TODO implement
    public static final long AAC_COMMON_SERIAL_VERSION = 400L;
    public static final long AAC_CORE_SERIAL_VERSION = 400L;
    public static final long AAC_OAUTH2_SERIAL_VERSION = 400L;
    public static final long AAC_OIDC_SERIAL_VERSION = 400L;
    public static final long AAC_SAML_SERIAL_VERSION = 400L;
    public static final long AAC_SPID_SERIAL_VERSION = 400L;

    public static final String REALM_GLOBAL = "";
    public static final String REALM_INTERNAL = "internal";
    public static final String REALM_SYSTEM = "system";
    public static final String REALM_COMMON = "common";

    public static final String AUTHORITY_INTERNAL = "internal";
    public static final String AUTHORITY_OIDC = "oidc";
    public static final String AUTHORITY_SAML = "saml";
    public static final String AUTHORITY_SPID = "spid";
    public static final String AUTHORITY_CIE = "cie";
    public static final String AUTHORITY_MAPPER = "mapper";
    public static final String AUTHORITY_SCRIPT = "script";
    public static final String AUTHORITY_WEBHOOK = "webhook";
    public static final String AUTHORITY_WEBAUTHN = "webauthn";

    public static final String CLIENT_TYPE_OAUTH2 = "oauth2";
    public static final String CLIENT_TYPE_SAML = "saml";
    public static final String CLIENT_TYPE_OIDC = "oidc";

    public static final String SERVICE_CLIENT_TYPE_INTROSPECT = "introspect";
    public static final String SERVICE_CLIENT_TYPE_MACHINE = "machine";
    public static final String SERVICE_CLIENT_TYPE_WEB = "web";

    public static final String RESOURCE_USER = "user";
    public static final String RESOURCE_ACCOUNT = "account";
    public static final String RESOURCE_ATTRIBUTES = "attributes";
    public static final String RESOURCE_IDENTITY = "identity";
    public static final String RESOURCE_AUTHENTICATION = "authenticationToken";
    public static final String RESOURCE_SUBJECT = "subject";
    public static final String RESOURCE_CREDENTIALS = "credentials";
    public static final String RESOURCE_CLIENT = "client";
    public static final String RESOURCE_ROLE = "role";
    public static final String RESOURCE_SERVICE = "service";

    public static final String PATH_SEPARATOR = "/-/";

    public static final String PATH_USER = "/user";
    public static final String PATH_DEV = "/dev";
    public static final String PATH_ADMIN = "/admin";

    public static final String PERSISTENCE_LEVEL_NONE = "none";
    public static final String PERSISTENCE_LEVEL_SESSION = "session";
    public static final String PERSISTENCE_LEVEL_MEMORY = "memory";
    public static final String PERSISTENCE_LEVEL_REPOSITORY = "repository";

    public static final String EVENTS_LEVEL_NONE = "none";
    public static final String EVENTS_LEVEL_MINIMAL = "minimal";
    public static final String EVENTS_LEVEL_DETAILS = "details";
    public static final String EVENTS_LEVEL_FULL = "full";

    public static final String DISPLAY_MODE_BUTTON = "button";
    public static final String DISPLAY_MODE_FORM = "form";
    public static final String DISPLAY_MODE_SPID = "spid";

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_RECOVERY = "recovery";
    public static final String ACTION_ENABLE = "enable";
    public static final String ACTION_DISABLE = "disable";

    public static final String SLUG_PATTERN = "^[a-zA-Z0-9_-]+$";
    public static final String ID_PATTERN = "^[a-zA-Z0-9_-|]+$";
    public static final String SCOPE_PATTERN = "^[a-zA-Z.:]{3,}$";
    public static final String NAMESPACE_PATTERN = "^[a-zA-Z0-9._:/-]+$";
    public static final String KEY_PATTERN = "^[a-zA-Z0-9._]+$";
    public static final String URI_PATTERN = "^[a-zA-Z0-9._:/-]+$";
    public static final String SPECIAL_PATTERN = "^[a-zA-Z0-9!@#$&()\\-`.+,/\"]*$";
    public static final String JWT_PATTERN = "(^[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*$)";

    public static final int DEFAULT_APPROVAL_VALIDITY = 60 * 60 * 24 * 30;// 30 days

    public static final MediaType MEDIA_TYPE_YAML = MediaType.valueOf("text/yaml");
    public static final MediaType MEDIA_TYPE_YML = MediaType.valueOf("text/yml");
    public static final MediaType MEDIA_TYPE_XYAML = MediaType.valueOf("application/x-yaml");

}
