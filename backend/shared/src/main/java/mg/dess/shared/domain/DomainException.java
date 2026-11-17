package mg.dess.shared.domain;

/**
 * Base class for business-rule violations. The code is stable and meant for API clients;
 * the message is meant for humans.
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
