---
adr_id: "0004"
title: UserAuthenticationAuditLevelConfiguration
status: open
---

## <a name="question"></a> Question

Should the level of detail of the login events in the audit log be chosen separately from the one of the identity provider protocol events, and where?

### Context

Every identity provider has one audit setting (`events`), with four levels: none, minimal, details, full. Today this single setting drives two different kinds of audit events:

* the protocol events of the provider: the SAML, SPID or OpenID Federation messages exchanged during a login;
* the login events: the successful or failed login of a user, written for every provider.

One setting has two meanings: an administrator cannot, for example, keep the complete SPID messages without also logging the complete user profile at every login.

The two kinds of events also read the setting in a different way. With `none` the protocol events are not written, while the login events are still written, with the basic data only. The login events are written with the basic data only also when the setting is empty.

Related items:

* Issue #798 (open, nothing implemented): proposes a separate setting for the login events, in a new **Audit** tab of the realm settings, which falls back to the provider setting when it is empty.

## <a name="options"></a> Options

1. <a name="option-1"></a> Keep one setting for both kinds of events.
   * **Pros:** nothing to add; the login level can be different for each provider.
   * **Cons:** one setting, two meanings; the two levels cannot be chosen separately.

2. <a name="option-2"></a> A separate setting for the login events, for the whole realm, in a new Audit tab of the realm settings (issue #798). When it is empty, the provider setting is used, as today.
   * **Pros:** the two levels can be chosen separately; one place for the whole realm; nothing changes for the existing realms until the new setting is used.
   * **Cons:** the same level applies to all the providers of the realm; a new section of the realm settings to build.

3. <a name="option-3"></a> A separate setting for the login events on each identity provider, next to the existing one. When it is empty, the existing setting is used, as today.
   * **Pros:** the two levels can be chosen separately; the login level can still be different for each provider; smallest change.
   * **Cons:** no single place for the whole realm: the providers must be changed one by one.

## <a name="criteria"></a> Criteria

* Each setting has one meaning.
* Nothing changes for the existing realms and providers until someone chooses it, apart from the meaning of `none`.
* With `none` nothing is logged, for the login events too, as it already is for the protocol events and for the token events. This holds whichever option is chosen.
* How fine the choice has to be: one level for the realm, or one for each provider.

This ADR only proposes the hypothesis; the choice among the options is left to the maintainers.
