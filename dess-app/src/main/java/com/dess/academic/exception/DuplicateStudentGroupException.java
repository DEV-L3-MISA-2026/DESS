package com.dess.academic.exception;

/** Levée lorsqu'un code de groupe est déjà utilisé dans une période académique. */
public class DuplicateStudentGroupException extends AcademicException
{
    public DuplicateStudentGroupException(String code, long periodId)
    {
        super("Un groupe avec le code '" + code + "' existe déjà pour la période '" + periodId + "'");
    }
}
