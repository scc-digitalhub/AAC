package it.smartcommunitylab.aac.common;

public class NoSuchProviderException extends Exception {

    private static final long serialVersionUID = 6002273173583990343L;

    public NoSuchProviderException() {
        super();
    }

    public NoSuchProviderException(String message) {
        super(message);
    }

    public NoSuchProviderException(Throwable cause) {
        super(cause);
    }

    public NoSuchProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}