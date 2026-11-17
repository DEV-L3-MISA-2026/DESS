package mg.dess.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.List;

/** Enforces the bounded-context rules of docs/architecture.md. */
@AnalyzeClasses(packages = "mg.dess", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final List<String> CONTEXTS =
            List.of("institution", "people", "scheduling", "academic", "assessment", "security");

    @ArchTest
    static void otherContextsOnlyUseThePublishedApi(JavaClasses classes) {
        for (String context : CONTEXTS) {
            noClasses()
                    .that().resideOutsideOfPackage("mg.dess." + context + "..")
                    .should().dependOnClassesThat().resideInAPackage("mg.dess." + context + ".internal..")
                    .because("only mg.dess." + context + ".api may be used by other modules")
                    .check(classes);
        }
    }

    @ArchTest
    static void domainDoesNotDependOnOuterLayers(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..internal.application..", "..internal.infrastructure..", "..internal.rest..")
                .check(classes);
    }

    @ArchTest
    static void schedulingKnowsNoOtherBusinessContext(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("mg.dess.scheduling..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "mg.dess.academic..", "mg.dess.assessment..", "mg.dess.institution..",
                        "mg.dess.people..", "mg.dess.security..")
                .check(classes);
    }
}
