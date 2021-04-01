package it.smartcommunitylab.aac.common;

public class NoSuchRealmException extends Exception {

    private static final long serialVersionUID = -185899671739659871L;

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
