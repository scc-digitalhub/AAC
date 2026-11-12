---
adr_id: "0002"
title: OAuth2TokenGrantAuditFiltering
status: open
---

## <a name="question"></a> Question

How much information should AAC record in the audit event written when an OAuth2 token is issued (`OAUTH2_TOKEN_GRANT`), in order to avoid oversized audit records and the exposure of personal data and replayable tokens, and who should configure it?

### Context

Originally every token grant audit event contained the complete user profile and the clear token value. For users authenticated with SAML/SPID the profile includes the identity attributes (fiscal number, mobile phone, name, family name, ...), so each record was large, contained personal data and allowed the token to be replayed by anyone with access to the audit logs.

Related items:

* Issue #800 (closed): first proposal, a 4-level filter applied to the user profile only.
* Issue #805 (closed): token values in the audit logs must not be replayable.
* Issue #798 (open): proposal to configure the audit level of realm user authentication events separately from the identity providers.
* PR #822 (merged into 5.x, closes #797, #800, #805): current implementation of [Option 2](#option-2).
* PR #810 (open, draft): alternative token masking.

This ADR records the options so that the maintainers can confirm (or change) the approach.

## <a name="options"></a> Options

1. <a name="option-1"></a> Approach 1 (proposal of issue #800): filter only the user profile, with 4 levels configured by a dedicated user events setting (falling back to the existing identity provider `events` setting). The audit event is always written.
   * **FULL:** the user profile as it is (identities, accounts, attribute sets).
   * **DETAILS:** without attribute sets and identity attributes, keeping the core account data (email, name, surname, status).
   * **MINIMAL:** without the core account data, keeping only the basic user profile.
   * **NONE:** only the user identifiers (subject, realm, username, enabled, locked).
   * **Pros:** fine-grained control on the user profile; every token grant is always traced.
   * **Cons:** client request data and token value are not covered; `NONE` does not mean "no event", unlike the other audit settings; the setting is tied to the identity provider configuration.

2. <a name="option-2"></a> Approach 2 (current 5.x implementation, PR #822): filter the whole event with incremental levels, configured per realm by the realm administrator in the realm OAuth2 settings. When the level is not configured, `minimal` is used.
   * **NONE:** no token grant audit event is written.
   * **MINIMAL:** grant type, client identifiers, user identifiers (subject, realm, username), scopes and token metadata (id, issue and expiration time, audience, authorized party). No request data, no token value.
   * **DETAILS:** MINIMAL plus the data of the request made by the client application to the token endpoint (IP address, user agent, locale), the user accounts with their attributes and the masked token value (JWT without a valid signature, opaque tokens fully masked).
   * **FULL:** DETAILS with the complete user profile.
   * **Pros:** a single setting covers user, request and token data; `NONE` disables the event, consistently with the other audit settings; configurable per realm from the developer console; the token is never stored in a replayable form.
   * **Cons:** `DETAILS` still stores identity attributes (e.g. SPID fiscal number and mobile phone), so personal data are reduced only at `MINIMAL`; the setting is independent from the user authentication audit level discussed in #798; the token masking differs from PR #810.

   * **MINIMAL Payload Example:**
     ```json
     {
       "timestamp": 1784278576.554,
       "principal": "u_8c58dc439da34ea1b7245d93c82bd60b",
       "type": "OAUTH2_TOKEN_GRANT",
       "data": {
         "grant_type": "authorization_code",
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
         "scope": ["openid", "profile", "offline_access"],
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

   * **DETAILS Payload Example (account fields abridged):**
     ```json
     {
       "timestamp": 1784278576.554,
       "principal": "u_8c58dc439da34ea1b7245d93c82bd60b",
       "type": "OAUTH2_TOKEN_GRANT",
       "data": {
         "grant_type": "authorization_code",
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
                   "userId": "u_8c58dc439da34ea1b7245d93c82bd60b",
                   "subjectId": "TINIT-LVLDAA85T50G702B",
                   "username": "TINIT-LVLDAA85T50G702B",
                   "issuer": "https://demo.spid.gov.it",
                   "attributes": {
                     "authnContextClassRef": "https://www.spid.gov.it/SpidL2",
                     "spidCode": "SPID-002",
                     "name": "Ada",
                     "familyName": "Lovelace",
                     "fiscalNumber": "TINIT-LVLDAA85T50G702B",
                     "mobilePhone": "3939393939"
                   },
                   "type": "account:saml",
                   "locked": false
                 }
               }
             ]
           }
         },
         "type": "user",
         "token": "eyJraWQiOiJiMjc3...<payload>.__SIGNATURE__",
         "scope": ["openid", "profile", "offline_access"],
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

## <a name="criteria"></a> Criteria

* Avoid oversized token grant audit records (issue #800).
* Minimise the personal data stored in the audit logs.
* Token values in the audit logs must not be replayable (issue #805).
* Consistent meaning of the audit levels (`none`, `minimal`, `details`, `full`) across AAC, in line with issue #798.
* Configurable per realm by the realm administrators, with a safe default.
* Backward compatibility of the existing audit records and realm configurations.

Open points for the maintainers, independent from the chosen option: whether `DETAILS` should keep the identity attributes, and which token masking to keep (PR #822 or PR #810).
