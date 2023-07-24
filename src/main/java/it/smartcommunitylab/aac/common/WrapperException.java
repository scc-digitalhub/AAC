package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class WrapperException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public WrapperException(String message, Throwable cause) {
        super(message, cause);
    }
}
