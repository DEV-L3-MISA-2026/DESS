-- Module academic : cohorts taught and examined together
CREATE TABLE student_group (
    id                 UUID PRIMARY KEY,
    code               VARCHAR(30) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    owning_unit_id     UUID NOT NULL,
    academic_period_id UUID NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT student_group_code_period_unique UNIQUE (code, academic_period_id)
);

CREATE INDEX idx_student_group_academic_period_id
    ON student_group (academic_period_id);

CREATE INDEX idx_student_group_owning_unit_id
    ON student_group (owning_unit_id);
