[![Build Status](https://travis-ci.org/scc-digitalhub/AAC.svg?branch=master)](https://travis-ci.org/scc-digitalhub/AAC)

# AAC
Authentication and Authorization Control Module.

This module exposes the OAuth2.0 protocol functionality for user authentication and
for the authorization of resource access (e.g., APIs). The module allows for managing 
user registration and for authenticating user using social accounts, in particular Google+ and Facebook. 

## 1. Installation Requirements
While AAC is distributed as a self contained jar, for production deployments the installation requirements are:

* RDBMS (tested on MySQL 5.5+)
* Java 11+
* Apache Maven (3.0+)

## 2. Configuration

AAC configuration properties are configured through the corresponding Spring profile settings. By default, ``local`` 
profile is activated and the execution environment is configured in the ``src/main/resources/application-local.yml`` file. 
The example configuration for the local execution is defined in ``application-local.yml.example``.

Alternatively, environment variables can be used to configure most of the settings.

> **NOTE:** 
AAC can be run with the default configuration in a development/demo mode, which leverages an embedded H2 database. Please configure an external database for production. 

> **NOTE:**
The default configuration is equivalent to the examples in each section. Admins can override only the desired settings, either via configuration files or via environment variables, and keep all the other settings at their default values.

### 2.1. DB Configuration

AAC default configuration uses an embedded H2 database. In order to configure a production environment perform
DB Configuration via the JDBC driver configuration, e.g. for MySQL

    jdbc:
      dialect: org.hibernate.dialect.MySQLDialect    
      driver: com.mysql.jdbc.Driver
      url: jdbc:mysql://localhost:3306/aac
      user: ac
      password: ac

or via ENV

    JDBC_DIALECT = org.hibernate.dialect.MySQLDialect    
    JDBC_DRIVER = com.mysql.jdbc.Driver
    JDBC_URL = jdbc:mysql://localhost:3306/aac
    JDBC_USER = ac
    JDBC_PASS = ac
     

The database and the user with the privileges should have been already configured.

Supported production databases are :
* MySQL 5.5+
* PostgreSQL 9+


### 2.2. AAC Endpoint Configuration

AAC exposes an HTTP endpoint which can be configured via the following properties:

    server:
      host: localhost
      port: 8080
      servlet:
        contextPath: /

or ENV

    SERVER_HOST = localhost
    SERVER_PORT = 8080
    SERVER_CONTEXT = /aac

When deployed behind a reverse proxy, for example when using HTTPS termination, the following setting should be updated with the correct external URL accessed by the clients. This is needed for redirects and authentication callbacks.

    application.url: http://localhost:8080/aac
    
or ENV

    APPLICATION_EXT_URL = http://localhost:8080/aac
    
It is mandatory to provide the fully qualified URL, with the correct protocol and path as exposed by the reverse proxy.     

### 2.3. Security Configuration

In order to deploy AAC in a production enviroment, it is mandatory to configure the security settings described in the following sections.

#### Admin account

The default configuration creates an administrative account with complete access configured as :

    username = admin
    password = admin

The following security properties MUST be configured for a production environment:

    admin:
      username: admin
      password: admin

or via ENV

      ADMIN_USERNAME = admin
      ADMIN_PASSWORD = admin

It is advisable to change the default username to reduce the attack surface.

Additionally, the following properties can be updated:
* ``admin.contexts``: default role contexts managed by the platform. Defaults to ``authorization, components, organizations, resources, services``
* ``admin.contextSpaces``: default role spaces associated to the admin. Defaults to ``components/apimanager/carbon.super`` (used by WSO2 API Manager as a default tenant)
* ``admin.roles``: a list of roles associated to the admin
    
### 2.4. OAuth2.0 Configuration

The following properties refer to the configuration of the OAuth2.0 protocol. Specifically


| Property    | Description              | Default | ENV Variable |
| ----------- | ------------------------ | ------- |
| `oauth2.redirects.matchports` | Whether strictly match ports in redirect URLs | true | `REDIRECT_MATCH_PORTS` |
| `oauth2.redirects.matchdomains` | Whether strictly match domains in redirect URLs | true | `REDIRECT_MATCH_SUBDOMAINS` |
| `oauth2.jwt` | Enable use of JWT tokens | true | `ENABLE_JWT` |
| `oauth2.introspection.permitAll` | Allow instrospection for all tokens, not only to the "owning" clients | false | `OAUTH2_INTROSPECTION_PERMIT_ALL` |
| `oauth2.pkce.allowRefresh` | Allow refresh tokens for PKCE flow | true | `OAUTH2_PKCE_ALLOW_REFRESH` |    
| `oauth2.clientCredentials.allowRefresh` | Allow refresh tokens for client credentials flow | true | `OAUTH2_CLIENTCREDENTIALS_ALLOW_REFRESH` |    
| `oauth2.resourceOwnerPassword.allowRefresh` | Allow refresh tokens for password flow | true | `OAUTH2_RESOURCEOWNERPASSWORD_ALLOW_REFRESH` |    
| `oauth2.accesstoken.validity` | Default validity (seconds) of the access token | 43200 | `ACCESS_TOKEN_VALIDITY` |    
| `oauth2.refreshtoken.validity` | Default validity (seconds) of the refresh token | 2592000 | `ACCESS_TOKEN_VALIDITY` |    
| `oauth2.authcode.validity` | Default validity (seconds) of the auth code | 2592000 | `AUTH_CODE_VALIDITY` |    


> **NOTE:**
When using JWTs as access tokens, it is advisable to reduce their validity to overcome the revocation problem, where revoked but not expired tokens are considered valid by third parties such as resource servers. Please set a low value (e.g 1 hour) to address the issue.

### 2.5. OpenID Configuration 

AAC provide an implementation of the OpenID protocol. The implementation is based on the [MitreID](https://mitreid.org/) project.
By default, the *token issuer* will be set equals to the ``application.url``. 
It is possible to separately configure the issuer via properties:

    jwt:
      issuer: http://localhost:8080/aac
      
or via ENV

     JWT_ISSUER = http://localhost:8080/aac

      
OpenID extension requires RSA keys for JWT signature. The default configuration does not possess any permanent key, and for development/demo purposes generates a new set at each startup.

In order to deploy AAC in production mode it is mandatory to provide an external key. 
To generate new key please follow the instructions available [here](https://mkjwk.org/). 

The resulting key should be placed in the resources (i.e., src/main/resources). Alternatively, it is possible to override the default keystore via configuration by setting the following property 

    security:
      keystore: file:///absolute/path/to/keystore.jwks
      
or via ENV

    JWK_KEYSTORE = file:///absolute/path/to/keystore.jwks

The keystore can contain more than one key. By providing the id of the key via ``kid`` property, we can instruct AAC to select a specific key within those available. If not provided, the software will take the first suitable key from the set for signing/validation, and disable the encryption.

Via properties:

    jwt:
      kid:
         sig: SIGNATURE_KEY_ID
         enc: ENCRYPTION_KEY_ID

or via ENV:

    JWT_KID_SIG = SIGNATURE_KEY_ID
    JWT_KID_ENC = ENCRYPTION_KEY_ID

      
The OpenID metadata is available at ``/.well-known/openid-configuration``.

The keys are available as a JSON Web Key Set (JWKS) at ``/jwk``.

### 2.5. OAuth2.0 JWT Configuration

When enabled, AAC can issue bearer tokens as *JSON Web Tokens* (JWT) containing a subset of the standard OpenID claims.


> **NOTE:** switching from opaque tokens to JWT on an existing setup could require a flush of all the tokens already issued. Otherwise, as long as refresh tokens are valid, AAC will continue to provide the same tokens to clients. Only new requests will receive JWTs. To perform the flush, simply clear the content of both the tables *oauth_access_token* and *oauth_refresh_token* from the db console.

Since JWTs should be cryptographically signed by issuers, AAC provides 2 different working modes when dealing with JWTs:

  * use a **symmetric key**, via HMAC algorithm
  * use a **public/private keypair**, via RSA algorithm

By default, AAC works with HMAC signatures for JWTs. HMAC employs a *secret key*, known only to the parties, which is used for both the signature and the validation. As such, HMAC-*signed* tokens can ensure the *integrity* of the content, but not the identity of the issuer: any party knowing the secret key can forge them. A viable solution to mitigate such risk is to restrict the usage of keys, by providing each client with a unique secret instead of using a globally-shared key known to each and every client. Given that clients already possess a ```client_secret``` for authenticated flows, AAC can use this unique string to sign tokens. This way, only legitimate clients, already in possess of the required secret, will be able to verify the JWT signature, while others should reject the token.

If issuer verification is needed, AAC can alternatively sign tokens via RSA with a public/private keypair. By sharing only the public key, clients can independently verify that the *issuer* possesses the correct private key and thus protect themselves from tokens forged from other clients.

By default, AAC employs RSA keys for signature, by leveraging the same keyset used for OpenID.
It is possible to configure *per-client* the algorithm and scheme used, and thus switch single clients to HMAC-mode with ``client_secret`` or to RSA with a dedicated keypair.


At the moment, AAC does not support *encryption* for JWTs. Given the lack of confidentiality when dealing with non-encrypted JWTs (which in essence are just base64-encoded json), users should double check information included in claims, to avoid leaking private or sensible data over insecure channels.


### 2.6. Mail Configuration

For the user registration and the corresponding communications (email verification, password reset, etc), the 
SMTP email server should be configured:

    mail:
      username: info@example.com
      password: somepassword
      host: smtp.example.com
      port: 465
      protocol: smtps 

or via ENV

    MAIL_USER = info@example.com
    MAIL_PASS = somepassword
    MAIL_HOST = smtp.example.com
    MAIL_PORT = 465
    MAIL_PROTOCOL = smtps

Without a valid mail configuration AAC won't be able to send any email to users.

 
### 2.6. Configuring Identity Providers

AAC allows for configuring different types of Identity Providers, such as internal (email/password-based login), OpenID Connect providers, or SAML providers. It is possible to define some specific properties and models for the providers.

#### Internal Provider Configuration

The following properties can be pre-configured for the  Internal provider configuration:

| Property    | Description              | Default |
| ----------- | ------------------------ | ------- |
| `authorities.internal.confirmation.required` | Require confirmation for the registrered users | true | 
| `authorities.internal.confirmation.validity` | Confirmation link validity (seconds) | 86400 | 
| `authorities.internal.password.minLength` | User password min length | 8 | 
| `authorities.internal.password.maxLength` | User password max length | 20 | 
| `authorities.internal.password.requireAlpha` | User password requires letters | true | 
| `authorities.internal.password.requireNumber` | User password requires numbers | false | 
| `authorities.internal.password.requireSpecial` | User password requires special symbols | false | 
| `authorities.internal.password.supportWhitespace` | User password supports whitespaces | false | 
| `authorities.internal.password.reset.enabled` | User password reset enabled | true | 
| `authorities.internal.password.reset.enabled` | User password reset key validity (seconds) | 86400 | 

#### Configuring Identity Provider templates

It is possible to prec-configure properties for the 
different Identity Providers. Specifically, the configuration by default allows for the Google, Facebook, and GitHub clients.

#### SPID Configuration properties

It is possible to configure the list of SPID providers
through `spid.idps.XXX` properties.


### 2.7. Logging Configuration 

By default, AAC will log all the messages with a priority level equals or major than *INFO* to STDOUT. 
The logging settings may be configured via properties:

    logging:
      level:
         it.smartcommunitylab.aac: INFO

or via ENV

    LOG_LEVEL = INFO


The project relies on the Logback configuration (see ``src/main/resources/logback.xml``). In order to persist the logs to file, the default configuration requires the log folder path defined with ``aac.log.folder`` property. If the property is not set, the application will use default value: `WORKING_DIRECTORY/logs`.

### 2.8. Bootstrapping default configuration 

if necessary, it is possible to preload a set of configurations (clients, realms, etc) at the moment of the
system bootsrap. The configurations should be placed in the ``bootstrap.file`` property (defaults to ``classpath:/bootstrap.yaml``) or to ``BOOTSTRAP`` environment variable. To disable bootstrap load use ``bootstrap.apply`` property or ``BOOTSTRAP_APPLY`` environment variable. 

## 3. Execution

To execute from command line, use maven Spring Boot task:

    mvn -Plocal spring-boot:run
    
Once started, the AAC tool UI is available at ``http://localhost:8080/``.


