package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchClientException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchClientException() {
        super();
    }

    public NoSuchClientException(String message) {
        super(message);
    }

    public NoSuchClientException(Throwable cause) {
        super(cause);
    }

    public NoSuchClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
