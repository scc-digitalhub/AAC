# Realms

## A security realm

A system which is composed by different parts, such as the client+server application model widely adopted on the web, needs to define and adopt a given level of *trust* between parties involved in the communication. When we can define a domain where each and every actor has a given level of trust in all the other members, we obtain a *trust domain*, which is the foundation of a *security domain*.

A security domain is thus a space where all the actors possess a degree of trust in all the other participants, either directly or indirectly. In combination with digital identities, we define a *realm* as a security domain where the identities are trusted by all the various actors.

Do note that *trust* doesn't mean that actors do not need to verify communications and assertions, but only that each actor can assert the level of trust in a given interaction, and the decide whether to accept or refuse. 

AAC, by adopting *OAuth2* and *OpenID Connect* frameworks, offers applications the ability to verify *access and identity tokens*, and thus assert the trust level and the correctness of the data presented by clients.

## Multi-realm

In a digital environment which shares a common set of criteria for user and clients management, we can either include each and every agent in a single *security domain*, or support a form of *multi-tenancy*, where different organizations can each possess a distinct, isolated *security domain*.

Realms are used to define a cohesive and shared security domain. AAC supports multi-tenancy via the definition of multiple realm, each modelled as an independent tenant with a dedicated user base, client repository, identity providers etc.

The common usage of realms follows logical organizational patterns, where a given *environment* owned by an organization is linked to a private *realm*. Many different applications and backend services can live under the same realm, as long as they belong to the same *security domain* (rooted in AAC) and share a common user and client base.


## Cross-realm

When client interactions need to cross the realm barrier, we obtain a *cross-realm* interaction.
Common examples are:

* a client/user from realm A tries to access a backend service in realm B
* a backend from realm B want to offer services to users from realm A

In these cases, and all the similar ones, we can identify the action which crosses the barrier. Given that realm are *independent* security domains, by default there should be *zero trust* between different realms. As such, cross-realm interactions are inherently insecure. Given the presence of a single shared root for all realms, which is the single instance of AAC, we can mitigate this criteria by transferring some level of trust from realm A to AAC to realm B, and in also in the opposite way. This enabled us to connect different isolated realms in a secure and reliable manner.