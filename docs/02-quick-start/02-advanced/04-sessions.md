# User sessions

In AAC only *users* can have a session after performing login. Each sessions is bounded to a specific identifier, and is managed internally by AAC to fulfill the needs of identifying valid users, reusing matching authentications inside a given realm, and finally supporting OpenID Connect session management.

## Login and authentication

Any user which requires interaction with the realm, either via *authorization request* or via *web/console access*, needs to validate its identity via *authentication*. The process is handled by *identity providers*, and requires users to perform a *login* process where they enter their credentials and receive in response an *authentication token*.

AAC binds *authentication tokens*, *subjects* and *users* into a specific *session*, which is given a unique random (and hard-to-guess) identifier and is then bound to the user agent either via cookie or via request parameter.

From now on, the *authenticated* user won't be asked to sign-in again until the sessions is either *expired* or *terminated*.

## Session expiration and renewal

Sessions are persisted in AAC and keep an immutable reference to their *creation time*, which is usually the same as login, and their *duration*, which enables the system to calculate an *expiration time*. 
When a session expires, AAC invalidates the identifier, drops all the associated information and performs the *logout* from the interface. At the same time, every pending *authorization request* (for example an OAuth2 code redemption) will be invalidated.

Users which are *active*, which means they performed some kind of interaction with either the web interface or the developer console, can *extend* the duration of their session, effectively moving forward the session's expiration. This mechanism is subject to security, architectural and usability requirements and is not customizable by administrators.


## OpenID session management

The OpenID Connect framework contains a specific set of specifications for session management, single logout and front/back channel logout. While AAC aims to fully support all these standards, as of now the implementation is limited to a partial front channel *single logout* approach.

AAC exposes an `/endsession` endpoint, which when instructed from a valid client with an `id_token` matching the current user session, will instruct AAC to perform global logout.

The global logout terminates the user session at AAC, and will eventually signal to all the clients which obtained a token from the same session that the user is no longer connected. 

This section will be updated with details when AAC will fully support the standards.
