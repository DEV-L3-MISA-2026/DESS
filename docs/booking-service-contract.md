# BookingService contract, frozen

Status: frozen on 2026-11-20 for the DESS MVP.
Owners: scheduling (Miranto, Vahatra) and assessment (Christian, Fabien).
Code: `BookingService`, `BookingRequest`, `ResourceRef`, `ResourceType`.

## What a booking is

A booking reserves one or more resources for one owner and one time
range. The owner is a teaching session or an exam session. The
booking is the only way to reserve a room, a person or a student
group, so a course and an exam can never use the same resource at the
same time.

## Frozen rules

1. Resource types are exactly ROOM, PERSON and STUDENT_GROUP.
   No other type is accepted by the typed API. Adding a type needs an
   agreement between the scheduling owners and the assessment owners.
2. Time ranges are half open, [startsAt, endsAt). A booking that
   starts exactly when another one ends does not overlap it. A zero
   length range is rejected, startsAt must be before endsAt.
3. Instants are stored in UTC. `BookingRequest` converts any offset
   to UTC on construction. The database stores TIMESTAMPTZ.
4. At least one resource is required. Resources carry a typed
   reference, never a bare string, through the typed API.
5. Creating a booking is atomic. Either every resource is reserved,
   or nothing is reserved and the call fails with a clear message.
6. Cancelling a booking frees all of its slots. Cancelled slots never
   block a new booking.
7. Owner types are "SESSION" for a teaching session and
   "EXAM_SESSION" for an exam session. See the constants on
   `BookingService`.

## People

People are identified by a numeric Person id in the people module,
while booking slots store a UUID. `ResourceRef.personId(long)` maps a
Person id to one stable UUID, so the same person always maps to the
same resource. That mapping never changes. It is used for
invigilators and for students booked as individuals.

## Enforcement

The database enforces the same rules: the booking table rejects an
empty or reversed range, and the booking_slot table has an exclusion
constraint on `tstzrange(starts_at, ends_at, '[)')` for ACTIVE
slots of the same resource. The application check
(`hasOverlap`, `startsAt < endsAt and endsAt > startsAt`) mirrors the
half open rule, so the application and the database agree.

## What is not in the contract

The contract says nothing about who may book, about prices, or about
recurring bookings. Assessment definitions, eligibility and results
belong to the assessment module. Room management belongs to the
scheduling module. Any change to this document needs the agreement of
both module owner pairs.
