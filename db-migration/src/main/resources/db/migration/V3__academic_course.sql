-- Module academic : catalogue des cours
CREATE TABLE course (
    id          UUID PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT course_code_unique UNIQUE (code)
);