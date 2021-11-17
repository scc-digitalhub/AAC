# Developers documentation

This doc covers the usage of AAC for developing third party applications which leverage AAC for authentication and authorization of both users and clients.

# Basic concepts

* identity management / authentication / authorization
* realm
* user and identities
* client applications
* identity providers
* attribute providers
* roles and authorities
* custom services (API)


# Quick start

base
* realm
* identity provider
* client application
* custom services (API) and scopes

advanced
* user identities
* attributes sets and providers
* roles and authorities
* session management
* audit
    

expert
* custom claims
* custom attributes mapping
* webhooks
* realm customization
  

# Usage scenarios

# Web (OIDC)

* external idp
* client 
* id_token/userinfo

# Native app

* internal idp
* client
* id_token/userinfo


# Machine-to-machine

* clients
  
# Multiple web apps (OIDC)

* idp
* clients
* single login + single logout

# Authorization

* user attributes
* client custom mapping
* roles
* roleSpaces
  


# Topics

## Identity providers

* internal
* oidc
* saml
* SPID
* (CIE)
* (google)
* (facebook)
* (azure)

## Attribute providers

* attribute sets
* mappers
* webhook
* scripts
  
## Client applications

* oauth2

## Custom services

* api
* scopes
* claims

## Users

* user management
* authorities
  
## Roles

* permissions

## Audit

* events


# Standards

* OAuth2
* OpenID Connect
* SAML
* JWT
* SPID + CIE
* EIDAS