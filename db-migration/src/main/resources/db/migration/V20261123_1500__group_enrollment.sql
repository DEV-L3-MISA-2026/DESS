-- Module academic : inscription d'un étudiant dans un groupe
-- IMPORTANT: ce numéro doit être coordonné avec Iavo avant toute migration suivante.
CREATE TABLE group_enrollment (
    id                 UUID PRIMARY KEY,
    student_id         UUID NOT NULL,
    group_id           UUID NOT NULL,
    academic_period_id UUID NOT NULL,
    enrolled_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_group_enrollment
        UNIQUE (student_id, group_id, academic_period_id)
);

-- Le module institution/student_group peut ne pas être encore fusionné sur dev.
-- Ajoute la FK dès que la table student_group existe, sans modifier les autres modules.
DO $$
BEGIN
    IF to_regclass('public.student_group') IS NOT NULL THEN
        ALTER TABLE group_enrollment
            ADD CONSTRAINT fk_group_enrollment_student_group
            FOREIGN KEY (group_id) REFERENCES student_group (id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX ix_group_enrollment_group_period
    ON group_enrollment (group_id, academic_period_id);

-- academic_period(id) n'existe pas sur cette branche: period utilise bigint.
-- Aucune FK academic_period_id n'est donc ajoutée. Ajouter une FK uniquement
-- après concordance des identifiants de période avec l'owner academic.
