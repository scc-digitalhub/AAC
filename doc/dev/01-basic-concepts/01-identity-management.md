# Identity management


A digital identity is a representation of a given *agent* in a digital environment, made of properties and assertions which uniquely identify and describe the user in a form suitable for the consumption of digital applications.

These information usually include data about:

* the *subject* of the identity (a user, a client, a machine etc)
* the *attributes* describing properties of the identity
* the *identificators*, which are attributes which discriminate this identity from all the others, either distinctively or uniquely

The specific models and processing procedures associated to identities are the domain of a digital *identity management system* (not exhaustively: definition, collection, storage, retrieval, authentication...).

AAC, which stands for *Authentication and Authorization Control module*, is a fully fledged identity manager and provider.


## Identification

The *identification* process defines the validity of a given digital identity, as presented by a specific agent. Not to be confused with *authentication* (see below), identification only aims at binding a valid identity to a given agent, usually by deciding whether the identity:

* exists in the domain, or
* can be created in the domain

The identity is usually bound to an application session, which will be used to perform tasks with the given identity.

## Authentication

As per definition, authentication is the process of *verifying the identity of the user*.
Given an identity, we can examine the claims which bind this to a given user by performing the **authentication process**:

* collect information from the user
* validate the correctness, freshness and validity of the proofs

A common way to perform authentication is to ask users to provide an **identificator**, such as a *username*, along with a **secret**, such as a *password*.
The identifier will be used to find and bind an existing identity to the user, while the secret will be used to **prove** that the agent is legit.
This process is a single-factor authentication, which uses a *knownledge factor* to verify the process.
Other commonly used factors are:

* possession of a *key*
* biometrics
* location

When different factors are employed within a single authentication process, we obtain a Multi-Factor authentication, a more secure and robust verification process.

## Authorization

The *authorization* process aims to verify whether a given user, represented by a specific *identity* and successfully performed the *authentication*, has the *permission* to perform a specific action on a given resource.

As such, it depends on the ability of the system to bind some kind of access *policy* to an *identity*. This policy needs to express the permission constraints in a way suitable for consumption by applications. 

Among the many models available, AAC natively supports these *authorization* models:

* ABAC, which are *attribute-based* 
* IBAC, which are *identity-based*
* RBAC, which are *role-based*

With the integration of external components, AAC can also support *RAC*, which are rule based policies.

### ABAC

Given that each identity is described in the system as an ensemble of *attributes*, any one among these can be used by access control modules to evaluate permissions. For example, the email domain can be used to control access to a diagnostic dashboard.

AAC, by adopting the *OpenID Connect* framework, is able to describe users (and clients) in an authoritative and verifiable way, providing all kind of properties as attributes via *claims*. The key point in supporting the ABAC model in fact is not only to provide a list of attributes, but to enable consumers (ie access control modules) to verify the validity, correctness and freshness of such attributes. By digitally signing assertions, AAC marks the validity of a given set of claims at that specific instant in time, in an externally verifiable (and thus trustable) way.

### IBAC

Identity based policies are essentially based on the ability of the system to properly and securely *identify* the agent (user, client etc) performing a given action.
AAC, via *OAuth2 access tokens* and *OpenID Connect id tokens*, provides clients with a digitally signed (and thus verifiable) token which carries such identity identifiers, enabling access control modules in performing the evaluation of IBAC policies.

### RBAC

Role based access control is built on the concept of *roles* as an ensemble of *permissions*. A permission is the definition of a given *action*, usually bound to a specific resource (or resource type). Roles group permission sets by logical or other organizationally sound ways, and are then assigned to users.
The consequence of this is the *inheritance* of permissions: users possessing a given role will also have access to all the permissions assigned to given role.

AAC fully supports the RBAC model, by providing developers with the ability to define:

* permissions as *scopes* on custom APIs
* roles as sets of *permissions*

and then roles can be assigned to both users and clients.
Eventually, access control modules will be able to receive a digitally signed access token carrying the information of which roles are assigned to the carrier.




