-- assessment: exam sessions, their candidates and their invigilators.
-- Flyway owns this schema. The JPA mapping must not create or update these tables.
CREATE TABLE exam_session (
    id            UUID        PRIMARY KEY,
    assessment_id UUID        NOT NULL,
    room_id       UUID        NOT NULL,
    starts_at     TIMESTAMPTZ NOT NULL,
    ends_at       TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT exam_session_range_valid CHECK (starts_at < ends_at)
);

CREATE INDEX exam_session_starts_at_idx ON exam_session (starts_at DESC);

-- Person ids are the numeric ids of the people module, not bookable resource UUIDs.
CREATE TABLE exam_session_candidate (
    session_id UUID   NOT NULL REFERENCES exam_session (id) ON DELETE CASCADE,
    person_id  BIGINT NOT NULL,
    position   INTEGER NOT NULL,
    PRIMARY KEY (session_id, person_id)
);

CREATE TABLE exam_session_invigilator (
    session_id UUID   NOT NULL REFERENCES exam_session (id) ON DELETE CASCADE,
    person_id  BIGINT NOT NULL,
    position   INTEGER NOT NULL,
    PRIMARY KEY (session_id, person_id)
);
