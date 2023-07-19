package nl.wilcokas.luckystackworker.exceptions;

public class ProfileNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 5965809592319933012L;

    public ProfileNotFoundException(String message) {
        super(message);
    }

}
