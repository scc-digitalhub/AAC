package it.smartcommunitylab.aac.spid.setupflow;

import org.springframework.test.web.servlet.MockMvc;

/**
 * Configuration context for executing tests on SPID authentication flows and AgID anomaly scenarios.
 * Implements the Builder pattern to ensure clean and immutable instantiation.
 */
public class SpidAgidErrorContextBuilder {

    private final MockMvc mockMvc;
    private final String xmlTemplate;
    private final String xmlAgidErrorTemplate;
    private final String baseUrlAac;
    private final String userDestinationUrl;
    private final String authenticatePath;
    private final String loginDestinationUrl;
    private final String signingIdpSsoUrl;
    private final String registrationId;
    private final String assertingPartyEntityId;
    private final String entityIdAac;
    private final String idpPrivateKey;
    private final String idpCertificate;
    private final Boolean activePostBinding;

    private SpidAgidErrorContextBuilder(Builder builder) {
        this.mockMvc = builder.mockMvc;
        this.xmlTemplate = builder.xmlTemplate;
        this.xmlAgidErrorTemplate = builder.xmlAgidErrorTemplate;
        this.baseUrlAac = builder.baseUrlAac;
        this.userDestinationUrl = builder.userDestinationUrl;
        this.authenticatePath = builder.authenticatePath;
        this.loginDestinationUrl = builder.loginDestinationUrl;
        this.signingIdpSsoUrl = builder.signingIdpSsoUrl;
        this.registrationId = builder.registrationId;
        this.assertingPartyEntityId = builder.assertingPartyEntityId;
        this.entityIdAac = builder.entityIdAac;
        this.idpPrivateKey = builder.idpPrivateKey;
        this.idpCertificate = builder.idpCertificate;
        this.activePostBinding = builder.activePostBinding;
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getters ---
    public MockMvc getMockMvc() { return mockMvc; }
    public String getXmlTemplate() { return xmlTemplate; }
    public String getXmlAgidErrorTemplate() { return xmlAgidErrorTemplate; }
    public String getBaseUrlAac() { return baseUrlAac; }
    public String getUserDestinationUrl() { return userDestinationUrl; }
    public String getAuthenticatePath() { return authenticatePath; }
    public String getLoginDestinationUrl() { return loginDestinationUrl; }
    public String getSigningIdpSsoUrl() { return signingIdpSsoUrl; }
    public String getRegistrationId() { return registrationId; }
    public String getAssertingPartyEntityId() { return assertingPartyEntityId; }
    public String getEntityIdAac() { return entityIdAac; }
    public String getIdpPrivateKey() { return idpPrivateKey; }
    public String getIdpCertificate() { return idpCertificate; }
    public Boolean getActivePostBinding() { return activePostBinding; }

    // --- Static Builder ---
    public static class Builder {
        private MockMvc mockMvc;
        private String xmlTemplate;
        private String xmlAgidErrorTemplate;
        private String baseUrlAac;
        private String userDestinationUrl;
        private String authenticatePath;
        private String loginDestinationUrl;
        private String signingIdpSsoUrl;
        private String registrationId;
        private String assertingPartyEntityId;
        private String entityIdAac;
        private String idpPrivateKey;
        private String idpCertificate;
        private Boolean activePostBinding;

        public Builder mockMvc(MockMvc mockMvc) { this.mockMvc = mockMvc; return this; }
        public Builder xmlTemplate(String xmlTemplate) { this.xmlTemplate = xmlTemplate; return this; }
        public Builder xmlAgidErrorTemplate(String xmlAgidErrorTemplate) { this.xmlAgidErrorTemplate = xmlAgidErrorTemplate; return this; }
        public Builder baseUrlAac(String baseUrlAac) { this.baseUrlAac = baseUrlAac; return this; }
        public Builder userDestinationUrl(String userDestinationUrl) { this.userDestinationUrl = userDestinationUrl; return this; }
        public Builder authenticatePath(String authenticatePath) { this.authenticatePath = authenticatePath; return this; }
        public Builder loginDestinationUrl(String loginDestinationUrl) { this.loginDestinationUrl = loginDestinationUrl; return this; }
        public Builder signingIdpSsoUrl(String signingIdpSsoUrl) { this.signingIdpSsoUrl = signingIdpSsoUrl; return this; }
        public Builder registrationId(String registrationId) { this.registrationId = registrationId; return this; }
        public Builder assertingPartyEntityId(String assertingPartyEntityId) { this.assertingPartyEntityId = assertingPartyEntityId; return this; }
        public Builder entityIdAac(String entityIdAac) { this.entityIdAac = entityIdAac; return this; }
        public Builder idpPrivateKey(String idpPrivateKey) { this.idpPrivateKey = idpPrivateKey; return this; }
        public Builder idpCertificate(String idpCertificate) { this.idpCertificate = idpCertificate; return this; }
        public Builder activePostBinding(Boolean activePostBinding) { this.activePostBinding = activePostBinding; return this; }

        public SpidAgidErrorContextBuilder build() {
            return new SpidAgidErrorContextBuilder(this);
        }
    }
}
