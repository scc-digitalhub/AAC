# Audit

An *audit trail* is a ledger containing all the events which are relevant for a given realm, regarding:

* user successful authentication
* user failed authentication
* client successful authentication
* client failed authentication
* oauth2 access token grant
* oauth2 access token revocation
* openid id token creation
* openid endsession
  
Every event is collected by AAC and automatically transformed into a suitable transposition, always guaranteeing confidentiality and secrecy of the content.

The design of the system ensures that audit events are *unmodifiable* after their creation: they are received by a dedicated component and inserted into an *append-only* database.

Future releases will support the usage of *external* storage systems, content encryption and partial confidentiality.


## Audit console

Administrators can access the dedicated console by navigating to the developer console and selecting *Audit* from the menu.

The interface lets users consult the registry, inspect events payload and verify their source. By inserting either a *start date* or and *end date* administrators can retrieve results from a specific time interval, and investigate suspicious activities at need.

Furthermore, the *audit* console is available directly from the *users management console*, where authorized administrators can review the specific user activity.
