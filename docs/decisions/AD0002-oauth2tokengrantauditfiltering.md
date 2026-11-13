---
adr_id: "0002"
comments:
    - author: "gianpaolo-tndigit"
      comment: "1"
      date: "2026-11-13 09:52:21"
status: decided
title: OAuth2TokenGrantAuditFiltering
---

## <a name="question"></a> Question

How much information should AAC record in the audit event written when an OAuth2 token is issued (`OAUTH2_TOKEN_GRANT`), and where is the level of detail configured?

### Context

Before PR #822 every token grant event stored the complete user details and the clear token value, with no way to reduce them. Each record was large, always contained the identity attributes (for SAML/SPID: fiscal number, mobile phone, name, family name) and exposed a token that could be replayed by anyone with access to the audit logs. The events of the refresh token grant did not contain the user.

Related items:

* Issues #800 (filtering of the event), #805 (replayable tokens), #797 (user missing on refresh): closed by PR #822, merged into 5.x.
* Issue #798 (audit level of the user authentication events): a different problem, not decided here.

## <a name="options"></a> Options

1. <a name="option-1"></a> Filter only the user profile (first proposal of issue #800): 4 levels (`full`, `details`, `minimal`, `none`) applied to the user profile, read from the setting proposed in #798. The event is always written; at `none` it keeps only the user identifiers.
   * **Pros:** every token grant is always traced.
   * **Cons:** request data and token value are not covered; `none` still writes an event; depends on #798, which is not implemented.

2. <a name="option-2"></a> Filter the whole event with incremental levels, set per realm by the realm administrator in the realm OAuth2 settings (`eventsLevel`, default `minimal`). Current 5.x implementation (PR #822).
   * **NONE:** no event is written.
   * **MINIMAL:** grant type, client (id, realm, name), user identifiers (subject, realm, username), scopes, token metadata (id, issue and expiration time, audience, authorized party).
   * **DETAILS:** MINIMAL plus the client request data (IP address, user agent, locale), the accounts of the user identities with their attributes, and the masked token value.
   * **FULL:** DETAILS with the complete user details in place of the accounts only.
   * **Pros:** one setting covers user, request and token data; `none` disables the event, as for the identity provider protocol events (SAML, SPID, OpenID Federation); no dependency on #798; personal data are stored only when `details` or `full` is selected.
   * **Cons:** one more audit setting, separate from the identity provider `events` setting.

   The token value is never stored in clear: the signature of a JWT is replaced with `__SIGNATURE__`, any other token is replaced with `***MASKED_TOKEN***`.

   Size of `data` for the same SPID login (example of issue #800, compact JSON, token excluded): 515 bytes at `minimal`, 1714 bytes at `details` (992 of which are the identity accounts).

   * **MINIMAL payload:**
     ```json
     {
       "timestamp": 1784278576.554,
       "principal": "u_8c58dc439da34ea1b7245d93c82bd60b",
       "type": "OAUTH2_TOKEN_GRANT",
       "data": {
         "grant_type": "authorization_code",
         "client": {
           "clientId": "c_8ab8ff3f10544f0891ef92f81d4572b9",
           "realm": "test",
           "clientName": "Test User"
         },
         "user": {
           "subjectId": "u_8c58dc439da34ea1b7245d93c82bd60b",
           "realm": "test",
           "username": "TINIT-LVLDAA85T50G702B"
         },
         "type": "user",
         "scope": ["openid", "profile", "offline_access"],
         "jti": "a2R1hwx1svv6tYYyryNcwsxjLdE",
         "realm": "test",
         "expiration": 1784278586501,
         "issuedAt": 1784278576501,
         "audience": "c_8ab8ff3f10544f0891ef92f81d4572b9",
         "authorizedParty": "c_8ab8ff3f10544f0891ef92f81d4572b9"
       },
       "id": "2026-07-17T08:56:16.554Z"
     }
     ```

   * **DETAILS adds to `data` (fields abridged):**
     ```json
     {
       "webAuthenticationDetails": {
         "remoteAddress": "0:0:0:0:0:0:0:1",
         "userAgent": "PostmanRuntime/7.51.1",
         "locale": "it_IT"
       },
       "user": {
         "details": {
           "identities": [
             {
               "account": {
                 "authority": "saml",
                 "provider": "dfddc901-4675-4f92-80b6-e1d044592ab0",
                 "username": "TINIT-LVLDAA85T50G702B",
                 "issuer": "https://demo.spid.gov.it",
                 "attributes": {
                   "authnContextClassRef": "https://www.spid.gov.it/SpidL2",
                   "spidCode": "SPID-002",
                   "name": "Ada",
                   "familyName": "Lovelace",
                   "fiscalNumber": "TINIT-LVLDAA85T50G702B",
                   "mobilePhone": "3939393939"
                 }
               }
             }
           ]
         }
       },
       "token": "eyJraWQiOiJiMjc3...<payload>.__SIGNATURE__"
     }
     ```

## <a name="criteria"></a> Criteria

* Avoid oversized token grant audit records (issue #800).
* Token values in the audit logs must not be replayable (issue #805).
* No personal data in the audit logs by default: identity attributes only at the levels selected for that purpose.
* Same level names as the identity provider protocol events (SAML, SPID, OpenID Federation), with `none` meaning "no event".
* Configurable per realm by the realm administrators, with a safe default.

## <a name="outcome"></a> Outcome
We decided for [Option 2](#option-2) because: it is the only option that covers user data, request data and token value with one per-realm setting, it does not depend on #798, and it is already implemented in 5.x (PR #822, merged on 2026-08-25). The content of `details` and `full`, identity attributes included, was reviewed with the maintainers and confirmed as intended: realms that must not store these data keep `minimal` (default) or `none`.

## <a name="comments"></a> Comments
<a name="comment-1"></a>1. (2026-11-13 09:52:21) : marked decision as decided
