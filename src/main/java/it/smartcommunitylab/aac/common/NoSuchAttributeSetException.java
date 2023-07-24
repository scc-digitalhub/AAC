package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchAttributeSetException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchAttributeSetException() {
        super();
    }

    public NoSuchAttributeSetException(String message) {
        super(message);
    }

    public NoSuchAttributeSetException(Throwable cause) {
        super(cause);
    }

    public NoSuchAttributeSetException(String message, Throwable cause) {
        super(message, cause);
    }
}
