-- Module academic : cohorts taught and examined together
CREATE TABLE student_group (
    id                 UUID PRIMARY KEY,
    code               VARCHAR(30) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    owning_unit_id     VARCHAR(100) NOT NULL,
    academic_period_id BIGINT NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_group_code_period UNIQUE (code, academic_period_id),
    CONSTRAINT fk_student_group_period FOREIGN KEY (academic_period_id)
        REFERENCES period (id) ON DELETE RESTRICT
);

CREATE INDEX ix_student_group_academic_period
    ON student_group (academic_period_id);

CREATE INDEX ix_student_group_owning_unit
    ON student_group (owning_unit_id);
