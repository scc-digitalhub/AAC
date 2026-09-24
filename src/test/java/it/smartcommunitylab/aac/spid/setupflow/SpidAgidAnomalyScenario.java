package it.smartcommunitylab.aac.spid.setupflow;

/**
 * Enumeration of SPID Anomaly Codes as defined by AgID technical specifications (v1.4).
 */
public enum SpidAgidAnomalyScenario {

    // --- IDP & PROTOCOL ANOMALIES (SAML Status Failures) ---

    /** Error nr02: Identity Provider non disponibile. */
    CODE_02("urn:oasis:names:tc:SAML:2.0:status:Responder", null, "ErrorCode nr02"),

    /** Error nr03: Errore di sistema interno dell'IdP. */
    CODE_03("urn:oasis:names:tc:SAML:2.0:status:Responder", null, "ErrorCode nr03"),

    /** Error nr04: Errore di formato della richiesta (rilevato a monte dall'IdP). */
    CODE_04("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr04"),

    /** Error nr05: Verifica della firma dell'AuthnRequest fallita lato IdP. */
    CODE_05("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr05"),

    /** Error nr06: Richiesta scaduta rispetto al timestamp (IssueInstant). */
    CODE_06("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr06"),

    /** Error nr07: Errore di sistema generico dell'IdP. */
    CODE_07("urn:oasis:names:tc:SAML:2.0:status:Responder", null, "ErrorCode nr07"),

    /** Error nr10: Elementi strutturali o tag non conformi rilevati dall'IdP. */
    CODE_10("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr10"),

    // --- LOCAL REQUEST VALIDATION ANOMALIES (Status: VersionMismatch / Requester) ---

    /** Error nr09: SAML version mismatch (not 2.0). */
    CODE_09("urn:oasis:names:tc:SAML:2.0:status:VersionMismatch", null, "ErrorCode nr09"),

    /** Error nr08: Signature of the AuthnRequest is missing or invalid. */
    CODE_08("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr08"),

    /** Error nr11: ID of the AuthnRequest is missing or invalid. */
    CODE_11("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr11"),

    /** Error nr12: Requested AuthnContext is not available or not supported by IdP. */
    CODE_12("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext", "ErrorCode nr12"),

    /** Error nr13: Request denied - generic security failure. */
    CODE_13("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestDenied", "ErrorCode nr13"),

    /** Error nr14: Destination URL mismatch. */
    CODE_14("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr14"),

    /** Error nr15: IsPassive is set to true (not supported). */
    CODE_15("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:NoPassive", "ErrorCode nr15"),

    /** Error nr16: AssertionConsumerService validation failure. */
    CODE_16("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr16"),

    /** Error nr17: NameIDPolicy Format attribute error. */
    CODE_17("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr17"),

    /** Error nr18: AttributeConsumerServiceIndex error. */
    CODE_18("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr18"),

    // --- RESPONSE ANOMALIES (Status: Responder) - USER ERRORS ---

    /** Error nr19: Authentication failed (wrong credentials multiple times). */
    CODE_19("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr19"),

    /** Error nr20: Missing credentials for requested level. */
    CODE_20("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr20"),

    /** Error nr21: User authentication timeout. */
    CODE_21("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr21"),

    /** Error nr22: User denied consent to send attributes. */
    CODE_22("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr22"),

    /** Error nr23: User identity suspended or credentials blocked. */
    CODE_23("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr23"),

    /** Error nr25: User cancelled process. */
    CODE_25("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr25"),

    /** Error nr30: Identity mismatch (e.g. Legal Person vs Natural Person). */
    CODE_30("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr30"),

    // --- RESPONSE ANOMALIES (Status: Responder) - Previous Identity Reuse Processes ---

    /** Error nr27: User already exists. */
    CODE_27("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr27"),

    /** Error nr28: Operation cancelled. */
    CODE_28("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr28"),

    /** Error nr29: Identity not provided. */
    CODE_29("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr29");

    private final String samlTopLevelStatus;
    private final String samlSubStatus;
    private final String samlStatusMessage;

    SpidAgidAnomalyScenario(String samlTopLevelStatus, String samlSubStatus, String samlStatusMessage) {
        this.samlTopLevelStatus = samlTopLevelStatus;
        this.samlSubStatus = samlSubStatus;
        this.samlStatusMessage = samlStatusMessage;
    }

    public String getSamlTopLevelStatus() { return samlTopLevelStatus; }
    public String getSamlSubStatus() { return samlSubStatus; }
    public String getSamlStatusMessage() { return samlStatusMessage; }
}
