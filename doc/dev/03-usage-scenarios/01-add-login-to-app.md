# Add Login to your App

This scenario targets the possibility to use AAC for the user authentication within an app. 
Using AAC in your apps means that the app wiil delegate the authentication process to a centralized login page similar to Gmail or YouTube redirects to accounts.google.com for the user to signin.
The user will authenticate and AAC will generate a token that will be passed back to your application.

The scenario relies on the OAuth2.0 / OpenID Connect protocol and may be enabled in the following
situations: 

* [Regular Web Application](./01-add-login-to-app-web.md)
* [Single Page Application](./02-add-login-to-app-spa.md)
* [Native / mobile Apps](./03-add-login-to-app-native.md) 