[![Build Status](https://travis-ci.org/scc-digitalhub/AAC.svg?branch=master)](https://travis-ci.org/scc-digitalhub/AAC)

# AAC
Authentication and Authorization Control Module.

This module exposes the OAuth2.0 protocol functionality for user authentication and
for the authorization of resource access (e.g., APIs). The module allows for managing 
user registration and for authenticating user using social accounts, in particular Google+ and Facebook. 

## 1. Installation Requirements
While AAC is distributed as a self contained jar, for production deployments the installation requirements are:

* RDBMS (tested on MySQL 5.5+)
* Java 1.8+
* MongoDB 3.4+ (optional, needed for Authorization Module)
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

> **IMPORTANT:** The default configuration already contains MySQL driver version 5.1.40. In case a newer version of the DB is used, update the driver version in ``pom.xml`` accordingly.



### 2.2. AAC Endpoint Configuration

AAC exposes an HTTP endpoint which can be configured via the following properties:

    server:
      host: localhost
      port: 8080
      contextPath: /aac

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
* ``admin.contexts``: default role contexts managed by the platform. Defaults to ``apimanager, authorization, components``
* ``admin.contextSpaces``: default role spaces associated to the admin. Defaults to ``apimanager/carbon.super`` (used by WSO2 API Manager as a default tenant)
* ``admin.roles``: a list of roles associated to the admin

#### RememberME

The *remember me* functionality needs a proper security key to be able to sign the cookie.
Generate a *secure random* string and then configure 

    security.rememberme.key: REMEMBER_ME_SECRET_KEY

or via ENV
    
    REMEMBER_ME_KEY = REMEMBER_ME_SECRET_KEY
    
#### Token Validity

OAuth2 *access token* and *refresh tokens* are generated with a predefined validity, which determines their expiration date. 
Expired tokens are considered invalid.

It is possible to adjust the default validity (in seconds) via the settings

    security:
      accesstoken:
         validity: 43200
      refreshtoken:
         validity: 2592000 

or via ENV

    ACCESS_TOKEN_VALIDITY = 43200
    REFRESH_TOKEN_VALIDITY = 2592000 

The default configuration generates *access tokens* with a validity of 12 hours, while *refresh tokens* have a validity of 30 days. 

> **NOTE:**
When using JWTs as access tokens, it is advisable to reduce their validity to overcome the revocation problem, where revoked but not expired tokens are considered valid by third parties such as resource servers. Please set a low value (e.g 1 hour) to address the issue.

#### Redirect policy

OAuth2 clients should provide all the callback URLs within the dedicated configuration section inside AAC.
By default, the system will check the validity of such URLs and then match all the requests to the provided configuration in a strict way. When the URL passed as callback does not *exactly* match the one provided in the configuration, AAC will refuse to release an access token.

It is possible to relax this policy by enabling the following settings:
* ``match_ports``: when set to false permits the redirect towards any port of the host
* ``match_subdomains``: when set to false permits the redirect towards any subdomain

via configuration properties

    security:
      redirects:
      matchports: true
      matchsubdomains: true 

or via ENV

    REDIRECT_MATCH_PORTS = true
    REDIRECT_MATCH_SUBDOMAINS = true



#### Default admin client

Optionally, if needed, it is possible to configure the default OAuth2.0  client application associated with
the admin user. The following properties may be customized:

- ``id``: id of the client app
- ``secret``: secret key for the client
- ``scopes``: default scopes to be associated with the client
- ``redirects``: default list of redirect URIs associated with the client.

via configuration properties:

    adminClient:
       id: API_MGT_CLIENT_ID
       secret: YOUR_MNGMT_CLIENT_SECRET
       scopes: SCOPES_LIST
       redirects: REDIRECTS_LIST

or via ENV

    APIM_ID = API_MGT_CLIENT_ID
    APIM_SECRET = YOUR_MNGMT_CLIENT_SECRET
    APIM_SCOPES = SCOPES_LIST
    APIM_REDIRECTS = REDIRECTS_LIST
      
By setting an empty configuration the default admin client won't be created.

### 2.4. Mail Configuration

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

 
### 2.5. Configuring Identity Providers

By default, AAC relies on the internally registered users. 
However, the integration with the external identity provider is supported.
Out of the box, AAC comes with the following providers pre-configured:

* Facebook
* Google 
* ADC

In order to activate any of those it is required to provide the correct client parameters.
After providing the required settings, enable the external identity providers by updating the list of active authorities via configuration:

    authorities:
      enabled: internal,google,facebook,adc

or via ENV
   
    AUTHORITIES = internal,google,facebook,adc

> **NOTE:**
AAC requires the *internal* authority enabled for admin access.

#### Facebook
First register as a developer and create a client application via Facebook.
Then add to application.yml the following properties:
 
    oauth-providers:
      providers:
        - provider: facebook
          client:
            clientId: YOUR_FACEBOOK_CLIENT_ID
            clientSecret: YOUR_FACEBOOK_CLIENT_SECRET

or via ENV
        
    OAUTH_FACEBOOK_CLIENTID = YOUR_FACEBOOK_CLIENT_ID
    OAUTH_FACEBOOK_CLIENTSECRET = YOUR_FACEBOOK_CLIENT_SECRET

#### Google
First register as a developer and create a client application via Google.
Then add to application.yml the following properties:

    oauth-providers:
      providers:
       - provider: google
         client:
           clientId: YOUR_GOOGLE_CLIENT_ID
           clientSecret: YOUR_GOOGLE_CLIENT_SECRET

or via ENV

    OAUTH_GOOGLE_CLIENTID = YOUR_GOOGLE_CLIENT_ID
    OAUTH_GOOGLE_CLIENTSECRET = YOUR_GOOGLE_CLIENT_SECRET
           
#### Custom configuration           
The support for compatible OAuth2.0 providers can be achieved through the configuration files.
 Specifically, to configure an OAuth2.0 identity provider it is necessary to specify the OAuth2.0 client configuration in the properties, e.g. for Facebook: 

    oauth-providers:
      providers:
        - provider: facebook
          client:
            clientId: YOUR_FACEBOOK_CLIENT_ID
            clientSecret: YOUR_FACEBOOK_CLIENT_SECRET
            accessTokenUri: https://graph.facebook.com/oauth/access_token
            userAuthorizationUri: https://www.facebook.com/dialog/oauth
            preEstablishedRedirectUri: ${application.url}/auth/facebook-oauth/callback
            useCurrentUri: false
            tokenName: oauth_token
            authenticationScheme: query
            clientAuthenticationScheme: form
            scope:
                - openid
                - email
                - profile    

Besides, it is necessary to describe the provider and the possible identity mappings in
``src/main/resources/it/smartcommunitylab/aac/manager/authorities.xml`` file. The configuration amounts to
defining the properties to extract, mapping name and surname properties, defining the unique identity attribute
for the provider, and relations to the other providers' attributes (e.g., via email). For example,

    <authorityMapping name="google" url="google" public="true" useParams="true">
        <attributes alias="it.smartcommunitylab.aac.givenname">given_name</attributes>
        <attributes alias="it.smartcommunitylab.aac.surname">family_name</attributes>
        <attributes alias="it.smartcommunitylab.aac.username">email</attributes>
        <attributes>name</attributes>
        <attributes>link</attributes>
        <identifyingAttributes>email</identifyingAttributes>
    </authorityMapping>

defines the Google authentication attributes. Specifically, the ``email`` attribute is used to uniquely identify
Google users.  

Furthermore, any additional authority added to the mapping should be enabled via the setting ``authorities.enabled``.

### 2.6. Authorization Module

AAC allows for integration of the authorization module, where it is possible to configure the access rights for
the user at the level of data. To enable the authorization module, the corresponding ``authorization`` Maven profile
should be activated. 
 
### 2.7. Logging Configuration 

By default, AAC will log all the messages with a priority level equals or major than *INFO* to STDOUT. 
The logging settings may be configured via properties:

    logging:
      level:
         it.smartcommunitylab.aac: INFO

or via ENV

    LOG_LEVEL = INFO


The project relies on the Logback configuration (see ``src/main/resources/logback.xml``). In order to persist the logs to file, the default configuration requires the log folder path defined with ``aac.log.folder`` property. If the property is not set, the application will use default value: `WORKING_DIRECTORY/logs`.
 
### 2.8. OpenID Configuration 

AAC provide a basic implementation of the OpenID protocol. The implementation is based on the [MitreID](https://mitreid.org/) project.
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


### 2.9 OAuth2 JWT Configuration

By default, AAC provides oauth ``access_tokens`` and ``refresh_tokens`` as *opaque strings*. If needed, AAC can issue bearer tokens as *JSON Web Tokens* (JWT) containing a subset of the standard OpenID claims.

To globally enable JWT mode set the following property to ```true```:

    oauth2:
      jwt: true

or via ENV
   
    ENABLE_JWT = true


> **NOTE:** switching from opaque tokens to JWT on an existing setup could require a flush of all the tokens already issued. Otherwise, as long as refresh tokens are valid, AAC will continue to provide the same tokens to clients. Only new requests will receive JWTs. To perform the flush, simply clear the content of both the tables *oauth_access_token* and *oauth_refresh_token* from the db console.

Since JWTs should be cryptographically signed by issuers, AAC provides 2 different working modes when dealing with JWTs:

  * use a **symmetric key**, via HMAC algorithm
  * use a **public/private keypair**, via RSA algorithm

By default, AAC works with HMAC signatures for JWTs. HMAC employs a *secret key*, known only to the parties, which is used for both the signature and the validation. As such, HMAC-*signed* tokens can ensure the *integrity* of the content, but not the identity of the issuer: any party knowing the secret key can forge them. A viable solution to mitigate such risk is to restrict the usage of keys, by providing each client with a unique secret instead of using a globally-shared key known to each and every client. Given that clients already possess a ```client_secret``` for authenticated flows, AAC can use this unique string to sign tokens. This way, only legitimate clients, already in possess of the required secret, will be able to verify the JWT signature, while others should reject the token.

If issuer verification is needed, AAC can alternatively sign tokens via RSA with a public/private keypair. By sharing only the public key, clients can independently verify that the *issuer* possesses the correct private key and thus protect themselves from tokens forged from other clients.

By default, AAC employs RSA keys for signature, by leveraging the same keyset used for OpenID.
It is possible to configure *per-client* the algorithm and scheme used, and thus switch single clients to HMAC-mode with ``client_secret`` or to RSA with a dedicated keypair.


At the moment, AAC does not support *encryption* for JWTs. Given the lack of confidentiality when dealing with non-encrypted JWTs (which in essence are just base64-encoded json), users should double check information included in claims, to avoid leaking private or sensible data over insecure channels.

> **IMPORTANT**: Do not put secret information in the claims of a JWT unless it is encrypted.

## 3. Execution

To execute from command line, use maven Spring Boot task:

    mvn -Plocal spring-boot:run
    
In case you run the tool from the IDE, add the profile configuration to the VM parameters:

    -Dspring.profiles.active=local

To enable the authorization module, add the corresponding profile to the profile list (comma-separated)

Once started, the AAC tool UI is available at ``http://localhost:8080/aac``.


## 4. Usage Scenarios

### 4.1. Using AAC for Authentication

This is a scenario where AAC is used as an Idenity Provider in a federated authentication environment. 

To enable this scenario, AAC exposes OAuth2.0 protocol. Specifically, it is possible to use *OAuth2.0 Implicit Flow* as follows.


**4.1.1. Register Client App** 

Create Client App with AAC developer console (*/aac/*).
To do this
- Login with authorized account (see access configuration above);
- Click *New App* and specify the corresponding client name
- In the *Settings* tab check *Server-side* and *Browser access* and select the identity providers to be used 
 for user authentication (e.g., google, internal, or facebook). Specify also a list of allowed redirect addresses (comma separated).
- In the *Permissions* tab select *Basic profile service* and check *profile.basicprofile.me* and 
 *profile.accountprofile.me* scopes. These scopes are necessary to obtain the information of the currently signed 
 user using AAC API.
 
Alternatively, if API Manager is engaged, create a new API Manager application and subscribe the AAC API. 
         
**4.1.2. Activate Implicit Flow Authorization**

This flow is suitable for the scenarios, where the client application (e.g., client part of a Web app) makes the authentication and then direct access to the API without passing through its own Web server backend. This allows for generating only a token for a short time period, so the next time the API access is required, the authentication should be performed again. In a nutshell, the flow is realized as follows:    

- The client app, when there is a need for the token, emits an authorization request to AAC in a browser window.
  <code>https://myaac.instance.com/aac/eauth/authorize</code>.
  
  The request accepts the following set of parameters:
     - *client_id*: the client_id obtained in developer console Indicates the client that is making the request. 
       The value passed in this parameter must exactly match the value in the console.
     - *response_type* with value *token*,  which determines if the OAuth 2.0 endpoint returns a token.
     - *redirect_uri*: URL to which the AAC will redirect upon user authentication and authorization. 
       The value of this parameter must exactly match one of the values registered in the APIs Console 
       (including the http or https schemes, case , and trailing ‘/’).
     - *scope*: space-delimited set of permissions the application requests Indicates the access your 
        application is requesting. 

- AAC redirects the user to the authentication page, where the user selects one of the identity providers and performs
  the sign in.
- Once authenticated, AAC asks the user whether the permissions for the requested operations may be granted.
- If the user accepts, the browser is redirected to the specified redirect URL, attaching the token data in the 
url hash part:  
  <code>http://www.example.com#access\_token=025a90d4-d4dd-4d90-8354-779415c0c6d8&token\_type=Bearer&expires\_in=3600</code>.

- Use the obtained token to obtain user data using the AAC API:


      GET /aac/basicprofile/me HTTPS/1.1 
      Host: aacserver.com 
      Accept: application/json 
      Authorization: Bearer 025a90d4-d4dd-4d90-8354-779415c0c6d8


  The result of the invocation describes basic user properties (e.g., userId) that can be used to uniquely identify the 
  user.
  
  
### 4.2. Using AAC for Securing Resources and Services

In this scenario the goal is to restrict access to the protected resources (e.g., an API endpoint). Also in this case
the scenario relies on the use of OAuth2 protocol. The two cases are considered here:

- The protected resources deal with user-related data or operation. In this case (according to OAuth2.0), the access to the API should be accompanied with the *Authorization* header that contains the access token obtained via Implicit Flow (or via Authorization Code Flow).

- The protected resource does not deal with user-related data or operation and is not performed client-side. In this case, the access to the API should also be accompanied with the *Authorization* header that contains the access token obtained via OAuth2.0 client credentials flow.
  
The protected resource will use the OAuth2.0 token and dedicated AAC endpoints to ensure that the token is valid and (in case of user-related data) to verify user identity.  If the token is not provided or it is not valid, the protected resource should return 401 (Unauthorized) error to the caller. 

**4.2.1. Generating Client Credentials Flow Token**

In case the access to the non user-resource is performed, it is possible to use access token obtained through
OAuth2 client credentials flow. In this flow, the resulting token is associated to an client application only. 

The simplest way to obtain such token is through the AAC development console: on the *Overview* page of the client app use the *Get client credentials flow token* link to generate the access token. Note that the token is not expiring and therefore may be reused.

Alternatively, the token may be obtained through the AAC OAuth2.0 token endpoint call:

    POST /aac/oauth/token HTTPS/1.1
    Host: aacserver.com 
    Accept: application/json 
    client_id=23123121sdsdfasdf3242&
    client_secret=3rwrwsdgs4sergfdsgfsaf&
    grant_type=client_credentials
    
The following parameters should be passed:

- *grant_type*: value *client_credentials*
- *client_id*: client app ID
- *client_secret*: client secret

A successful response is returned as a JSON object, similar to the following:

    "access_token": "025a90d4-d4dd-4d90-8354-779415c0c6d8",
    "token_type": "bearer",
    "expires_in": 38937,
    "scope": "profile.basicprofile.all"      
    
Finally, if the API Manager is used, the token may be obtained directly from the API Manager console.   
    
## 5. Data Model

### 5.1. Users
In AAC the user management may be federated with the other Identity Providers. Apart from the built-in user management,
where the user is identified by the registration email and authenticated by password, it is possible to authenticate with
external standard-based IdPs, e.g., OAuth2.0, SAML, etc. 

Each user is, therefore, associated with the account type used at the authentication. The users managed internally, have
``internal`` account type, the users authenticated with Google, have ``google`` type, etc. The mechanism for defining different
IdP depends on the specific protocol; all the supported IdPs (authentication authorities) should be declared in
authority mapping file (``it/smartcommunitylab/aac/manager/authorities.xml``), where for each IdP it is necessary to specify

- identifying attributes (used to uniquely identify the user in this IdP)  
- list of supported attributes specific to IdP. Some of the attributes may be annotated in order to extract common values (e.g., name and surname)
- mapping between the IdPs to automatically map the same user across IdPs using common attribute values (e.g., internal email and Google email).
 
Common user information is captured by the basic user profile, while account information associated with the user
is represented with the account profile data (see., Profile API). The standard ways to capture these attributes is represented
with the OpenID Connect Claims extracted from the user profile, account, and role information.  

### 5.2. Roles

In AAC the roles are contextualized in order to support various multitenancy scenarios. The general idea is 
that the roles are associated to the role *spaces* uniquely identified by their namespace and managed
by the space *owners*. A user may have different roles in different spaces and the authorization
control for individual organizations, components, and deployment may be performed within the corresponding space. 
More specifically, each role is represented as a tuple with

- *context*, defining the "parent" space of roles (if any)
- *space*, defining the specific role space value. Together with the context form the unique role namespace 
- *role*, defining the specific role value for the space.

In this way, the spaces may be hierarchically structured (e.g., departments of an organization may have their
own role space within the specific organization space).

Syntactically, the role is therefore represented in the following form: ``<context>/<space>:<role>``. To represent
the owner of the space the role therefore should have the following signature: ``<context>/<space>:ROLE_PROVIDER``.

The owner of the space may perform the following functionalities:
- associate/remove users to/from the arbitrary roles in the owned spaces (including other owners).
- create new child spaces within the owned ones. This is achieved through creation of the corresponding owner roles for the child space being created: ``<parentcontext>/<parentspace>/<childspace>:ROLE_PROVIDER``.

The operation of user role management and space management may be performed either via API or through
the AAC console. 

Some of the role spaces may be pre-configured for the AAC. These include:
- ``apimanager``: Role space for the API Manager tenants.
- ``authorization``: Role space for managing the Authorization API grant tenants.
- ``components``: Role space for managing tenants of various platform components. 

### 5.3. Services, Scopes, and Claims

The authorization and information model of the OAuth2.0 and OIDC protocols relies on the definition of claims (i.e., user
attributes) and scopes (i.e., data usage markers). Specifically, the scopes define which data / resources the client app can access and manage on behalf of the authorizing user or even directly. The set of returned claims depend on the scopes requested by the client app by default or during the authorization phase.

To capture these properties, AAC organizes the claims and scopes into information "services" or resources. Each service
defines

- set of scopes and their characteristics (e.g., association to user or app, requirements for access)
- set of claims and their characteristics (e.g., claim data type and cardinality, possibility to derive from other claims).

Each service has unique serviceId and a namespace (used in claims as prefix). For the purpose of management, each
service is associated to the role context so that only the user with that role may manage that service. That is, given the context ``mycontext`` the users with the role ``services/mycontext:ROLE_PROVIDER`` will be able to manage the services associated to that context.

Core services, scopes and claims are managed directly by AAC and configured declaratively (see ``src/main/resources/it/smartcommunitylab/aac/manager/services.json`` for details). Custom services may be created and managed by the AAC users either through the AAC developer console ("Services" section) or programmatically via API.

####5.3.1 Scopes

The scopes define may optionally declare the set of user claims they enable to access. E.g., ``openid`` scope enables
access to the standard user claims such as sub, username, etc. To control which client apps have access to certain
sensible scopes, AAC allows for two mechanisms:

- explicit platform admin approval
- user roles that should be associated to the user (end user in case of user scopes or app owner in case of client scopes).

These mechanisms may be declared through the scope definition.

All the scopes defined within AAC are then available in the client app configuration so that client app may declare 
which scopes it requires access to.   

####5.3.2. User Claims and Claim Management

AAC manages the following standard OIDC claims:

- sub (scopes ``openid``, ``profile``, ``profile.basicprofile.me``)
- username (scopes ``openid``, ``profile``, ``profile.basicprofile.me``)
- preferred_username (scopes ``openid``, ``profile``, ``profile.basicprofile.me``)
- given_name (scopes ``openid``, ``profile``, ``profile.basicprofile.me``)
- family_name (scopes ``openid``, ``profile``, ``profile.basicprofile.me``)
- email (scopes ``openid``, ``profile``, ``email``, ``profile.basicprofile.me``)

Additionally, AAC supports the following core custom claims 

- ``accounts``: represent the associated user accounts (requires ``profile.accountprofile.me`` scope).
- ``authorities``: list of the roles associated to the user (requires ``user.roles.me`` scope).
- ``groups``: list of role contexts the user has a role (requires ``user.roles.me`` scope)
- ``resource_access``: roles associated to various role contexts (requires ``user.roles.me`` scope).

Custom claims are defined with the custom services either via AAC developer console or programmatically. 

Claim values are populated in three different ways:

- core claims are managed directly by AAC (e.g., registration information about the users, roles, etc)
- custom claims associated to the users explicitly (via user claim API)
- custom claims derived from other user claims as defined by the claim mapping function at the level of the corresponding service definition.

Please note that the claim mapping function does not overwrite the explicitly stored custom claims.


#### 5.3.3. Client-level Claim Customization

When a client app requires specific custom claims to be presented, it is possible for the client app to customize the representation of the user claims (but not the predefined ones
like ``sub``, ``aud``, etc.). The mapping is defined in the client app configuration **Roles&Claims** section
as a JavaScript function that should provide the customized set of claims given the pre-defined ones.

In this way, a client app may map predefined claim values to the expected ones; reduce the number of claims or add new ones. 

#### 5.3.4. End User Roles Disambiguation

Frequently, different multitenant application does not allow the user to belong to more than one tenant. AAC, however, does not
have this restriction and the user may have roles in different contexts associated to that application. To avoid the customization of these applications to deal with the users associated to multiple tenants, AAC allows for the role
disambiguation during the authorization step. That is, after authenticating the user, AAC will ask (together with the request for the scope approval) the user to select a single tenant for the required role context. 

To configure this behavior, the client app should list the contexts, for which the user should be asked to select (**Roles&Claims** configuration of the client app). For example, if the client app is associated with the context ``components/grafana``, the context should be specified in the **Unique Role Spaces** list. During the authorization request, if the user is associated with more than one space within that context, he/she will be asked to select a single value to proceed.
    
#### 5.3.5. Authentication Flow hooks

The developers may configure the client apps to emit notifications about relevant events
before the actual authorization flow completes. This is a useful scenario in case of custom OAuth2.0 / OpenID Connect
integrations, where some specific actions should, e.g., be performed **before** the actual code / token is received. 
An example is when the external app should prepare some user-related data (e.g., data spaces / tenants) before the user
is authenticated in that external app.

The notifications have the form of Web hooks configured for the client application. The expected protocol is

- the client app defines a Web hook URL
- AAC performs a **synchronous** GET call to the specified URL passing the JWT user claims matching the configured client scopes

Currently, the only hook event supported is triggered immediately **after** the user approval (if applied) in the
authorization flow (authorization code flow or implicit) **before** the actual token / code is returned to the client app.     
    
## 6. AAC API
  
The Swagger UI for the AAC API is available at ``http://localhost:8080/aac/swagger-ui.html``.   
  
### 6.1. Profile API  
To obtain the basic user data the following call should be performed (scope ``profile.basicprofile.me``):   

    GET /aac/basicprofile/me HTTPS/1.1 
    Host: aacserver.com 
    Accept: application/json 
    Authorization: Bearer <token-value>  
  
If the token is valid, this returns the user data:

    {
    "name": "Mario",
    "surname": "Rossi",
    "userId": "6789",
    "username": "mario@gmail.com"
    }  

To obtain the account-related data (e.g., the Identity Provider-specific attributes),  the following call should be performed  (scope ``profile.accountprofile.me``):   

    GET /aac/accountprofile/me HTTPS/1.1 
    Host: aacserver.com 
    Accept: application/json 
    Authorization: Bearer <token-value>  
  
If the token is valid, this returns the user data, e.g.,:

    {
      "name": "Mario",
      "surname": "Rossi",
      "username": "rossi@gmail.com",
      "userId": "1",
      "accounts": {
        "google": {
          "it.smartcommunitylab.aac.surname": "Rossi",
          "it.smartcommunitylab.aac.username": "rossi@gmail.com",
          "it.smartcommunitylab.aac.givenname": "Mario",
          "email": "rossi@gmail.com"
        }
      }
    }

### 6.2. Token API

To validate the token, i.e., to check the token is not expired and is associated to proper scopes AAC supports
Token introspection API (ITEF RFC 7662). The call should be provided with the Client Credentials (e.g., as BasicAuth, with ``client_id:client_secret``):

    GET /aac/token_introspection HTTPS/1.1 
    Host: aacserver.com 
    Accept: application/json 
    Authorization: Basic <client_id:client_secret>  
    token=<token_value_to_introspect>

The request returns a JSON token with standard claims regarding the user, the client, and the token. The returned claims
represents the standard token claims (see ITEF RFC 7662 for details) as well as custom AAC claims (prefixed with ``aac_``):

    {
        "active": true,
        "scope": "openid user.roles.me",
        "client_id": "9958cd52-2af0-440a-980e-360967101507",
        "username": "admin",
        "token_type": "Bearer",
        "sub": "1",
        "iss": "http://localhost:8080/aac",
        "aud": "9958cd52-2af0-440a-980e-360967101507",
        "exp": 1572290840,
        "iat": 1572249670,
        "nbf": 1572249670,
        "aac_user_id": "1",
        "aac_grantType": "implicit",
        "aac_applicationToken": false,
        "aac_am_tenant": "carbon.super"
    }

### 6.3. Role API

The role API allows for the checking the role of the specific users. More details can be found on the
Swagger documentation.

### 6.4. OpenID API

AAC provides support for some of the OpenID Connect functionalities. The OIDC metadata information is available at
the  ``/.well-known/openid-configuration`` endpoint, where the supported features and relevant endpoints are captured.


In particular, the OpenID userinfo endpoint allows for getting the standard user info claims (scopes ``profile``, ``email``). The response is provided
in the form of JSON object or JWT token.

    GET /aac/userinfo HTTPS/1.1 
    Host: aacserver.com 
    Accept: application/json 
    Authorization: Bearer <token-value>  

The data provided represents the subset of standard OpenID claims.

      {
        "sub": "123456789",
        "name": "Mario Rossi",
        "preferred_username": "rossi@mario.com",
        "given_name": "Mario",
        "family_name": "Rossi",
        "email": "rossi@mario.com",
      }

### 6.5. Service, Scope, Claim API

The service API allows one to define programmatically custom service definition, associated scopes, and claims. Through the API it is possible to 

- define, update, or delete a service (id, name, description, mapping, namespace, role context)
- add / remove scopes (scope, name, description, client/user scope type, claims, access requirements)
- add / remove claims (claim, name, type, cardinality) 

More details can be found on the Swagger documentation.


### 6.6. Custom User Claim API

The custom user claim API allows for associating / changing custom user claims for a specific service and user. More specifically, it is possible to

- list users with claims defined by a specific service
- list claims associated to a specific user and a service
- modify claim values for a specific user and a service

Please note that it is possible to manage the claims for already registered users (using user ID) or to
the user that eventually will be registered (using email as username).

More details can be found on the Swagger documentation.


## 7. Docker image build 

1 - Download java and maven.<br>
2 - Copy jdk1.8.0 and apache-maven folder to dockerfiles.<br>
3 - Run this command:<br>
  docker build -t aac:latest .
