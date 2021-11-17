# Roles and authorities

AAC handles users and roles according to various logical schemas, in accordance with different usage scenarios.

At minimum, users and client can have one or more *authorities* associated, which are bound to a selected realm and determine the level of privileges and permissions users can obtain *inside AAC*. The permissions define whether users or clients can *use, manage or administer* the realm in AAC.

The authorities available are:

* USER
* CLIENT
* DEVELOPER
* ADMIN

which define the various permission levels.

Do note that authorities are meant to be used *only* inside AAC, to define management.

**Roles** on the other hand are defined, managed and assigned to users and clients by realm *administrators* according to their needs, with no fixed definition.
At minimum, *roles* can be used to describe organizational *groups*.
By approving permissions for specific service *scopes*, and then assigning roles to users, admins can obtain a fully fledged RBAC authorization framework, manageable via UI or API.