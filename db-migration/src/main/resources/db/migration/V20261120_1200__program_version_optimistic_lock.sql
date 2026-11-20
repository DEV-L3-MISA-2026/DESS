-- Ajoute le verrouillage optimiste aux versions de programme.
ALTER TABLE program_version
    ADD COLUMN version bigint NOT NULL DEFAULT 0;
