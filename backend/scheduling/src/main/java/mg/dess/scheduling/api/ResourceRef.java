package mg.dess.scheduling.api;

import java.util.Objects;
import java.util.UUID;

/** A reference to something that can be booked. scheduling only knows the type and the id. */
public record ResourceRef(BookableResourceType type, UUID id) {

    public ResourceRef {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }
}
