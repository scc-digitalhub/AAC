package it.smartcommunitylab.aac.common;

public class NoSuchUserException extends Exception {

    private static final long serialVersionUID = -185899671739659871L;

    public NoSuchUserException() {
        super();
    }

    public NoSuchUserException(String message) {
        super(message);
    }

    public NoSuchUserException(Throwable cause) {
        super(cause);
    }

    public NoSuchUserException(String message, Throwable cause) {
        super(message, cause);
    }

}
