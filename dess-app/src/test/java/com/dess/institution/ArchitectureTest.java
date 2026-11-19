package com.dess.institution;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garde-fous d'architecture des modules institution et people. */
class ArchitectureTest {

    private static final Path MAIN = Path.of("src", "main", "java");

    private static List<Path> mainSources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    @Test
    void mainSourcesExist() throws IOException {
        assertFalse(mainSources().isEmpty());
    }

    @Test
    void institutionDoesNotImportOtherBusinessModules() throws IOException {
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/institution/")) continue;
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("import ")) continue;
                for (String forbidden : List.of("com.dess.academic", "com.dess.people",
                        "com.dess.assessment", "com.dess.scheduling")) {
                    assertFalse(trimmed.startsWith("import " + forbidden),
                            source + " importe le module " + forbidden);
                }
            }
        }
    }

    @Test
    void peopleDoesNotImportAcademic() throws IOException {
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/people/")) continue;
            for (String line : Files.readAllLines(source)) {
                assertFalse(line.trim().startsWith("import com.dess.academic"),
                        source + " importe academic");
            }
        }
    }

    @Test
    void jakartaPersistenceOnlyInInfrastructurePackages() throws IOException {
        for (Path source : mainSources()) {
            boolean infrastructure = source.toString().contains("/com/dess/")
                    && source.toString().contains("/infrastructure/");
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import jakarta.persistence")
                        || trimmed.startsWith("import jakarta.persistence.")) {
                    assertTrue(infrastructure, source + " utilise jakarta.persistence hors infrastructure");
                }
            }
        }
    }

    @Test
    void transactionalOnlyInApplicationImplAndInfrastructure() throws IOException {
        Pattern transactional = Pattern.compile("@Transactional\\b");
        for (Path source : mainSources()) {
            boolean allowed = source.toString().contains("/application/impl/")
                    || source.toString().contains("/infrastructure/");
            for (String line : Files.readAllLines(source)) {
                if (transactional.matcher(line).find()) {
                    assertTrue(allowed, source + " utilise @Transactional hors des packages autorisés");
                }
            }
        }
    }

    @Test
    void institutionDoesNotDeclareReservedNames() throws IOException {
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/institution/")) continue;
            String name = source.getFileName().toString();
            for (String banned : List.of("Person", "PersonProfile", "Student", "Professor",
                    "Teacher", "Course", "Assessment", "Result", "ExamSession", "StudentGroup",
                    "AcademicProgram", "AcademicProgramVersion")) {
                assertFalse(name.equals(banned + ".java"),
                        "nom réservé à un autre module : " + name);
            }
        }
    }

    @Test
    void institutionDomainHasNoTechnicalDependencies() throws IOException {
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/institution/domain/")) continue;
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                assertFalse(trimmed.startsWith("import jakarta.persistence"), source + " importe JPA");
                assertFalse(trimmed.startsWith("import jakarta.ws.rs"), source + " importe REST");
                assertFalse(trimmed.startsWith("import jakarta.servlet"), source + " importe Servlet");
                assertFalse(trimmed.contains("com.dess.institution.infrastructure"),
                        source + " dépend de infrastructure");
            }
        }
    }

    @Test
    void peopleDomainHasNoTechnicalDependencies() throws IOException {
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/people/domain/")) continue;
            String content = Files.readString(source);
            assertFalse(content.contains("jakarta.persistence"), source + " dépend de JPA");
            assertFalse(content.contains("jakarta.ws.rs"), source + " dépend de REST");
            assertFalse(content.contains("jakarta.servlet"), source + " dépend de Servlet");
            assertFalse(content.contains("com.dess.people.infrastructure"),
                    source + " dépend de infrastructure");
        }
    }

    @Test
    void institutionAndPeopleHaveNoForbiddenRestJpaSqlImports() throws IOException {
        Pattern forbidden = Pattern.compile("@(Path|GET|POST|PUT|DELETE|Entity|Table|PersistenceContext)\\b");
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/institution/")
                    && !source.toString().contains("/com/dess/people/")) continue;
            boolean persistenceAllowed = source.toString().contains("/com/dess/people/infrastructure/");
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) continue;
                if (!persistenceAllowed) {
                    assertFalse(forbidden.matcher(trimmed).find(),
                            source + " utilise une annotation technique interdite : " + trimmed);
                }
                for (String forbiddenImport : List.of("java.sql", "javax.sql", "org.hibernate",
                        "org.flywaydb", "org.postgresql")) {
                    assertFalse(trimmed.startsWith("import " + forbiddenImport),
                            source + " importe " + forbiddenImport);
                }
            }
        }
    }

    @Test
    void unitTypesAreNeverHardCodedAsStringLiteralsOrEnums() throws IOException {
        Pattern literal = Pattern.compile("\"(FACULTE|MENTION|PARCOURS|SERIE|NIVEAU|DEPARTEMENT|CAMPUS|FILIERE|"
                + "FACULTY|LEVEL|DEPARTMENT|SERIES)\"");
        for (Path source : mainSources()) {
            if (!source.toString().contains("/com/dess/institution/")) continue;
            String content = Files.readString(source);
            assertFalse(literal.matcher(content).find(), source + " contient un type d'unité codé en dur");
            assertFalse(Pattern.compile("enum\\s+\\w*Unit\\w*Type").matcher(content).find(),
                    source + " déclare une enum de types d'unités");
        }
    }

    @Test
    void pomDeclaresNoPersistenceRestOrSiblingModuleDependency() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        Pattern dependencyPattern = Pattern.compile(
                "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(.*?)</dependency>",
                Pattern.DOTALL);
        Matcher dependencyMatcher = dependencyPattern.matcher(pom);

        for (String banned : List.of("persistence", "ws.rs", "resteasy",
                "dess-academic", "dess-people", "dess-assessment", "dess-scheduling")) {
            assertFalse(pom.toLowerCase().contains("<artifactId>" + banned)
                            || pom.toLowerCase().contains("<groupId>" + banned),
                    "pom.xml déclare une dépendance interdite : " + banned);
        }

        while (dependencyMatcher.find()) {
            String artifactId = dependencyMatcher.group(2).trim().toLowerCase();
            String dependencyBlock = dependencyMatcher.group(3);
            boolean testScope = dependencyBlock.contains("<scope>test</scope>");

            if (artifactId.contains("postgresql") || artifactId.contains("testcontainers")
                    || artifactId.contains("hibernate") || artifactId.contains("flyway")) {
                assertTrue(testScope, artifactId + " doit être limité au scope test");
            }
        }

        assertTrue(pom.contains("<artifactId>cdi-api</artifactId>")
                        || pom.contains("<artifactId>jakarta.jakartaee-api</artifactId>"),
                "CDI ou Jakarta EE API est attendue");
    }
}
