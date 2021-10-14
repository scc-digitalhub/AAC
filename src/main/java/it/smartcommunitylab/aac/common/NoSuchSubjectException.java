package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchSubjectException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchSubjectException() {
        super();
    }

    public NoSuchSubjectException(String message) {
        super(message);
    }

    public NoSuchSubjectException(Throwable cause) {
        super(cause);
    }

    public NoSuchSubjectException(String message, Throwable cause) {
        super(message, cause);
    }

}
