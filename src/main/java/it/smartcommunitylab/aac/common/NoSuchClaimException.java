package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchClaimException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchClaimException() {
        super();
    }

    public NoSuchClaimException(String message) {
        super(message);
    }

    public NoSuchClaimException(Throwable cause) {
        super(cause);
    }

    public NoSuchClaimException(String message, Throwable cause) {
        super(message, cause);
    }

}
