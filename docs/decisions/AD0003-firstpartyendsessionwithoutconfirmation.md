---
adr_id: "0003"
title: FirstPartyEndSessionWithoutConfirmation
status: open
---

## <a name="question"></a> Question

When a **first party** client starts an OpenID Connect RP-Initiated Logout, should AAC log the user out automatically and redirect straight back to the client, instead of always asking the user to confirm the logout?

### Context

Today, when an authenticated user reaches the AAC end session endpoint, AAC always shows a confirmation page. Only after the user confirms is the AAC session terminated and the user redirected to the client `post_logout_redirect_uri` (with `state`). If the user is not logged in, the redirect happens right away.

AAC already treats first party clients as trusted during login: their scopes are auto-approved for users of the same realm, and they are not forced to ask consent for `offline_access`. The logout flow has no such handling, so a first party application always shows an extra AAC page, even though the user has already chosen "logout" inside the application.

The [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) specification says that the OP SHOULD ask the End-User whether to log out of the OP as well, and that the confirmation is needed in particular when no valid `id_token_hint` is provided or when it does not match the current session (protection against unwanted or forced logouts).

Related items:

* Issue #560 (open): proposes to skip the confirmation for first party clients when a valid `id_token_hint` matching the current session is provided.
* PR #872 (open, targets 5.x, fixes #560): implementation close to [Option 2](#option-2), without a configuration setting. It should be merged or changed according to the outcome of this ADR.

## <a name="options"></a> Options

1. <a name="option-1"></a> Keep the current behaviour: always ask for confirmation, for every client.
   * **Pros:** no change; most conservative reading of the specification; the user always explicitly confirms the end of the AAC session, which may be shared with other applications (SSO).
   * **Cons:** extra page and extra click for first party applications; inconsistent with the trust already granted to first party clients at login.

2. <a name="option-2"></a> Automatic logout for first party clients: skip the confirmation only when all these conditions hold:
   * the request carries a valid `id_token_hint` issued by AAC for the currently authenticated user;
   * the client is first party and belongs to the same realm as the user;
   * the `post_logout_redirect_uri`, if present, is registered for the client.

   In that case AAC terminates the session and redirects directly to the client. In every other case the confirmation page is still shown.
   * **Pros:** consistent with the first party handling at login; smoother experience for trusted applications; the `id_token_hint` check prevents forced logouts, as suggested by the specification.
   * **Cons:** changes the behaviour of all existing first party clients, with no way to opt out; the user is no longer told that the whole AAC session (not only the calling application) is terminated.

3. <a name="option-3"></a> Same as [Option 2](#option-2), but enabled by an explicit per-client setting (available only for first party clients), disabled by default.
   * **Pros:** backward compatible; administrators decide client by client; same security conditions as [Option 2](#option-2).
   * **Cons:** one more client setting to manage in the console, API, import/export and documentation.

## <a name="criteria"></a> Criteria

* Compliance with OpenID Connect RP-Initiated Logout 1.0: the confirmation may be skipped only for trustworthy requests (valid `id_token_hint` for the current session, registered redirect URI).
* Consistency with the existing meaning of first party clients in AAC (trusted client of the same realm).
* No forced logout or open redirect introduced.
* Backward compatibility for existing realms and clients.
* User experience of first party applications versus transparency towards the user about the end of the SSO session.

This ADR only proposes the hypothesis; the choice among the options is left to the maintainers.
