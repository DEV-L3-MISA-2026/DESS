package mg.dess.scheduling.api;

import mg.dess.shared.domain.DomainException;

/** A requested resource is already booked (or too small) during the requested time range. */
public class BookingConflictException extends DomainException {

    public static final String CODE = "BOOKING_CONFLICT";

    private final transient ResourceRef conflictingResource;

    /** @param conflictingResource the resource in conflict, or null when it cannot be determined */
    public BookingConflictException(ResourceRef conflictingResource) {
        super(CODE, conflictingResource == null
                ? "A requested resource is already booked during the requested time range."
                : conflictingResource.type() + " " + conflictingResource.id()
                        + " is already booked during the requested time range.");
        this.conflictingResource = conflictingResource;
    }

    public ResourceRef conflictingResource() {
        return conflictingResource;
    }
}
