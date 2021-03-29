package it.smartcommunitylab.aac.common;

public class NoSuchScopeException extends Exception {

    private static final long serialVersionUID = -185899671739659871L;

    public NoSuchScopeException() {
        super();
    }

    public NoSuchScopeException(String message) {
        super(message);
    }

    public NoSuchScopeException(Throwable cause) {
        super(cause);
    }

    public NoSuchScopeException(String message, Throwable cause) {
        super(message, cause);
    }

}
