-- ============================================================================
-- V20261126_1200 — Module assessment : durée et historique des corrections
--
-- Objectif : ajouter la durée d'une évaluation (issue #105) et garder
--             l'historique complet des corrections de résultats
--             (issue #108), comme prévu par docs/assessment-module.md.
--
-- Correspondance domaine -> tables :
--   Assessment.durationMinutes -> assessment.duration_minutes
--   ResultCorrection           -> assessment_result_correction
--
-- Conventions (AGENTS.md) :
--   * Commentaires en français, identifiants SQL en anglais.
--   * Contraintes nommées pk_ / fk_ / uq_ / ck_.
--   * Statuts stockés en varchar avec CHECK, sans enum PostgreSQL.
--   * Les notes sont des décimaux exacts (numeric), jamais des flottants.
--
-- Règles garanties par la base :
--   * La durée est un nombre de minutes strictement positif
--     (ck_assessment_duration). Les lignes créées avant cette
--     migration reçoivent 60 minutes, uniquement pour remplir la
--     colonne ; le défaut est retiré aussitôt, donc toute nouvelle
--     ligne doit fournir sa durée.
--   * Une entrée de correction garde la valeur avant et après la
--     correction, qui l'a faite et quand (assessment_result_correction).
--     Les règles score/statut sont les mêmes que pour
--     assessment_result (ck_assessment_result_correction_score_status).
--   * Supprimer un résultat supprime son historique
--     (fk_assessment_result_correction_result ON DELETE CASCADE).
-- ============================================================================

ALTER TABLE assessment ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 60;
ALTER TABLE assessment ALTER COLUMN duration_minutes DROP DEFAULT;
ALTER TABLE assessment
    ADD CONSTRAINT ck_assessment_duration CHECK (duration_minutes > 0);

CREATE TABLE assessment_result_correction (
    id                UUID          PRIMARY KEY,
    result_id         UUID          NOT NULL,
    previous_score    NUMERIC(6, 2),
    previous_status   VARCHAR(20)   NOT NULL,
    new_score         NUMERIC(6, 2),
    new_status        VARCHAR(20)   NOT NULL,
    corrected_by      VARCHAR(200)  NOT NULL,
    corrected_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_assessment_result_correction_result FOREIGN KEY (result_id)
        REFERENCES assessment_result (id) ON DELETE CASCADE,
    CONSTRAINT ck_assessment_result_correction_previous_status
        CHECK (previous_status IN ('SCORED', 'ABSENT', 'EXCUSED')),
    CONSTRAINT ck_assessment_result_correction_new_status
        CHECK (new_status IN ('SCORED', 'ABSENT', 'EXCUSED')),
    CONSTRAINT ck_assessment_result_correction_previous_score
        CHECK (previous_score IS NULL OR previous_score >= 0),
    CONSTRAINT ck_assessment_result_correction_new_score
        CHECK (new_score IS NULL OR new_score >= 0),
    CONSTRAINT ck_assessment_result_correction_score_status CHECK (
        ((previous_status = 'SCORED' AND previous_score IS NOT NULL)
            OR (previous_status IN ('ABSENT', 'EXCUSED') AND previous_score IS NULL))
        AND ((new_status = 'SCORED' AND new_score IS NOT NULL)
            OR (new_status IN ('ABSENT', 'EXCUSED') AND new_score IS NULL))
    )
);

CREATE INDEX ix_assessment_result_correction_result
    ON assessment_result_correction (result_id, corrected_at);
