package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchAuthorityException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchAuthorityException() {
        super();
    }

    public NoSuchAuthorityException(String message) {
        super(message);
    }

    public NoSuchAuthorityException(Throwable cause) {
        super(cause);
    }

    public NoSuchAuthorityException(String message, Throwable cause) {
        super(message, cause);
    }
}