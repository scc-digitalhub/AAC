---
adr_id: "0002"
title: OAuth2TokenGrantAuditFiltering
status: open
---

## <a name="question"></a> Question

How should the OAuth2EventListener filter user details in OAUTH2_TOKEN_GRANT audit logs to prevent log bloat and reduce sensitive data exposure?

## <a name="options"></a> Options

1. <a name="option-1"></a> Approach 1: Static 4-tier filtering hierarchy (FULL, DETAILS, MINIMAL, NONE) with dedicated `filterUserDetails` method.
   * **FULL:** Log everything natively (retains full arrays, attributeSets, and deep identities).
   * **DETAILS:** Exclude heavy raw custom attribute sets (`attributeSets`) and identity-level attributes, but keep core account parameters (email, name, surname, status).
   * **MINIMAL:** Drop core account parameters as well, preserving only basic principal profile information.
   * **NONE:** Strips the object completely, reducing output to only 5 top-level root fields (`subjectId`, `realm`, `username`, `enabled`, `locked`).

2. <a name="option-2"></a> Approach 2: Incremental audit levels without NONE (MINIMAL containing basic client/user identity; DETAILS adding webAuthenticationDetails and SAML identity attributes; FULL retaining all payload details).

   * **MINIMAL Payload Example:**
     ```json
     {
       "timestamp": 1784278576.554,
       "principal": "u_8c58dc439da34ea1b7245d93c82bd60b",
       "type": "OAUTH2_TOKEN_GRANT",
       "data": {
         "grant_type": "authorization_code",
         "issued_tokens": [
           "access_token",
           "refresh_token",
           "id_token"
         ],
         "client": {
           "clientId": "c_8ab8ff3f10544f0891ef92f81d4572b9",
           "realm": "claude",
           "clientName": "Test User"
         },
         "user": {
           "subjectId": "u_8c58dc439da34ea1b7245d93c82bd60b",
           "realm": "claude",
           "username": "TINIT-LVLDAA85T50G702B"
         },
         "type": "user",
         "scope": [
           "openid",
           "profile",
           "offline_access"
         ],
         "jti": "a2R1hwx1svv6tYYyryNcwsxjLdE",
         "realm": "claude",
         "expiration": 1784278586501,
         "issuedAt": 1784278576501,
         "audience": "c_8ab8ff3f10544f0891ef92f81d4572b9",
         "authorizedParty": "c_8ab8ff3f10544f0891ef92f81d4572b9"
       },
       "id": "2026-07-17T08:56:16.554Z"
     }
     ```

   * **DETAILS Payload Example:**
     ```json
     {
       "timestamp": 1784278576.554,
       "principal": "u_8c58dc439da34ea1b7245d93c82bd60b",
       "type": "OAUTH2_TOKEN_GRANT",
       "data": {
         "grant_type": "authorization_code",
         "issued_tokens": [
           "access_token",
           "refresh_token",
           "id_token"
         ],
         "webAuthenticationDetails": {
           "remoteAddress": "0:0:0:0:0:0:0:1",
           "timestamp": 1784278576485,
           "scheme": "http",
           "protocol": "HTTP/1.1",
           "locale": "it_IT",
           "userAgent": "PostmanRuntime/7.51.1",
           "language": "it"
         },
         "client": {
           "clientId": "c_8ab8ff3f10544f0891ef92f81d4572b9",
           "realm": "claude",
           "clientName": "Test User"
         },
         "user": {
           "subjectId": "u_8c58dc439da34ea1b7245d93c82bd60b",
           "realm": "claude",
           "username": "TINIT-LVLDAA85T50G702B",
           "details": {
             "identities": [
               {
                 "account": {
                   "authority": "saml",
                   "provider": "dfddc901-4675-4f92-80b6-e1d044592ab0",
                   "realm": "claude",
                   "id": "4905f79c9bdc48d8949003bf481942a8",
                   "userId": "u_8c58dc439da34ea1b7245d93c82bd60b",
                   "repositoryId": "dfddc901-4675-4f92-80b6-e1d044592ab0",
                   "subjectId": "TINIT-LVLDAA85T50G702B",
                   "uuid": "4905f79c9bdc48d8949003bf481942a8",
                   "username": "TINIT-LVLDAA85T50G702B",
                   "issuer": "[https://demo.spid.gov.it](https://demo.spid.gov.it)",
                   "name": "TINIT-LVLDAA85T50G702B",
                   "attributes": {
                     "authnContextClassRef": "[https://www.spid.gov.it/SpidL2](https://www.spid.gov.it/SpidL2)",
                     "mobilePhone": "3939393939",
                     "spidIssuer": "[https://demo.spid.gov.it](https://demo.spid.gov.it)",
                     "idp": "[https://demo.spid.gov.it](https://demo.spid.gov.it)",
                     "provider": "dfddc901-4675-4f92-80b6-e1d044592ab0",
                     "familyName": "Lovelace",
                     "name": "Ada",
                     "spidCode": "SPID-002",
                     "id": "TINIT-LVLDAA85T50G702B",
                     "fiscalNumber": "TINIT-LVLDAA85T50G702B",
                     "subjectId": "TINIT-LVLDAA85T50G702B",
                     "username": "TINIT-LVLDAA85T50G702B"
                   },
                   "createDate": 1784019191889,
                   "modifiedDate": 1784022238559,
                   "type": "account:saml",
                   "locked": false,
                   "accountId": "TINIT-LVLDAA85T50G702B"
                 }
               }
             ]
           }
         },
         "type": "user",
         "token": "eyJraWQiOiJiMjc3...",
         "scope": [
           "openid",
           "profile",
           "offline_access"
         ],
         "jti": "a2R1hwx1svv6tYYyryNcwsxjLdE",
         "realm": "claude",
         "expiration": 1784278586501,
         "issuedAt": 1784278576501,
         "audience": "c_8ab8ff3f10544f0891ef92f81d4572b9",
         "authorizedParty": "c_8ab8ff3f10544f0891ef92f81d4572b9"
       },
       "id": "2026-07-17T08:56:16.554Z"
     }
     ```

   * **FULL Payload:** Retains complete user details and raw unredacted payload natively.

## <a name="criteria"></a> Criteria

Must solve Issue #800 by introducing dynamic filtering (`filterUserDetails`) for `authUser.getDetails()`, remove or sanitize sensitive data, prevent log bloat in `OAUTH2_TOKEN_GRANT` events.