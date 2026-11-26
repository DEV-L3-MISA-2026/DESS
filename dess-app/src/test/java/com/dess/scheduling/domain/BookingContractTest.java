package com.dess.scheduling.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.dess.scheduling.application.BookingRequest;

class BookingContractTest
{
    @Test
    void createsValidRequestAndNormalizesInstantsToUtc()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-23T09:00:00+03:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-11-23T11:00:00+03:00");

        BookingRequest request = new BookingRequest("EXAM_SESSION", UUID.randomUUID(),
                start, end, List.of(ResourceRef.room(UUID.randomUUID())));

        assertEquals(ZoneOffset.UTC, request.startsAt().getOffset());
        assertEquals(ZoneOffset.UTC, request.endsAt().getOffset());
        assertEquals(start.withOffsetSameInstant(ZoneOffset.UTC), request.startsAt());
        assertEquals(end.withOffsetSameInstant(ZoneOffset.UTC), request.endsAt());
    }

    @Test
    void acceptsAllThreeFrozenResourceTypes()
    {
        List<ResourceRef> resources = List.of(
                ResourceRef.room(UUID.randomUUID()),
                ResourceRef.personId(12L),
                ResourceRef.studentGroup(UUID.randomUUID()));

        BookingRequest request = new BookingRequest("SESSION", UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-23T09:00:00Z"),
                OffsetDateTime.parse("2026-11-23T11:00:00Z"), resources);

        assertEquals(3, request.resources().size());
        assertEquals(ResourceType.ROOM, request.resources().get(0).type());
        assertEquals(ResourceType.PERSON, request.resources().get(1).type());
        assertEquals(ResourceType.STUDENT_GROUP, request.resources().get(2).type());
    }

    @Test
    void samePersonIdAlwaysMapsToSameResourceId()
    {
        ResourceRef first = ResourceRef.personId(42L);
        ResourceRef second = ResourceRef.personId(42L);

        assertEquals(first, second);
        assertNotEquals(first, ResourceRef.personId(43L));
    }

    @Test
    void requestKeepsItsOwnCopyOfResources()
    {
        List<ResourceRef> resources = new ArrayList<>();
        resources.add(ResourceRef.room(UUID.randomUUID()));

        BookingRequest request = new BookingRequest("SESSION", UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-23T09:00:00Z"),
                OffsetDateTime.parse("2026-11-23T11:00:00Z"), resources);
        resources.clear();

        assertEquals(1, request.resources().size());
    }

    @Test
    void rejectsEmptyResourceList()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new BookingRequest("SESSION", UUID.randomUUID(),
                        OffsetDateTime.parse("2026-11-23T09:00:00Z"),
                        OffsetDateTime.parse("2026-11-23T11:00:00Z"), List.of()));

        assertEquals("at least one resource is required", exception.getMessage());
    }

    @Test
    void rejectsNullResource()
    {
        List<ResourceRef> resources = new ArrayList<>();
        resources.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> new BookingRequest("SESSION", UUID.randomUUID(),
                        OffsetDateTime.parse("2026-11-23T09:00:00Z"),
                        OffsetDateTime.parse("2026-11-23T11:00:00Z"), resources));
    }

    @Test
    void rejectsZeroLengthRange()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-23T09:00:00Z");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new BookingRequest("SESSION", UUID.randomUUID(), start, start,
                        List.of(ResourceRef.room(UUID.randomUUID()))));

        assertEquals("startsAt must be before endsAt", exception.getMessage());
    }

    @Test
    void rejectsBlankOwnerType()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new BookingRequest("  ", UUID.randomUUID(),
                        OffsetDateTime.parse("2026-11-23T09:00:00Z"),
                        OffsetDateTime.parse("2026-11-23T11:00:00Z"),
                        List.of(ResourceRef.room(UUID.randomUUID()))));
    }

    @Test
    void rejectsNullResourceRefParts()
    {
        assertThrows(NullPointerException.class, () -> new ResourceRef(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> new ResourceRef(ResourceType.ROOM, null));
        assertTrue(ResourceType.valueOf("ROOM") == ResourceType.ROOM);
    }
}
