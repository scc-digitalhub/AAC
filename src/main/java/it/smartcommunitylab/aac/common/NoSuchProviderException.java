package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchProviderException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchProviderException() {
        super();
    }

    public NoSuchProviderException(String message) {
        super(message);
    }

    public NoSuchProviderException(Throwable cause) {
        super(cause);
    }

    public NoSuchProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
