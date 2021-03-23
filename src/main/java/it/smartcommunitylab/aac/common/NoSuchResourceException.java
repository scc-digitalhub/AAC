package it.smartcommunitylab.aac.common;

public class NoSuchResourceException extends Exception {

    private static final long serialVersionUID = -185899671739659871L;

    public NoSuchResourceException() {
        super();
    }

    public NoSuchResourceException(String message) {
        super(message);
    }

    public NoSuchResourceException(Throwable cause) {
        super(cause);
    }

    public NoSuchResourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
