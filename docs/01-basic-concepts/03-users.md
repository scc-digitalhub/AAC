# Users

Users are modelled in AAC as a set of digital *identities*, each composed of an *account* bound to a set of *attributes*.

A single user corresponds to a single *subject*, i.e. uniquely identified actor of type *user* with a statically assigned *subjectId*. Every identity bound to the subject can be used to perform an *authentication process* and obtain a valid session in AAC, which can then be used to produce access and id tokens for client applications.

In figure you can see the user model as described.

TODO

## Identities

User *identities* are a logically sound representation of a given subject as managed by a given *identity provider*. They are made up by the combination of a user *account* used to perform authentication tasks and one or more sets of user *attributes*, well-defined groups of properties describing characteristics of users according to a schema.

The process of binding identities to subjects is called *linking*, and is performed either automatically by AAC at login, by inspecting linkable *attributes*, or manually by an administrator.

Do note that by default AAC evaluates the *(verified) email address* as linking attribute. As such, for identity models which do not expose the email, linking won't be supported.

## Accounts

User *accounts* are the foundation of *identities*, and are implementation-specific representations of user model in accordance with the specific identity *authority*. For example, the *internal* authority will store *(username, password)* to enable credentials-based logins, while the *oidc* authority will store *(issuer, sub)* wo identify logins from different external providers.

All the properties stored in user accounts can be inspected and retrieved by clients and mappers by requesting the *user.accountprofile.me* scope.

## Attributes

Attributes are modelled as a set of *key-value* properties, defined by a *type* which can be *primitive* (string, boolean, number, date etc) or *complex* (json object). When retrieved from providers at login, attribute maps are processed and converted into a basic *attributeSet* linked to the *account*.

Later in the process, *attribute providers* can access the identity attribute set and derive different sets following specific schemas, such as *basic, openid, email*, where the definition and meaning of keys and values are treated according to the standard.