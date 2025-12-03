package nl.wilcokas.luckystackworker.exceptions;

import java.io.Serial;

public class DeRotationStoppedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2658531234567890223L;

    public DeRotationStoppedException(String message) {
        super(message);
    }
}
