package it.smartcommunitylab.aac.common;

public class NoSuchClaimException extends Exception {

    private static final long serialVersionUID = -185899671739659871L;

    public NoSuchClaimException() {
        super();
    }

    public NoSuchClaimException(String message) {
        super(message);
    }

    public NoSuchClaimException(Throwable cause) {
        super(cause);
    }

    public NoSuchClaimException(String message, Throwable cause) {
        super(message, cause);
    }

}
