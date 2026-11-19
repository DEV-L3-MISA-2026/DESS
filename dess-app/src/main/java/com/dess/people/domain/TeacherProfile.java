package com.dess.people.domain;

import java.util.Objects;

/** Profil immuable d'un enseignant. */
public record TeacherProfile(long personId, String academicRank) {
    public TeacherProfile {
        Objects.requireNonNull(academicRank, "academicRank");
        if (academicRank.isBlank()) throw new IllegalArgumentException("Le grade académique est obligatoire");
    }
}
