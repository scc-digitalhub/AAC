package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchRealmException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchRealmException() {
        super();
    }

    public NoSuchRealmException(String message) {
        super(message);
    }

    public NoSuchRealmException(Throwable cause) {
        super(cause);
    }

    public NoSuchRealmException(String message, Throwable cause) {
        super(message, cause);
    }

}
