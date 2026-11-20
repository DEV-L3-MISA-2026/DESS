package com.dess.people.application;

/** Données nécessaires à la création d'un enseignant. */
public record CreateTeacherCommand(String email, String lastName, String firstName, String academicRank) {}
