# Using AAC to implement the authorization permissions

In AAC the implementation of the authorization permissions is based on assigning the permissions to the protected resources (APIs) and on defining the rules to assign these permissions to identities (users or applications). Role-based, attribute-based, or identity-based policies may be used for this purpose.

## Overview

The process of authorization implementation is based on the following steps:

1. Determine and define the resource to be protected. In AAC this is captured through *custom services*.
2. Determine different *permissions* that the different resources (API methods) may require with respect to the protected resource.
3. Define the policies that determine when the permissions may be assigned to specific subjects.

Once implemented, AAC uses the OAuth2.0 protocol to enforce the authorization as follows

1. The requesting party needs to obtain the access token in order to perform resource access and therefore requests AAC to provide it. 
2. Together with the request, explicitly or implicitly, the requesting party should specify *which* resources it intends to access, e.g., by specifying the permissions (scopes in OAuth2.0 terminology) associated with the requested resource.
3. Authenticate the subject (user or client app) and extract the subject information associated to the provided identity.  
4. Using the information about the subject, AAC evaluates the policies associated to the requested resource and, in case of positive evaluation, provides the access token that is explicitly associated with the granted permissions.
5. The requesting partner provides the access token that is evaluated by the protected resource (server). The evaluation checks in particular that the necessary permissions are present within the token information. 

It is important to note that the OAuth2.0 protocol does not explicitly defines the rules to grant permissions to the specific subject. The following approaches are, therefore, may be considered.

## Explicit User Consent

In some scenarios the protected resources deal with the end user data exposing that data as APIs. A typical example refers to the user profile data exposed by the Identity Provider and by AAC in particular. Following Opend ID Connect protocol, the ``/userinfo`` endpoint exposes user information attributes. To access these attributes, it is possible to specify different permissions to distinguish different blocks of information, more or less "public". In this case, the ownership of the data by the end user entails the necessity to get an explicit consent of the user to get such data.

That is, if the request is performed by a *third party* application on behalf of the user owning the data exposed by such resource, it is necessary to obtain a *consent* of the user for the app with respect to the necessary permissions. In case of the OAuth2.0 protocol, this consent is explicitly requested by AAC upon successful authentication: during this Web-based flow the user is presented with the list of the permissions requested by the third party and only if the permissions are granted the token request flow proceeds.

In order to implement this functionality for custom user-related resources, the following steps should be performed:

1. Define the custom service that represents the protected resource. See [here](../02-quick-start/01-base/04-custom-service.md) for the details about the custom service definition.
2. Define the permissions (scopes) that the end-users should explicitly grant. Such scopes should be of type **user**.
3. Ensure that the provided token is associated with the required permission. Based on the service implementation, this may be done in variety of ways. See [here](./06-call-your-api-user.md) for the corresponding details.

## Role-Based Access Control (RBAC)

The decision about whether to grant the access to the specific subject may be achieved through Role-Based Access Control natively supported by AAC. More specifically, the RBAC policies may be defined as follows:

1. Define the custom service that represent the protected resource. See [here](../02-quick-start/01-base/04-custom-service.md) for the details about the custom service definition.
2. Define the permissions (scopes) that are associated to this custom service. Depending on the type of the expected usage, the permissions may be of type **user** (i.e., requested on behalf of an end user and granted by the user consent), **application** (directly requested bythe client application), or **generic** (both scenarios are possible).
3. Model *custom roles* within the realm that are specific to the resources managed by the realm and associate permissions of the custom services to the role. See [here](../02-quick-stark/02-advanced/03-roles.md) for the information about how to configure custom roles. 
4. Assign roles to the interested subjects (users or clients). During the policy evaluation, AAC will check the roles of the subject and will verify whether the requested permissions are covered by the roles. The assigned may be done via AAC user management console or programmatically via API.


## Attribute-based Access Control (ABAC)

In certain cases it is possible that the decision about permission authorization depends on some specific information associated to the authorized subject. For example, it is possible to authorize the users based on their email domain, their residence, the fact that they make a part of particular community, etc. In this case, the control is *attribute-based*, where the attributes are the specific properties of the user or an application. 

While providing standard user attributes (e.g., OpenID attributes like name/surname, email, profile, etc) AAC supports various ways to define custom attributes and to assign them to the user. This includes in particular

* Attributes defined by the supported Identity Providers
* Attributes provided by some external services via API
* Attributes explicitly associated to the subjects and managed directly by AAC (through console or via API)
* Derived attributes that may be obtained from the basic ones programmatically

To exploit custom attributes for ABAC, it is necessary to 

1. Define *custom attribute set*. Describe specific user properties, their type and description.
2. Define *attribute providers*. It is possible to 
   
     * define the provider as a *mapping* from the identity provider attributes, 
     * the provider that stores the attributes and is managed by AAC itself (*internal* attribute provider), 
     * the external API providing the attribute info for the given subject (*webhook* provider), and 
     * as a custom function that derives the new attributes out of existing ones (*script* provider). 

   For further details regarding attribute definition and provisioning, see [here](../02-quick-start/02-advanced/02-attributes.md).

3. Define the approval policy rule as a custom *scope approval function*. In AAC the custom service scope configuration allows for defining the scope using RBAC policy or as a custom mapping function that takes the client app data, user data (if applicable), and the list of scopes as input and should return an object containing the approval information (``approved`` and ``expiresAt`` values).

When this approach is implemented, the AAC will execute the custom approval function upon the authorization request.

## Space Roles

In addition, for the purpose of authorization, AAC provides and extra mechanism to control the access policies, namely **space roles**. Differently from global or realm roles, the space roles are bounded to a context (named *space*) which is controlled by
the *owner* of the space. 

Space roles represent a model that supports a mix of RBAC and ABAC. Specifically, a space may be seen as a directory, where the owner of the directory may create subdirectories and may assign the ownership/roles to these subdirectories. Please note that the ownership is not inherited by default: the owner of a directory is not automatically owner of all the subdirectories. 


Role spaces uniquely identified by their namespace and managed by the space owners. A user may have different roles in different spaces and the authorization control for individual organizations, components, and deployment may be performed within the corresponding space. More specifically, each role is represented as a tuple with

* *context*, defining the "parent" space of roles (if any)
* *space*, defining the specific role space value. Together with the context form the unique role namespace
* *role*, defining the specific role value for the space.

In this way, the spaces may be hierarchically structured (e.g., departments of an organization may have their own role space within the specific organization space).

Space roles provide an easy and flexible way to capture such scenarios like multi-tenancy, authorization of access to different, hierarchical datasets or data spaces, etc.

Syntactically, the role is therefore represented in the following form: ``<context>/<space>:<role>``. To represent the owner of the space the role therefore should have the following signature: ``<context>/<space>:ROLE_PROVIDER``.

The owner of the space may perform the following functionalities:

  * associate/remove users to/from the arbitrary roles in the owned spaces (including other owners).
  * create new child spaces within the owned ones. This is achieved through creation of the corresponding owner roles for the child space being created: ``<parentcontext>/<parentspace>/<childspace>:ROLE_PROVIDER``.

The operation of user role management and space management may be performed either via API or through the AAC console.

Once the spaces are configured and the space roles are assigned, it is possible to use this information for the authorization. Specifically,

* it is possible to define the approval mapping function to read the ``spaceRoles`` claim and make the scope approval depend on a particular space roles;
* it is possible to perform the decisions based on the space roles outside AAC, using the user or client ``spaceRoles`` claim.

 
