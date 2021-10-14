package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchAttributeException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchAttributeException() {
        super();
    }

    public NoSuchAttributeException(String message) {
        super(message);
    }

    public NoSuchAttributeException(Throwable cause) {
        super(cause);
    }

    public NoSuchAttributeException(String message, Throwable cause) {
        super(message, cause);
    }
}