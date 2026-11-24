-- Ajoute le verrouillage optimiste aux inscriptions aux versions de programmes.
ALTER TABLE program_enrollment
    ADD COLUMN version bigint NOT NULL DEFAULT 0;
