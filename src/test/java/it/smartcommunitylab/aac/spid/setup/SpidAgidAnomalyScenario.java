package it.smartcommunitylab.aac.spid.setup;

/**
 * Enumeration of SPID Anomaly Codes as defined by AgID technical specifications (v1.4).
 */
public enum SpidAgidAnomalyScenario {

    // --- ANOMALIE RICHIESTA (Status: VersionMismatch) ---

    /** Error nr09: SAML version mismatch (not 2.0). */
    CODE_09("urn:oasis:names:tc:SAML:2.0:status:VersionMismatch", null, "ErrorCode nr09",  false),

    // --- ANOMALIE RICHIESTA (Status: Requester) ---

    /** Error nr08: Signature of the AuthnRequest is missing or invalid. */
    CODE_08("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr08", false),

    /** Error nr11: ID of the AuthnRequest is missing or invalid. */
    CODE_11("urn:oasis:names:tc:SAML:2.0:status:Requester", null, "ErrorCode nr11",  false),

    /** Error nr12: Requested AuthnContext is not available or not supported by IdP. */
    CODE_12("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext", "ErrorCode nr12", false),

    /** Error nr13: Request denied - generic security failure. */
    CODE_13("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestDenied", "ErrorCode nr13",  false),

    /** Error nr14: Destination URL mismatch. */
    CODE_14("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr14",  false),

    /** Error nr15: IsPassive is set to true (not supported). */
    CODE_15("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:NoPassive", "ErrorCode nr15",  false),

    /** Error nr16: AssertionConsumerService validation failure. */
    CODE_16("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr16",  false),

    /** Error nr17: NameIDPolicy Format attribute error. */
    CODE_17("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr17",  false),

    /** Error nr18: AttributeConsumerServiceIndex error. */
    CODE_18("urn:oasis:names:tc:SAML:2.0:status:Requester", "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported", "ErrorCode nr18", false),

    // --- ANOMALIE RISPOSTA (Status: Responder) ---

    /** Error nr19: Authentication failed (wrong credentials multiple times). */
    CODE_19("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr19", true),

    /** Error nr20: Missing credentials for requested level. */
    CODE_20("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr20", true),

    /** Error nr21: User authentication timeout. */
    CODE_21("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr21", true),

    /** Error nr22: User denied consent to send attributes. */
    CODE_22("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr22",  true),

    /** Error nr23: User identity suspended or credentials blocked. */
    CODE_23("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr23", true),

    /** Error nr25: User cancelled process. */
    CODE_25("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr25",  true),

    /** Error nr30: Identity mismatch (e.g. Legal Person vs Natural Person). */
    CODE_30("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr30", true),

    // --- ANOMALIE RISPOSTA (Status: Responder) - Processi di Riuso Identità Pregresse ---

    /** Error nr27: User already exists. */
    CODE_27("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr27", false),

    /** Error nr28: Operation cancelled. */
    CODE_28("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr28", false),

    /** Error nr29: Identity not provided. */
    CODE_29("urn:oasis:names:tc:SAML:2.0:status:Responder", "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", "ErrorCode nr29", false);

    private final String samlTopLevelStatus;
    private final String samlSubStatus;
    private final String samlStatusMessage;
    private final boolean requiresSpecificReason;

    SpidAgidAnomalyScenario(String samlTopLevelStatus, String samlSubStatus, String samlStatusMessage, boolean requiresSpecificReason) {
        this.samlTopLevelStatus = samlTopLevelStatus;
        this.samlSubStatus = samlSubStatus;
        this.samlStatusMessage = samlStatusMessage;
        this.requiresSpecificReason = requiresSpecificReason;
    }

    public String getSamlTopLevelStatus() { return samlTopLevelStatus; }
    public String getSamlSubStatus() { return samlSubStatus; }
    public String getSamlStatusMessage() { return samlStatusMessage; }
    public boolean requiresSpecificReason() { return requiresSpecificReason; }
}
