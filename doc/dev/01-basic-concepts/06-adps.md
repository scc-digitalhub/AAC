# Attribute providers

User identities are composed of *accounts* and *attributes*, which are properties describing the user, such as *name, surname, email address*...

Attributes are retrieved, processed and collected in attribute sets by attribute *providers*, which handle the process either at login or at retrieval time.

In order to retrieve attributes, providers receive the *subject identifier*, along with any information collected via authentication from the *identity provider*. By performing mapping, conversion, external calls or translations, providers can organize and derive new attributes which will then be associated to the *identity* (at login) or the *subject* (any other case).

