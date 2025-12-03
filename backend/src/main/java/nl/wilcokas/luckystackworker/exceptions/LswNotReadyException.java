package nl.wilcokas.luckystackworker.exceptions;

public class LswNotReadyException extends RuntimeException {
    public LswNotReadyException(String message) {
        super(message);
    }
}
