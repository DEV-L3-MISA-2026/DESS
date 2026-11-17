package com.dess.institution.domain;

/** Petits utilitaires de normalisation de texte partagés par le domaine. */
public final class Texts {

    private Texts() {
    }

    /** Retourne la chaîne, ou "" si elle est {@code null}. Le validateur signale ensuite les champs vides. */
    public static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Retourne {@code null} si la chaîne est nulle ou blanche, sinon la chaîne telle quelle. */
    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
