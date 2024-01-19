/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac;

import org.springframework.http.MediaType;

public class SystemKeys {

    // to be upgraded when serialized objects are updated, breaks db
    // TODO implement
    public static final long AAC_COMMON_SERIAL_VERSION = 420L;
    public static final long AAC_CORE_SERIAL_VERSION = 420L;
    public static final long AAC_OAUTH2_SERIAL_VERSION = 420L;
    public static final long AAC_OIDC_SERIAL_VERSION = 420L;
    public static final long AAC_SAML_SERIAL_VERSION = 420L;
    public static final long AAC_APPLE_SERIAL_VERSION = 420L;
    public static final long AAC_WEBAUTHN_SERIAL_VERSION = 420L;
    public static final long AAC_INTERNAL_SERIAL_VERSION = 420L;
    public static final long AAC_OPENIDFED_SERIAL_VERSION = 500L;

    public static final String REALM_GLOBAL = "";
    public static final String REALM_INTERNAL = "internal";
    public static final String REALM_SYSTEM = "system";
    public static final String REALM_COMMON = "common";

    public static final String AUTHORITY_AAC = "aac";
    public static final String AUTHORITY_INTERNAL = "internal";
    public static final String AUTHORITY_OIDC = "oidc";
    public static final String AUTHORITY_SAML = "saml";
    //    public static final String AUTHORITY_SPID = "spid";
    public static final String AUTHORITY_CIE = "cie";
    public static final String AUTHORITY_MAPPER = "mapper";
    public static final String AUTHORITY_SCRIPT = "script";
    public static final String AUTHORITY_WEBHOOK = "webhook";
    public static final String AUTHORITY_OAUTH2 = "oauth2";
    public static final String AUTHORITY_APPLE = "apple";
    public static final String AUTHORITY_WEBAUTHN = "webauthn";
    public static final String AUTHORITY_PASSWORD = "password";
    public static final String AUTHORITY_TEMPLATE = "template";
    public static final String AUTHORITY_TOS = "tos";
    public static final String AUTHORITY_OPENIDFED = "openidfed";

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
    public static final String RESOURCE_PRINCIPAL = "principal";
    public static final String RESOURCE_SUBJECT = "subject";
    public static final String RESOURCE_CREDENTIALS = "credentials";
    public static final String RESOURCE_CLIENT = "client";
    public static final String RESOURCE_ROLE = "role";
    public static final String RESOURCE_SPACE = "space";
    public static final String RESOURCE_SERVICE = "service";
    public static final String RESOURCE_GROUP = "group";
    public static final String RESOURCE_CREDENTIALS_SECRET = "credentials_secret";
    public static final String RESOURCE_CREDENTIALS_JWKS = "credentials_jwks";
    public static final String RESOURCE_PROVIDER = "provider";
    public static final String RESOURCE_REALM = "realm";
    public static final String RESOURCE_CONFIG = "config";
    public static final String RESOURCE_LOGIN = "login";
    public static final String RESOURCE_IDENTITY_PROVIDER = "identity_provider";
    public static final String RESOURCE_ATTRIBUTE_PROVIDER = "attribute_provider";
    public static final String RESOURCE_IDENTITY_SERVICE = "identity_service";
    public static final String RESOURCE_ATTRIBUTE_SERVICE = "attribute_service";
    public static final String RESOURCE_ACCOUNT_SERVICE = "account_service";
    public static final String RESOURCE_CREDENTIALS_SERVICE = "credentials_service";
    public static final String RESOURCE_TEMPLATE_PROVIDER = "template_provider";
    public static final String RESOURCE_TEMPLATE = "template";
    public static final String RESOURCE_SETTINGS = "settings";

    public static final String PATH_SEPARATOR = "/-/";
    public static final String ID_SEPARATOR = ":";
    public static final String URN_PROTOCOL = "urn://";
    public static final String URN_SEPARATOR = "/";
    public static final String SLUG_SEPARATOR = "_";

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

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_RECOVERY = "recovery";
    public static final String ACTION_ENABLE = "enable";
    public static final String ACTION_DISABLE = "disable";

    public static final String SLUG_PATTERN = "^[a-zA-Z0-9._-]+$";
    public static final String ID_PATTERN = "^[a-zA-Z0-9_-|]+$";
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._@-]+$";
    public static final String SCOPE_PATTERN = "^[a-zA-Z.:]{3,}$";
    public static final String RESOURCE_PATTERN = "^[a-zA-Z0-9._:/-]+$";
    public static final String NAMESPACE_PATTERN = "^[a-zA-Z0-9._:/-]+$";
    public static final String KEY_PATTERN = "^[a-zA-Z0-9._]+$";
    public static final String URI_PATTERN = "^[a-zA-Z0-9._:/-]+$";
    public static final String SPECIAL_PATTERN = "^[a-zA-Z0-9!@#$&()\\-`.+,/\"]*$";
    public static final String JWT_PATTERN = "(^[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*$)";

    public static final int DEFAULT_APPROVAL_VALIDITY = 60 * 60 * 24 * 30; // 30 days

    public static final MediaType MEDIA_TYPE_APPLICATION_YAML = new MediaType("application", "yaml");
    public static final MediaType MEDIA_TYPE_TEXT_YAML = new MediaType("text", "yaml");
    public static final MediaType MEDIA_TYPE_APPLICATION_XYAML = new MediaType("application", "x-yaml");

    public static final String MEDIA_TYPE_APPLICATION_YAML_VALUE = "application/yaml";
    public static final String MEDIA_TYPE_TEXT_YAML_VALUE = "text/yaml";
    public static final String MEDIA_TYPE_APPLICATION_XYAML_VALUE = "application/x-yaml";
}
