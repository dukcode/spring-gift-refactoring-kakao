package gift.common;

public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
