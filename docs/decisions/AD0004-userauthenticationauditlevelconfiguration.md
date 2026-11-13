---
adr_id: "0004"
title: UserAuthenticationAuditLevelConfiguration
status: open
---

## <a name="question"></a> Question

Should the audit level of the user authentication events (`USER_AUTHENTICATION_SUCCESS`, `USER_AUTHENTICATION_FAILURE`) be configured separately from the `events` setting of the identity providers, and where?

### Context

Every identity provider has an `events` setting (`none`, `minimal`, `details`, `full`). Today it drives two different sets of audit events:

* the protocol events of the provider (SAML, SPID, OpenID Federation requests and responses): `none` writes no event, `full` adds the complete protocol message, `minimal` and `details` are identical;
* the user authentication events, written at every login by `UserAuthenticationEventListener` for every provider: on success `minimal` adds the basic authentication data, `details` adds the user details, `full` adds the provider authentications too; on failure `details` and `full` add the exception, `full` also the authentication.

One key has two meanings: an administrator cannot, for example, keep the complete SPID messages (`full`) without also storing the complete user authentication at every login.

Two more facts about the current listener (5.x):

* `none` does not disable the user authentication events: they are still written, with provider, realm and request details only.
* When the provider has no `events` value, the `details` default declared in the listener is not applied: the success event is written with provider, realm and request details only, as with `none` today.

Related items:

* Issue #798 (open, nothing implemented): proposes a new key `userEventsLevel` for the user authentication events, in a new **Audit** tab of the realm configuration, with a fallback to `events` when the key is not set. The issue does not say where the key is stored: the tab is at realm level, but its code sample reads the key from the identity provider settings.
* ADR 0002 (decided): the token grant events already have their own realm level (`eventsLevel`, realm OAuth2 settings).

## <a name="options"></a> Options

1. <a name="option-1"></a> Keep the current configuration: the provider `events` setting drives both sets of events.
   * **Pros:** no new setting; the login level can differ per provider.
   * **Cons:** one key, two meanings; protocol and login levels cannot be set independently; no realm-wide setting.

2. <a name="option-2"></a> Realm level (issue #798): `userEventsLevel` in a new realm audit configuration (Audit tab), as done for `eventsLevel` in ADR 0002, with a fallback to the provider `events` when not set. The provider `events` setting is unchanged.
   * **Pros:** separates the two meanings; one setting per realm; existing realms keep working without migration.
   * **Cons:** once set, the same level applies to all the providers of the realm; new realm configuration section (model, API, console, import/export); with the fallback, two settings can affect the same event.

3. <a name="option-3"></a> Provider level: `userEventsLevel` added to the identity provider settings, next to `events` (as in the code sample of #798), with the same fallback. No realm tab.
   * **Pros:** separates the two meanings; keeps the per-provider granularity; smallest change.
   * **Cons:** no realm-wide setting: changing the login level of the whole realm means editing every provider; one more field in every provider form; with the fallback, two settings can affect the same event.

4. <a name="option-4"></a> Same as [Option 2](#option-2), and the Audit tab becomes the single place for the realm audit levels: `eventsLevel` of ADR 0002 moves there from the OAuth2 tab.
   * **Pros:** all the realm audit levels in one place.
   * **Cons:** moves a setting already released (migration of the existing realm configurations and API); larger change.

## <a name="criteria"></a> Criteria

* One meaning per setting.
* No migration of the existing provider and realm configurations.
* Same level names as the other audit events, and `none` writes no event, as for the token grant events (ADR 0002). This applies to every option, [Option 1](#option-1) included.
* Granularity really needed by the administrators: per realm or per provider.
* Limited impact on model, API, console and import/export.

Open point, independent from the chosen option: which default applies when no level is configured.

This ADR only proposes the hypothesis; the choice among the options is left to the maintainers.
