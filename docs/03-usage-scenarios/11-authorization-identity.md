# Using identity information to implement authorization policies

In some situation the authorization policy may become more sophisticated and dynamic for the standard solutions like OAuth2.0 permissions/scopes. This is particularly the case of the system, where the decision depends on some domain-specific information that cannot be stored inside AAC as the user/app attributes. Consider an example, the cases where the decision depends on the properties of the business client organization and its business subscription. Such information is stored in the protected service itself, while AAC provides the information about the user that operates on behalf of the business client. Here the authorization is performed by the service using the user attributes (e.g., the organization email domain) and the subscription info stored with the service data.


## Overview

To accomplish the implementation of such authorization, it is important to obtain the necessary user/app identity information in the form required by the protected service. This may be accomplished in the following ways:

* define custom *attribute sets* to model the relevant user/app properties. Define the appropriate *attribute providers* for these attributes. As described [here](../02-quick-start/02-advanced/02-attributes.md), there are different ways to define and extract user properties, e.g., from Identity Provider, external service, or explicitly store them within AAC. 
* use *space roles* for the definition of specific user data.
* define *custom mapping function* at the level of client application that, given different user attributes, provides a set of   additional custom attributes based on the existing user attributes.

Once defined, the process of authorization implementation on the side of the protected service is based on the following steps:

1. Perform user authentication. It is necessary to ask for the permissions to access the necessary entity attributes; 
2. Obtain required user attributes. Based on implementation, these attributes may be extracted from the JWT token or using the user information APIs exposed by AAC.
3. Use the custom attributes to implement the policy decision point.

## Custom Attribute Mapping

When the target service expect certain attributes to be provide in a specific way, it is possible to provide a custom mapping definition at the level of the AAC client app that converts the existing user attributes into this specific format. This is, e.g.,  a case for some existing or legacy software that define their own requirements on how the user identity data should be provided. 

Such custom mapping may be defined as a JavaScript function in the AAC client app management console. More specifically, the function defines a conversion operation that takes as input available authorized user / app claims and adds new claims based on their values. AAC provides also a test sandbox where this function may be applied to the claims of the currently authenticated user. 

Please note that it is not possible for the function to override the standard claims or the claims defined by other attribute sets. Such an attempt will be ignored by the claim mapping pipeline.
