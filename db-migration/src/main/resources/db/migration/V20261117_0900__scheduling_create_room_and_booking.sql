-- scheduling: rooms and bookings, with database-enforced conflict prevention (ADR-003).
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE room (
    id        UUID         PRIMARY KEY,
    code      VARCHAR(50)  NOT NULL UNIQUE,
    name      VARCHAR(200) NOT NULL,
    capacity  INTEGER      NOT NULL CHECK (capacity > 0)
);

-- One booking per session; it reserves one or more resources (booking_slot).
CREATE TABLE booking (
    id          UUID         PRIMARY KEY,
    owner_type  VARCHAR(30)  NOT NULL,
    owner_id    UUID         NOT NULL,
    starts_at   TIMESTAMPTZ  NOT NULL,
    ends_at     TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT booking_range_valid CHECK (starts_at < ends_at)
);

CREATE INDEX booking_owner_idx ON booking (owner_type, owner_id);

-- One row per reserved resource. Time range and status are copied from the booking so that
-- the partial exclusion constraint can see them (a cancelled booking frees its slots).
CREATE TABLE booking_slot (
    id             UUID         PRIMARY KEY,
    booking_id     UUID         NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
    resource_type  VARCHAR(30)  NOT NULL,
    resource_id    UUID         NOT NULL,
    starts_at      TIMESTAMPTZ  NOT NULL,
    ends_at        TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT booking_slot_range_valid CHECK (starts_at < ends_at),
    CONSTRAINT booking_slot_no_overlap EXCLUDE USING gist (
        resource_type WITH =,
        resource_id   WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status = 'ACTIVE')
);

CREATE INDEX booking_slot_booking_idx ON booking_slot (booking_id);
