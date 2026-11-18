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

How much information should AAC record in the audit log every time an OAuth2 token is issued, and who decides it?

### Context

Before PR #822 every token issued was logged with the complete user profile and the token itself, with no way to reduce them. Each event was large, contained the personal data of the user, and a token read from the log could be reused. When a token was refreshed, the event was logged without the user data (issues #800, #805, #797).

## <a name="options"></a> Options

1. <a name="option-1"></a> Reduce only the user profile (first proposal of issue #800): four levels decide how much of the user profile is logged. The event is always written.
   * **Pros:** every token issued is always traced.
   * **Cons:** the request data and the token are still logged as before; the event cannot be switched off; it relies on a setting proposed in issue #798 that does not exist yet.

2. <a name="option-2"></a> Reduce the whole event with four levels, chosen for each realm by its administrator in the realm OAuth2 settings. Implemented in 5.x by PR #822.
   * **None:** nothing is logged.
   * **Minimal** (default): who asked for the token (client and user identifiers) and what was issued (grant type, scopes, token identifier and validity).
   * **Details:** adds where the request came from (e.g. IP address), the account used to log in with its attributes, and the token in a masked form that cannot be reused.
   * **Full:** adds the complete user profile.
   * **Pros:** one setting covers user, request and token; the event can be switched off; the token is never logged in a reusable form.
   * **Cons:** one more audit setting to manage, next to the one of the identity providers.

   Example for a SPID login (identifiers shortened). At Minimal the event contains:
   ```json
   {
     "grant_type": "authorization_code",
     "client": { "clientId": "c_8ab8ff3f", "realm": "test", "clientName": "Test Client" },
     "user": { "subjectId": "u_8c58dc43", "realm": "test", "username": "TINIT-LVLDAA85T50G702B" },
     "type": "user",
     "scope": ["openid", "profile", "offline_access"],
     "jti": "a2R1hwx1svv6tYYyryNcwsxjLdE",
     "realm": "test",
     "expiration": 1784278586501,
     "issuedAt": 1784278576501,
     "audience": "c_8ab8ff3f",
     "authorizedParty": "c_8ab8ff3f"
   }
   ```
   Details adds (some fields omitted):
   ```json
   {
     "webAuthenticationDetails": {
       "remoteAddress": "10.1.2.3",
       "userAgent": "PostmanRuntime/7.51.1",
       "locale": "it_IT"
     },
     "user": {
       "details": {
         "identities": [
           {
             "account": {
               "authority": "saml",
               "username": "TINIT-LVLDAA85T50G702B",
               "issuer": "https://demo.spid.gov.it",
               "attributes": {
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
     "token": "eyJraWQiOiJiMjc3In0.eyJzdWIiOiJ1In0.__SIGNATURE__"
   }
   ```
   Full replaces `user` with the complete user profile, including the accounts not used for this login. No example is given, as it depends on the user.

## <a name="criteria"></a> Criteria

* Keep the audit log small.
* A token read from the audit log must not be reusable.
* Personal attributes are logged only when the administrator asks for them.
* Each realm decides for itself, with a safe default.

## <a name="outcome"></a> Outcome
We decided for [Option 2](#option-2) because: it reduces user, request and token data with one setting per realm, and it is already implemented in 5.x (PR #822). The content of the Details and Full levels, personal attributes included, was confirmed with the maintainers.

## <a name="comments"></a> Comments
<a name="comment-1"></a>1. (2026-11-13 09:52:21) : marked decision as decided
