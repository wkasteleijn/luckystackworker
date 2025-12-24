package nl.wilcokas.luckystackworker.exceptions;

import java.io.Serial;

public class BatchStoppedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2658531234567890223L;

    public BatchStoppedException(String message) {
        super(message);
    }
}
