package com.dess.people.application;

/** Données nécessaires à la création d'un étudiant. */
public record CreateStudentCommand(String email, String lastName, String firstName, String registrationNumber) {}
