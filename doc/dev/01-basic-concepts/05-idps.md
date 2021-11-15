# Identity providers

Identity providers are the entities responsible for providing AAC with user identities, and also for validating such identities via authentication.

User are able to obtain a valid session in AAC, bounded to a *subject*, by presenting a valid *identity* authenticated by a trusted *identity provider*. 
As always, every realm will possess a list of trusted *idps*, in a fully isolated approach.

In order to provide users with a valid identity, providers need to:

* model and return a valid *identity* based on an *account*
* retrieve and process properties as user *attributes*
* perform an authentication process and return an *authentication token* 


At the end of the authentication process, users will be provided with a session bounded to a specific *subject*, containing information about all the linked identities, along with attributes, and validated by the authentication tokens available after the login.

Do note that for users in possess of multiple linked identities, any login via one of those will result in a session containing all the linked identities, but only the authentication tokens resulting from the login with the selected provider. The result is the ability to both leverage all the information at token release time, and also to discriminate between security and trust levels thanks to the authentication tokens.

