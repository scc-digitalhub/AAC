# Add Login to Native/Mobile Application

In case of mobile app, to "outsource" the login functionality to AAC, it is necessary for the app to open a browser (SafariViewController on iOS or Custome Chrome Tab on Android) to perform the login operation. As in case of Single Page Applications, the recommended way to perform the authentication is to use OAuth2.0 **Authorization Code Flow with PKCE**. 

## Overview

In the native app:

1. The user access the login (button or link) and  the app redirects to the AAC Authorization Server in the opened browser.
2. The user is presented the login page with one or more Identity Providers configured for you app (e.g., username/password login, external login with Google, etc.).
3. The user authenticates and authorizes your app for the information access and your app is granted with the possibility to obtain the token.
4. AAC redirects to the registered redirect URL that is redirected by the browser to your app. The app requests and obtains the token necessary to retrieve relevant user info (e.g., ID token).

## Implementation

### 1. Configure Client Application

To enable your application to perform login with AAC, it is necessary to register your application as a Client App in the AAC management console of your [realm](../01-basic-concepts/02-realm.md). The Client application in AAC represents your app for the purpose of authentication; it defines the way the application is identified, presented to the user, and specifies which information it will request about the user (such as, e.g., user email, profile info, etc). 

To configure client application, please follow the instructions presented [here](../02-quick-start/01-base/03-client.md). Together with the app type (Native in this case), it is necessary to provide the app identity information (e.g., app name), and the configuration properties. In particular, it is necessary to enable ``authorization code`` flow in the OAuth2 settings of the app, define a valid redirect URL to use for the token extraction, and select the relevant user information scopes in the API Access settings. Specifically, it is necessary to add the ``openid`` scope and optionally some other scopes, such as Open ID ``profile``, ``email``, etc.

### 2. Configure Identity Providers
  
In AAC it is possible to enable different Identity Provider for user to login. This includes social login with Facebook, Google, or GitHub, SAML-based Identity Providers, or pre-configured providers like SPID. In order to associate different login channels for your application, it is necessary to

* configure the Identity Providers in your [realm](../01-basic-concepts/02-realm.md). See [here](../02-quick-start/01-base/02-idp.md) for detailed explanation on how to perform Identity Provider configuration.  
* enable the required Identity Providers in the configuration of your Client Application. See [here](../02-quick-start/01-base/03-client.md) for the detailed explanation about the Client configuration.

### 3. Use OAuth2.0/OpenID Connect SDK 

To complete the implementation, you may use an arbitrary OAuth2.0/OpenID Connect client SDK for the framework/OS you use. For example it is possible to use [AppAuth](https://appauth.io/) native libraries or rely on JS-based porting in case of cross-platform frameworks as Ionic. 

To perform the configuration you will need the following information:

* Issuer URI. Can be found under the endpoints of your Client app configuration (your app / view / endpoints / Issuer metadata (OIDC));
* Callback URL. Corresponds to the URL of your application as of integration. Should be registered in the Client App configuration in AAC under OAuth2 / Redirect URIs;
* Logout URL (optional) to return to your app landing page after the user performs the logout from AAC. Should be registered in the same way as the callback URL;
* Client Id as provided by AAC.

See also: [AppAuth](https://appauth.io/).


