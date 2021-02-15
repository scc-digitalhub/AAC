package it.smartcommunitylab.aac.common;

public class NoSuchClientException extends Exception {

    private static final long serialVersionUID = -7126728527383722454L;

    public NoSuchClientException() {
        super();
    }

    public NoSuchClientException(String message) {
        super(message);
    }

    public NoSuchClientException(Throwable cause) {
        super(cause);
    }

    public NoSuchClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
