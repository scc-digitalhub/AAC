package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchTemplateException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchTemplateException() {
        super();
    }

    public NoSuchTemplateException(String message) {
        super(message);
    }

    public NoSuchTemplateException(Throwable cause) {
        super(cause);
    }

    public NoSuchTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
