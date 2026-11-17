package mg.dess.platform.error;

/** The one error format returned by the API. */
public record ApiError(String code, String message) {
}
