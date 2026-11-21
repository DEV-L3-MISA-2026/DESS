package com.dess.scheduling.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * A reference to one concrete resource that a booking reserves.
 *
 * <p>The type is one of the frozen {@link ResourceType} values and the
 * id is the identifier of the concrete resource, for example the id of
 * a room or the id of a person. The pair (type, id) is what the
 * overlap check uses, so two bookings of the same resource can never
 * overlap in time.</p>
 *
 * <p>People are identified by a numeric Person id in the people
 * module, while booking slots store a UUID. The {@link #personId(long)}
 * factory maps a Person id to one stable UUID, so the same person
 * always maps to the same resource and the overlap check works for
 * invigilators and students. The mapping never changes.</p>
 */
public record ResourceRef(ResourceType type, UUID id)
{
    public ResourceRef
    {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(id, "id is required");
    }

    /** Creates a reference to a room. */
    public static ResourceRef room(UUID roomId)
    {
        return new ResourceRef(ResourceType.ROOM, roomId);
    }

    /** Creates a reference to one person, from a resource UUID. */
    public static ResourceRef person(UUID personResourceId)
    {
        return new ResourceRef(ResourceType.PERSON, personResourceId);
    }

    /**
     * Creates a reference to one person, from a numeric Person id.
     * The same Person id always yields the same resource id.
     */
    public static ResourceRef personId(long personId)
    {
        String key = "dess:person:" + personId;
        return new ResourceRef(ResourceType.PERSON,
                UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
    }

    /** Creates a reference to a student group. */
    public static ResourceRef studentGroup(UUID groupId)
    {
        return new ResourceRef(ResourceType.STUDENT_GROUP, groupId);
    }

    /** Returns the type name as stored on a booking slot. */
    public String typeName()
    {
        return type.name();
    }
}
