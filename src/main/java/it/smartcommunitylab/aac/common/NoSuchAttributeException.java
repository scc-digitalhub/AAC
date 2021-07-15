package it.smartcommunitylab.aac.common;

public class NoSuchAttributeException extends Exception {

    private static final long serialVersionUID = -1338175240970345221L;

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