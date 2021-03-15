package it.smartcommunitylab.aac.common;

public class NoSuchAttributeSetException extends Exception {

    private static final long serialVersionUID = -1338175240970345221L;

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