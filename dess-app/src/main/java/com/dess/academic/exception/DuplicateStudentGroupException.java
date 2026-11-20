package com.dess.academic.exception;

import java.util.UUID;

/** Levée lorsqu'un code de groupe est déjà utilisé dans une période académique. */
public class DuplicateStudentGroupException extends AcademicException {
    public DuplicateStudentGroupException(String code, UUID periodId) {
        super("Un groupe avec le code '" + code + "' existe déjà pour la période '" + periodId + "'");
    }
}
