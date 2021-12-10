# UI customization

AAC should appear to users as a recognizable, trusted identity provider.
This means that the overall theme, the form layout and the login/authorization flows should be consistent between clients and realms.

By providing a quickly identifiable appearance, the platform can ensure that users will build trust in the IdP and develop the skill needed to identify naive spoofing attempts. While the security measures needed to ensure trust are more complex (https, EV certificates, urls etc), consistency in the login phase is a requirement in building users' confidence.

Given the global frame, certain parts of the user interface can be **customized** per *realm* or per *client*, for example the header/footer combo or the application name and logo. When properly done, such customizations will increase the trustiness of the system, bridging the gap between user facing applications and the IdP interface.

It is mandatory to ensure that every user interaction aimed at transmitting personal and sensitive information is visually recognizable and easily linked to the application requesting the data, for example by including the application name in the approval form.

## Realm

A realm describes a group of applications, managed by the same partner, all sharing a common configuration. As such, it is advisable to let developers customize some graphical elements to properly transmit the *tenant* identity to end users. 
All elements personalized at the realm level are shared between applications registered inside the given realm.


List of elements customizable *per realm*:

* global: realm name
* global: realm logo
* layout: header text
* layout: footer text
* about page: realm description 
* about page: realm info/contacts (web page, mail etc)
* privacy policy: realm contacts added to platform handlers
* registration: enable/disable
* (global: languages enabled)


## Client

A single client is usually connected to a single user-facing application, and optionally to an API backend serving such app. 

The level of customization offered aims at personalizing the user-facing pages of the AAC UI, by adding descriptive texts, explanation section and application links.
More importantly, the client should expose an identity to users, enabling them in recognizing the application. The objective is to enable users in distinguishing clients inside the same realm, giving them the ability to properly choose what personal data share with each distinct client.

List of elements customizable *per client*:

* global: client name
* global: client description
* global: client info URI
* global: client logo
* login: custom message
* login: provider list (with custom order)
* login: enable/disable link to registration (registration is enabled at realm level since clients share the user database, we can only hide the link)
* registration: custom message
* registration: list of extra attributes included in form
* registration: confirmation message/mail
* MFA: custom message
* MFA: factor list (with custom order)

