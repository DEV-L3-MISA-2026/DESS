package com.dess.people.application.validation;

import com.dess.people.exception.StudentValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Valide les données communes aux créations et modifications d’étudiants. */
public final class StudentInputValidator {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*$");

    private StudentInputValidator() {}

    public static void validate(String email, String lastName, String firstName) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (firstName == null || firstName.trim().isBlank()) {
            errors.put("firstName", "Le prénom est obligatoire");
        }
        if (lastName == null || lastName.trim().isBlank()) {
            errors.put("lastName", "Le nom est obligatoire");
        }
        if (email == null || email.trim().isBlank()) {
            errors.put("email", "L’adresse e-mail est obligatoire");
        } else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            errors.put("email", "L’adresse e-mail est invalide");
        }
        if (!errors.isEmpty()) throw new StudentValidationException(errors);
    }
}
