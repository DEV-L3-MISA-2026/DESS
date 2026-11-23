package com.dess.people.domain;

import java.util.Objects;

/** Profil immuable d’un enseignant. */
public record TeacherProfile(long personId, String academicRank) {
    public TeacherProfile {
        if (personId <= 0) throw new IllegalArgumentException("L’identifiant doit être positif");
        Objects.requireNonNull(academicRank, "Le grade académique est obligatoire");
        academicRank = academicRank.trim();
        if (academicRank.isBlank()) throw new IllegalArgumentException("Le grade académique est obligatoire");
    }
}
