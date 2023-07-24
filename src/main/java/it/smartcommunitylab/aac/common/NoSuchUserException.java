package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchUserException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchUserException() {
        super();
    }

    public NoSuchUserException(String message) {
        super(message);
    }

    public NoSuchUserException(Throwable cause) {
        super(cause);
    }

    public NoSuchUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
