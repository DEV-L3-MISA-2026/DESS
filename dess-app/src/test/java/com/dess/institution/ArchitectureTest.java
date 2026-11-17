package com.dess.institution;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garde-fous d'architecture : le module reste indépendant de SQL, de REST et des autres modules. */
class ArchitectureTest {

    private static final Path MAIN = Path.of("src", "main", "java");

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "java.sql", "javax.sql", "jakarta.persistence", "javax.persistence", "org.hibernate",
            "org.flywaydb", "org.postgresql", "jakarta.ws.rs", "javax.ws.rs", "jakarta.servlet",
            "com.dess.academic", "com.dess.people", "com.dess.assessment", "com.dess.scheduling");

    private static List<Path> mainSources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    @Test
    void mainSourcesExist() throws IOException {
        assertFalse(mainSources().isEmpty());
    }

    @Test
    void noImportOfSqlJpaRestServletOrOtherBusinessModules() throws IOException {
        for (Path source : mainSources()) {
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import ")) {
                    for (String forbidden : FORBIDDEN_IMPORT_PREFIXES) {
                        assertFalse(trimmed.startsWith("import " + forbidden) || trimmed.startsWith("import static " + forbidden),
                                source + " importe " + forbidden);
                    }
                }
            }
        }
    }

    @Test
    void noRestResourceAnnotationsAndNoPersistenceAnnotationsAreUsed() throws IOException {
        Pattern forbidden = Pattern.compile("@(Path|GET|POST|PUT|DELETE|Entity|Table|PersistenceContext|Transactional)\\b");
        for (Path source : mainSources()) {
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                    continue; // commentaires et Javadoc
                }
                assertFalse(forbidden.matcher(trimmed).find(), source + " : annotation interdite -> " + trimmed);
            }
        }
    }

    @Test
    void noBusinessEntityOfOtherModulesIsDeclared() throws IOException {
        for (Path source : mainSources()) {
            String name = source.getFileName().toString();
            for (String banned : List.of("Student", "Professor", "Person", "Course", "Assessment", "Result",
                    "ExamSession", "StudentGroup", "AcademicProgram")) {
                assertFalse(name.equals(banned + ".java"), "classe réservée à un autre module : " + name);
            }
        }
    }

    @Test
    void unitTypesAreNeverHardCodedAsStringLiteralsOrEnums() throws IOException {
        Pattern literal = Pattern.compile("\"(FACULTE|MENTION|PARCOURS|SERIE|NIVEAU|DEPARTEMENT|CAMPUS|FILIERE|"
                + "FACULTY|LEVEL|DEPARTMENT|SERIES)\"");
        for (Path source : mainSources()) {
            String content = Files.readString(source);
            assertFalse(literal.matcher(content).find(), source + " contient un type d'unité codé en dur");
            assertFalse(Pattern.compile("enum\\s+\\w*Unit\\w*Type").matcher(content).find(),
                    source + " déclare une enum de types d'unités");
        }
    }

    @Test
    void pomDeclaresNoPersistenceRestOrSiblingModuleDependency() throws IOException {
        // on n'examine que les coordonnées Maven (groupId / artifactId), pas les commentaires
        Pattern coordinate = Pattern.compile("<(groupId|artifactId)>([^<]+)</\\1>");
        Matcher matcher = coordinate.matcher(Files.readString(Path.of("pom.xml")));
        List<String> coordinates = new java.util.ArrayList<>();
        while (matcher.find()) {
            coordinates.add(matcher.group(2).toLowerCase());
        }
        for (String banned : List.of("postgresql", "hibernate", "flyway", "persistence", "ws.rs", "resteasy",
                "dess-academic", "dess-people", "dess-assessment", "dess-scheduling")) {
            assertTrue(coordinates.stream().noneMatch(c -> c.contains(banned)),
                    "pom.xml déclare une dépendance interdite : " + banned);
        }
        assertTrue(coordinates.stream().anyMatch(c -> c.contains("cdi-api")), "CDI est l'API Jakarta attendue");
    }
}
