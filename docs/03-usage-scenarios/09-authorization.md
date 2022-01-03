# Authorization Scenarios

The *authorization* process aims to verify whether a given subject (a user or an application), represented by a specific *identity* and successfully performed the *authentication*, has the *permission* to perform a specific action on a given resource.  That is, in authorization, a user or application is granted access to an API after the API determines the extent of the permissions that it should assign. 

The authorization is achieved as binding a *policy* to an *identity*. This policy needs to express the permission constraints in a way suitable for consumption by applications. 

The general authorization flow is performed as follows:

1. Identify the resource that should be protected (e.g., API of an application). 
2. Define the policies that are required to protect the resource when accessed by a subject.
3. Implement the policy enforcement mechanisms that realize the policies. 

To support this process, AAC natively supports these *authorization* models:

* ABAC, which are *attribute-based* 
* IBAC, which are *identity-based*
* RBAC, which are *role-based*

The implementation of the policies may be done in two ways. 

First, it is possible to use AAC to model the policies, following the concept *permissions* (or scopes) that are assigned to the protected resource on the one side and to the identity on the other side. This approach is naturally supported by the OAuth2.0 protocol implemented by AAC. The three authorization models defined above may be used for the implementation.

Second it is, possible to implement the policies outside of the AAC, but exploiting the identity information provided by AAC upon authentication performed. This is particularly suitable when the complexity of the policies goes beyond the standard protocols and models and requires specific implementation.

* [Using AAC to implement the authorization permissions](10-authorization-permissions.md)
* [Using identity information to implement authorization policies](11-authorization-identity.md)