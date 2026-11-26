-- Garantit l’unicité insensible à la casse des adresses e-mail.
CREATE UNIQUE INDEX uq_app_user_email_lower
    ON app_user (lower(email));
