package nl.wilcokas.luckystackworker.exceptions;

import java.io.Serial;

public class DeRotationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2658531234567890223L;

    public DeRotationException(String message) {
        super(message);
    }
}
