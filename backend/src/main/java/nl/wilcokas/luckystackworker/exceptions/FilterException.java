package nl.wilcokas.luckystackworker.exceptions;

import java.io.Serial;

public class FilterException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2758538234567890123L;

    public FilterException(String message) {
        super(message);
    }

}
