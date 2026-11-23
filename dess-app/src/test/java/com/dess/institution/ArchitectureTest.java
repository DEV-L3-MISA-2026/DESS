package com.dess.institution;

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
import org.junit.jupiter.api.Test;

/**
 * @brief Architecture guard rails.
 *
 * <p>The institution module must remain independent from SQL, JPA, REST, servlets and the other
 * business modules, and unit types must never be hard-coded.</p>
 */
class ArchitectureTest
{
    // Note pour l'équipe (Iavo / Platform) :
    // On restreint ce test architectural au seul package 'com.dess.institution'
    // pour préserver son indépendance (pas de SQL/JPA/REST dans institution),
    // tout en permettant aux modules web/platform (comme DessApplication ou HealthResource)
    // d'utiliser les annotations Jakarta REST (@Path, @GET, etc.).
    private static final Path MAIN = Path.of("src", "main", "java", "com", "dess", "institution");

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "java.sql", "javax.sql", "jakarta.persistence", "javax.persistence", "org.hibernate",
            "org.flywaydb", "org.postgresql", "jakarta.ws.rs", "javax.ws.rs", "jakarta.servlet",
            "com.dess.academic", "com.dess.people", "com.dess.assessment", "com.dess.scheduling");

    private static List<Path> mainSources() throws IOException
    {
        try (Stream<Path> files = Files.walk(MAIN))
        {
            return files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    @Test
    void mainSourcesExist() throws IOException
    {
        assertFalse(mainSources().isEmpty());
    }

    /**
     * @brief Sources of one module, for guards that must look beyond institution.
     */
    private static List<Path> moduleSources(String module) throws IOException
    {
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java", "com", "dess", module)))
        {
            return files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    @Test
    void peopleDoesNotImportAcademic() throws IOException
    {
        for (Path source : moduleSources("people"))
        {
            for (String line : Files.readAllLines(source))
            {
                assertFalse(line.trim().startsWith("import com.dess.academic"),
                        source + " imports the academic module");
            }
        }
    }

    @Test
    void domainPackagesHaveNoTechnicalDependencies() throws IOException
    {
        for (String module : List.of("institution", "people"))
        {
            for (Path source : moduleSources(module))
            {
                if (!source.toString().contains("/domain/"))
                {
                    continue;
                }

                for (String line : Files.readAllLines(source))
                {
                    String trimmed = line.trim();
                    assertFalse(trimmed.startsWith("import jakarta.persistence"), source + " imports JPA");
                    assertFalse(trimmed.startsWith("import jakarta.ws.rs"), source + " imports REST");
                    assertFalse(trimmed.startsWith("import jakarta.servlet"), source + " imports Servlet");
                    assertFalse(trimmed.contains("com.dess." + module + ".infrastructure"),
                            source + " depends on infrastructure");
                }
            }
        }
    }

    @Test
    void noImportOfSqlJpaRestServletOrOtherBusinessModules() throws IOException
    {
        for (Path source : mainSources())
        {
            for (String line : Files.readAllLines(source))
            {
                String trimmed = line.trim();

                if (trimmed.startsWith("import "))
                {
                    String sourcePath = source.toString();
                    boolean ownModuleSource = sourcePath.contains("/com/dess/people/")
                                              || sourcePath.contains("/com/dess/academic/")
                                              || sourcePath.contains("/com/dess/scheduling/");
                    boolean infrastructureJpa = sourcePath.contains("/com/dess/people/infrastructure/")
                                                || sourcePath.contains("/com/dess/academic/infrastructure/")
                                                || sourcePath.contains("/com/dess/scheduling/infrastructure/")
                                                || sourcePath.contains("/com/dess/people/cdi/")
                                                || sourcePath.contains("/com/dess/academic/cdi/")
                                                || sourcePath.contains("/com/dess/scheduling/cdi/");

                    for (String forbidden : FORBIDDEN_IMPORT_PREFIXES)
                    {
                        if (ownModuleSource
                            && (forbidden.startsWith("com.dess.people")
                                || forbidden.startsWith("com.dess.academic")
                                || forbidden.startsWith("com.dess.scheduling")))
                        {
                            continue;
                        }

                        if (infrastructureJpa
                            && (forbidden.startsWith("jakarta.persistence")
                                || forbidden.startsWith("javax.persistence")))
                        {
                            continue;
                        }

                        assertFalse(trimmed.startsWith("import " + forbidden)
                                    || trimmed.startsWith("import static " + forbidden),
                                    source + " imports " + forbidden);
                    }
                }
            }
        }
    }

    @Test
    void noRestResourceAnnotationsAndNoPersistenceAnnotationsAreUsed() throws IOException
    {
        Pattern forbidden = Pattern.compile(
                "@(Path|GET|POST|PUT|DELETE|Entity|Table|PersistenceContext|Transactional)\\b");

        for (Path source : mainSources())
        {
            for (String line : Files.readAllLines(source))
            {
                String trimmed = line.trim();

                if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*"))
                {
                    continue; // Comments and Javadoc are not code.
                }

                boolean persistenceAllowed = source.toString().contains("/com/dess/people/infrastructure/")
                                             || source.toString().contains("/com/dess/academic/infrastructure/")
                                             || source.toString().contains("/com/dess/scheduling/infrastructure/")
                                             || source.toString().contains("/com/dess/people/cdi/")
                                             || source.toString().contains("/com/dess/academic/cdi/")
                                             || source.toString().contains("/com/dess/scheduling/cdi/")
                                             || source.toString().contains("/com/dess/people/application/impl/")
                                             || source.toString().contains("/com/dess/academic/application/impl/")
                                             || source.toString().contains("/com/dess/scheduling/application/impl/");

                if (!persistenceAllowed)
                {
                    assertFalse(forbidden.matcher(trimmed).find(), source + ": forbidden annotation -> " + trimmed);
                }
            }
        }
    }

    @Test
    void noBusinessEntityOfOtherModulesIsDeclared() throws IOException
    {
        for (Path source : mainSources())
        {
            String name = source.getFileName().toString();

            for (String banned : List.of("Student", "Professor", "Course", "Assessment", "Result",
                                         "ExamSession", "StudentGroup", "AcademicProgramVersion"))
            {
                assertFalse(name.equals(banned + ".java"), "class reserved for another module: " + name);
            }
        }
    }

    @Test
    void unitTypesAreNeverHardCodedAsStringLiteralsOrEnums() throws IOException
    {
        Pattern literal = Pattern.compile("\"(FACULTE|MENTION|PARCOURS|SERIE|NIVEAU|DEPARTEMENT|CAMPUS|FILIERE|"
                                          + "FACULTY|LEVEL|DEPARTMENT|SERIES)\"");

        for (Path source : mainSources())
        {
            String content = Files.readString(source);
            assertFalse(literal.matcher(content).find(), source + " contains a hard-coded unit type");
            assertFalse(Pattern.compile("enum\\s+\\w*Unit\\w*Type").matcher(content).find(),
                        source + " declares an enum of unit types");
        }
    }

    @Test
    void pomDeclaresNoPersistenceRestOrSiblingModuleDependency() throws IOException
    {
        // Only Maven coordinates (groupId / artifactId) are inspected, not comments.
        Pattern coordinate = Pattern.compile("<(groupId|artifactId)>([^<]+)</\\1>");
        Matcher matcher = coordinate.matcher(Files.readString(Path.of("pom.xml")));
        List<String> coordinates = new java.util.ArrayList<>();

        while (matcher.find())
        {
            coordinates.add(matcher.group(2).toLowerCase());
        }

        for (String banned : List.of("persistence", "ws.rs", "resteasy",
                                     "dess-academic", "dess-people", "dess-assessment", "dess-scheduling"))
        {
            assertTrue(coordinates.stream().noneMatch(c -> c.contains(banned)),
                       "pom.xml declares a forbidden dependency: " + banned);
        }

        // The JDBC driver and the ORM may appear only as test dependencies; WildFly supplies
        // them at runtime and Hibernate must never manage the schema.
        Pattern dependency = Pattern.compile(
                "<dependency>(.*?)</dependency>", Pattern.DOTALL);

        for (Matcher block = dependency.matcher(Files.readString(Path.of("pom.xml"))); block.find(); )
        {
            String body = block.group(1).toLowerCase();
            if (body.contains("postgresql") || body.contains("hibernate") || body.contains("flyway"))
            {
                assertTrue(body.contains("<scope>test</scope>"),
                           "database drivers and ORM must stay test-scoped");
            }
        }

        // Note pour l'équipe (Iavo / Platform) :
        // Le WAR unique 'dess-app' embarque désormais l'application complète déployée sur WildFly,
        // donc jakartaee-api est accepté pour fournir les spécifications standard (CDI, REST, etc.).
        assertTrue(coordinates.stream().anyMatch(c -> c.contains("cdi-api")
                                             || c.contains("jakartaee-api")),
                   "CDI or Jakarta EE API is the expected Jakarta API");
    }

    @Test
    void academicDomainHasNoTechnicalDependencies() throws IOException
    {
        for (Path source : moduleSources("academic"))
        {
            if (!source.toString().contains("/domain/")) continue;
            for (String line : Files.readAllLines(source))
            {
                String trimmed = line.trim();
                assertFalse(trimmed.startsWith("import jakarta.persistence"), source + " imports JPA");
                assertFalse(trimmed.startsWith("import jakarta.ws.rs"), source + " imports REST");
                assertFalse(trimmed.startsWith("import jakarta.servlet"), source + " imports Servlet");
                assertFalse(trimmed.contains("com.dess.academic.infrastructure"),
                        source + " depends on infrastructure");
            }
        }
    }

    @Test
    void academicPersistenceOnlyInInfrastructure() throws IOException
    {
        for (Path source : moduleSources("academic"))
        {
            boolean infrastructure = source.toString().contains("/academic/infrastructure/");
            for (String line : Files.readAllLines(source))
            {
                String trimmed = line.trim();
                if (trimmed.startsWith("import jakarta.persistence"))
                {
                    assertTrue(infrastructure, source + " uses JPA outside infrastructure");
                }
            }
        }

        for (Path source : moduleSources("academic"))
        {
            for (String line : Files.readAllLines(source))
            {
                String trimmed = line.trim();
                if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) continue;
                if (trimmed.contains("@Transactional"))
                {
                    boolean allowed = source.toString().contains("/application/impl/")
                            || source.toString().contains("/academic/infrastructure/");
                    assertTrue(allowed, source + " uses @Transactional outside application/impl or infrastructure");
                }
            }
        }
    }
}
