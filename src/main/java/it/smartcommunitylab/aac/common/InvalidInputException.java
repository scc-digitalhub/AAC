package it.smartcommunitylab.aac.common;

public class InvalidInputException extends RuntimeException {

    private static final long serialVersionUID = -7273689058090546155L;

    public InvalidInputException() {
        super();
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(Throwable cause) {
        super(cause);
    }

}
