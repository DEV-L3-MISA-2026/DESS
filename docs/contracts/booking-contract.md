# Booking contract (scheduling <-> academic / assessment)

Agree this on Day 1. Package names may change, the contract must not.

```java
public enum BookableResourceType { ROOM, PERSON, STUDENT_GROUP }
public enum BookingOwnerType { TEACHING_SESSION, EXAM_SESSION }

public record ResourceRef(BookableResourceType type, UUID id) {}

public record BookingRequest(
    UUID ownerId,
    BookingOwnerType ownerType,
    List<ResourceRef> resources,
    Instant startsAt,
    Instant endsAt,
    Integer requiredCapacity          // proposed addition, nullable - see below
) {}

public interface BookingService {
    BookingReceipt reserve(BookingRequest request);
    void cancel(UUID bookingId);
}
```

## Time model

- `startsAt` must be before `endsAt`.
- Ranges are half-open `[start, end)`: a session ending at 10:00 does not conflict with one starting at 10:00.
- Instants are stored in UTC and displayed in the institution's configured time zone.

## Errors

- A conflict raises `BookingConflictException` (code `BOOKING_CONFLICT`), mapped to HTTP 409.
- `reserve` must `flush()` inside the method so a PostgreSQL exclusion violation (SQLState `23P01`)
  surfaces here and is translated, instead of failing later at commit.
- `reserve` joins the caller's transaction: if it fails, the caller's work is rolled back too.

## Proposed addition: `requiredCapacity`

`scheduling` does not know group sizes. The caller (`academic`, `assessment`) computes the number of
people and passes it as `requiredCapacity`; `scheduling` rejects the booking if a requested room is
too small. When `null`, the capacity check is skipped. **Confirm or reject on Day 1.**

## Database

One row per reserved resource in `booking_slot`; a partial exclusion constraint on
`(resource_type, resource_id, tstzrange(starts_at, ends_at, '[)'))` where `status = 'ACTIVE'`
rejects overlaps even under concurrent requests. A cancelled booking sets its slots to `CANCELLED`.
