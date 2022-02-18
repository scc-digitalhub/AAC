# Custom Services (APIs)

Backend services, or APIs, can define a representation in AAC useful to manage resources and scopes, with the objective to properly separate frontend and backend clients and to apply authorization restrictions to resource access.

When adding an external API, independently developed and deployed at a given address, AAC lets developers define a *namespace* which uniquely identifies such service, and then enables them in describing actions via *scopes*, which should individuate meaningful operations on resources.

At access time, clients will request users to *approve* scopes, and then carry such authorization via *access tokens* to the proper backend, which will be able to verify and compare the *claims* according to a local policy.

