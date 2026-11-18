---
adr_id: "0003"
comments:
    - author: "gianpaolo-tndigit"
      comment: "1"
      date: "2026-11-18 11:35:18"
status: decided
title: FirstPartyEndSessionWithoutConfirmation
---

## <a name="question"></a> Question

When a **first party** client asks AAC to log the user out, should AAC do it right away and send the user back to the client, instead of always asking the user to confirm?

### Context

Today, when a logged-in user reaches the AAC logout (end session) endpoint, AAC always shows a confirmation page. The session is closed only after the user confirms, and the user is then sent back to the client. A user who is not logged in is sent back right away.

First party clients are already trusted at login, where AAC can approve their permissions automatically, without asking the user. At logout there is no such handling: the user chooses "logout" in the application and then has to confirm again on an AAC page.

The [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) specification says that the server should ask for confirmation, and must ask when the request does not carry a valid `id_token_hint` of the user currently logged in.

Related items:

* Issue #560 (open): the proposal.
* PR #872 (open): implements [Option 2](#option-2).

## <a name="options"></a> Options

1. <a name="option-1"></a> Keep the current behaviour: always ask for confirmation, for every client.
   * **Pros:** no change; the user always knows that the whole AAC session, which may be shared with other applications, is being closed.
   * **Cons:** an extra page and an extra click for first party applications; not consistent with the trust given to first party clients at login.

2. <a name="option-2"></a> Skip the confirmation when the client is first party and the request carries a valid `id_token_hint` of the user currently logged in: AAC closes the session right away, without showing the confirmation page. In every other case the confirmation page is still shown.
   * **Pros:** consistent with the login; smoother logout for trusted applications; within what the specification allows.
   * **Cons:** changes the behaviour of all the existing first party clients, with no way to opt out; the user is no longer told that the whole AAC session is being closed.

3. <a name="option-3"></a> Same as [Option 2](#option-2), but only for the first party clients where an administrator enables it. Disabled by default.
   * **Pros:** nothing changes for the existing clients; administrators decide client by client.
   * **Cons:** one more client setting to manage.

## <a name="criteria"></a> Criteria

* Stay within the OpenID Connect specification.
* Consistency with how first party clients are treated at login.
* A user must not be logged out by a request that does not come from their own session.
* A smoother logout, against telling the user that the whole session is being closed.

## <a name="outcome"></a> Outcome
We decided for [Option 2](#option-2) because: a client marked as first party is already trusted, the `id_token_hint` proves that the request comes from the session of the user, and the specification asks for a confirmation only when that proof is missing. No new setting is needed. PR #872 implements it.

## <a name="comments"></a> Comments
<a name="comment-1"></a>1. (2026-11-18 11:35:18) : marked decision as decided
