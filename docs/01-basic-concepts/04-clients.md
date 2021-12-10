# Clients

Clients are applications outside of AAC which need to consume identities to perform actions such as calling backend APIs or retrieving user information.

AAC supports clients via the adoption of **OAuth2** and **OpenId Connect**. Future versions will support additional frameworks such as *SAML2*.

## Basic client

All clients are modelled as actors in possess of a specific, unique, *subject* and the ability to perform some kind of authentication to obtain a valid security context. 
In order to consume *tokens*, which carry information about users or the client itself, clients need to define an access model, which dictates the way they authenticate, and which kind of information they need to receive, identified by *scopes*.
AAC offers a complex and complete way of managing clients and lets developers customize the interaction and responses in many way, in order to tailor the integration to the specifics of a given scenario. 

## OAuth2

OAuth2 defines a way for clients to ask users for an authorization delegation, expresses via an *access token*, which can be used to call a backend service (API/ resource server). The rationale is letting users control the way clients access resources they own on their behalf, without disclosing credentials or full privileges.

OAuth2 clients can perform *token requests* via the OAuth2 authorization framework, by instantiating one of the many flows available such as:

* authorization code w/secret
* authorization code w/PKCE
* implicit flow
* resource owner grant
* client credentials

Furthermore, they will be able to refresh expiring tokens by leveraging the *refresh token grant*, or inspect the token content by performing *introspection*, and ask for token *revocation* when needed.

AAC aims to fully support and comply to the following OAuth2 standards:

* TODO LIST


## OpenID Connect

While OAuth2 aims at letting clients act *on behalf of users* when communicating with resource servers, **OpenID Connect** offers both clients and services the ability to obtain verifiable information about *authenticated users*, without requiring actors to actually perform any kind of authentication.

By letting users authenticate with the *identity provider* (i.e. AAC) and then receiving in response a verifiable *identity token* (id token), clients can discover information about both the user identity (or identities) and the authentication result.

AAC supports the following OpenID standards:

* TODO list
* 