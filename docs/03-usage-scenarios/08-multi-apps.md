# Using Multiple Client Apps with the Same API

In some situations the same API may be used by several clients. For example, the same backend may have different front-end applications, both Web and/or mobile ones. These front-ends may be designed to satisfy different requirements, such as

* use different login flows / providers. A backoffice front-end may allow for registered users only, while client front-end may allow for social login as well.
* use different user data (and therefore different user attributes or data access scopes).

In such settings, it is necessary to use different AAC client apps with different configurations.

## Implementation 

### 1. Identity Provider Configuration

In the scenario where the different front-ends use different login channels (or sets of login providers), it is necessary to configure all these providers in the realm. It is even possible to register several providers of the same type (e.g., if both the mobile and Web app use Google authentication, distinct Google configurations may be required) or have different provider types. 

Once configured, the different clients shall have their specific set o Identity Providers assigned.

### 2. Custom Service Configuration

To allow multiple clients use the same API, it is necessary for the API to validate the tokens for all those clients. In case of *opaque* token validation, the AAC introspection API evaluates the token audience against the provided client ID. In case of multiple clients, the audience therefore should contain a common id that corresponds to the API. In AAC this can be achieved by defining a *custom service*, which logically represents the API to be protected and validated. Furthermore, the custom services allow to restrict the access to the API only to the authorized applications using rich set of policies that can be defined. See [here](../02-quick-start/01-base/04-custom-service.md) for the detailed information about how to set up and use the custom services of AAC.

### 3. User Login and API Calls

Once the configuration is performed, the corresponding front-end application should use appropriate libraries / SDK for the [login flow implementation](./01-add-login-to-app.md). The same applies to the [server implementation](./05-call-your-api.md), where the token validation should engage the *custom service client* identity and credentials to perform the validation and to check the audience against the custom service ID.


### 4. Single Sign-On and Single Logout (SSO / SLO)

TODO