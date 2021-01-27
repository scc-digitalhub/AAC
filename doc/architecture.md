# AAC architecture

TODO


```mermaid
graph TD
    AAC --> IDP
    IDP --> OAUTH[OAuth2 AS]
    IDP --> OPENID[OpenID Connect]

    AAC --> CLIENT
    AAC --> USER
    AAC --> SERVICES

    AAC --> APIKEY

```