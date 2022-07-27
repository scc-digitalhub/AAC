package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchCredentialException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchCredentialException() {
        super();
    }

    public NoSuchCredentialException(String message) {
        super(message);
    }

    public NoSuchCredentialException(Throwable cause) {
        super(cause);
    }

    public NoSuchCredentialException(String message, Throwable cause) {
        super(message, cause);
    }

}
