package mg.dess.academic.api;

import java.util.UUID;

/** Published read interface about course offerings. */
public interface OfferingQueries {

    boolean exists(UUID offeringId);
}
