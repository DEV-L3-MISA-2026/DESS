-- ============================================================================
-- V20261121_1200 — Module assessment : définitions et résultats
--
-- Objectif : représenter les définitions d'évaluation et les résultats
--             des étudiants, comme prévu par docs/PMP.md (lot C) et
--             docs/architecture_farany.md (§5, §10).
--
-- Correspondance domaine -> tables :
--   Assessment -> assessment
--   Result     -> assessment_result
--
-- Conventions (AGENTS.md) :
--   * Commentaires en français, identifiants SQL en anglais.
--   * Contraintes nommées pk_ / fk_ / uq_ / ck_.
--   * Statuts stockés en varchar avec CHECK, sans enum PostgreSQL.
--   * Les notes sont des décimaux exacts (numeric), jamais des flottants.
--
-- Règles garanties par la base :
--   * Un étudiant a au plus un résultat par assessment
--     (uq_assessment_result_assessment_student).
--   * Un résultat SCORED porte une note, un résultat ABSENT ou EXCUSED
--     n'en porte pas (ck_assessment_result_score_status).
--   * La note, quand elle existe, est positive
--     (ck_assessment_result_score). Sa borne haute est le maximum de
--     l'assessment : elle relève de l'application, qui connaît
--     l'assessment, et ne peut pas s'exprimer en CHECK.
--   * Un résultat référence un étudiant existant
--     (fk_assessment_result_student) et un assessment existant
--     (fk_assessment_result_assessment).
--   * Un assessment référence un cours existant
--     (fk_assessment_course). Il n'y a pas encore de table
--     course_offering, donc pas de clé vers une offre de cours.
--   * Il n'y a pas de clé vers exam_session : la table n'existe pas
--     encore sur dev. L'identifiant de session est enregistré tel
--     quel dans assessment_result.exam_session_id.
-- ============================================================================

CREATE TABLE assessment (
    id            UUID          PRIMARY KEY,
    course_id     UUID          NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    type          VARCHAR(20)   NOT NULL,
    maximum_score NUMERIC(6, 2) NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_assessment_course FOREIGN KEY (course_id)
        REFERENCES course (id),
    CONSTRAINT ck_assessment_type CHECK (type IN ('EXAM', 'TEST', 'PRACTICAL')),
    CONSTRAINT ck_assessment_maximum_score CHECK (maximum_score > 0)
);

CREATE TABLE assessment_result (
    id                UUID          PRIMARY KEY,
    assessment_id     UUID          NOT NULL,
    exam_session_id   UUID,
    student_person_id BIGINT        NOT NULL,
    score             NUMERIC(6, 2),
    status            VARCHAR(20)   NOT NULL,
    entered_by        VARCHAR(200)  NOT NULL,
    entered_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_assessment_result_assessment FOREIGN KEY (assessment_id)
        REFERENCES assessment (id),
    CONSTRAINT fk_assessment_result_student FOREIGN KEY (student_person_id)
        REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT uq_assessment_result_assessment_student
        UNIQUE (assessment_id, student_person_id),
    CONSTRAINT ck_assessment_result_status
        CHECK (status IN ('SCORED', 'ABSENT', 'EXCUSED')),
    CONSTRAINT ck_assessment_result_score
        CHECK (score IS NULL OR score >= 0),
    CONSTRAINT ck_assessment_result_score_status CHECK (
        (status = 'SCORED' AND score IS NOT NULL)
        OR (status IN ('ABSENT', 'EXCUSED') AND score IS NULL)
    )
);

CREATE INDEX ix_assessment_course ON assessment (course_id);
CREATE INDEX ix_assessment_result_assessment ON assessment_result (assessment_id);
CREATE INDEX ix_assessment_result_student ON assessment_result (student_person_id);
