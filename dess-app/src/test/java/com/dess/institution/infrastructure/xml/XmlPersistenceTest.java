package com.dess.institution.infrastructure.xml;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.application.ConfigurationStatus;
import com.dess.institution.application.CreateUnitCommand;
import com.dess.institution.application.HierarchyService;
import com.dess.institution.application.UpdateUnitCommand;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeType;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ConfigurationSaveException;
import com.dess.institution.exception.NotInitializedException;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief Tests for the XML persistence.
 *
 * <p>The XML file is the source of truth: every test reloads the data from disk instead of trusting
 * the in-memory state.</p>
 */
class XmlPersistenceTest
{
    @TempDir
    Path tmp;

    private InstitutionConfig reloadFromDisk(Path file)
    {
        return new XmlConfigLoader().load(file);
    }

    @Test
    void createdUnitIsWrittenToTheXmlFile() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().createUnit(CreateUnitCommand.of(
            "ARTS", "Faculty of Arts", "FACULTE", "univ-x", Map.of("doyen", "Rabe Marie")));

        String xml = Files.readString(file, StandardCharsets.UTF_8);

        assertTrue(xml.contains("Faculty of Arts"));
        assertTrue(xml.contains("Rabe Marie"));

        OrganizationalUnit onDisk = reloadFromDisk(file).findUnitByCode("ARTS").orElseThrow();

        assertEquals("univ-x", onDisk.parentId());
        assertEquals("Rabe Marie", onDisk.value("doyen").orElseThrow().value());
    }

    @Test
    void updatedUnitIsWrittenToTheXmlFile()
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().updateUnit(
            "faculte-droit",
            new UpdateUnitCommand(null, "Faculty of Law (renamed)", null, null));
        module.attributeService().setValue("faculte-droit", "doyen", "Randria Paul");

        OrganizationalUnit onDisk = reloadFromDisk(file).findUnit("faculte-droit").orElseThrow();

        assertEquals("Faculty of Law (renamed)", onDisk.name());
        assertEquals("Randria Paul", onDisk.value("doyen").orElseThrow().value());
    }

    @Test
    void movedUnitIsWrittenToTheXmlFileWithTheSameId() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().moveUnit("mention-droit", "faculte-sciences");

        InstitutionConfig onDisk = reloadFromDisk(file);

        assertEquals("faculte-sciences", onDisk.findUnit("mention-droit").orElseThrow().parentId());
        assertEquals(11, onDisk.units().size());

        // The subtree follows: its path stays under the moved mention.
        assertEquals("mention-droit", onDisk.findUnit("parcours-droit-prive").orElseThrow().parentId());
        assertTrue(Files.readString(file).contains("id=\"mention-droit\""));
    }

    @Test
    void typesAttributeDefinitionsAndPairsArePersisted()
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.unitTypeService().create(UnitTypeDefinition.of("LABORATORY", "Laboratory"));
        module.attributeService().addDefinition(
            "LABORATORY",
            AttributeDefinition.of("manager", "Manager", AttributeType.TEXT, false));
        module.unitTypeService().addAllowedPair("FACULTE", "LABORATORY");
        module.unitTypeService().deactivate("NIVEAU");

        InstitutionConfig onDisk = reloadFromDisk(file);

        assertEquals(1, onDisk.findType("LABORATORY").orElseThrow().attributes().size());
        assertTrue(onDisk.isAllowed("FACULTE", "LABORATORY"));
        assertFalse(onDisk.findType("NIVEAU").orElseThrow().active());
    }

    @Test
    void simulatedRestartKeepsEverything()
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule first = InstitutionModule.open(file);

        first.hierarchyService().createUnit(CreateUnitCommand.of(
            "ARTS", "Faculty of Arts", "FACULTE", "univ-x", Map.of("doyen", "Rabe Marie")));
        first.hierarchyService().moveUnit("mention-droit", "faculte-sciences");
        first.hierarchyService().deactivateUnit("dev-m1", false);

        InstitutionConfig before = first.institutionService().getConfig();
        InstitutionModule restarted = InstitutionModule.open(file);
        HierarchyService hierarchy = restarted.hierarchyService();

        assertEquals(before.units().size(), restarted.institutionService().getConfig().units().size());
        assertTrue(hierarchy.getByCode("ARTS").isPresent());
        assertEquals("faculte-sciences", hierarchy.getParent("mention-droit").orElseThrow().id());
        assertFalse(hierarchy.getById("dev-m1").orElseThrow().active());
        assertTrue(restarted.institutionService().check().valid());
    }

    @Test
    void previousVersionIsKeptAsBackup() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        byte[] original = Files.readAllBytes(file);
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().deactivateUnit("dev-m1", false);

        Path backup = file.resolveSibling("institution.xml.bak");

        assertTrue(Files.exists(backup));
        assertArrayEquals(original, Files.readAllBytes(backup));
    }

    @Test
    void failedWriteLeavesTheOldFileValidAndMemoryUnchanged() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        AtomicBoolean failing = new AtomicBoolean(true);

        InstitutionConfigRepository repository = new InstitutionConfigRepository(file, temporaryFile ->
        {
            if (failing.get())
            {
                throw new IOException("disk full (simulated)");
            }
        });

        InstitutionModule module = InstitutionModule.open(repository);
        byte[] before = Files.readAllBytes(file);

        assertThrows(ConfigurationSaveException.class, () -> module.hierarchyService().createUnit(
            CreateUnitCommand.of("ARTS", "Faculty of Arts", "FACULTE", "univ-x", Map.of("doyen", "A"))));

        assertArrayEquals(before, Files.readAllBytes(file), "the file must not have changed");
        assertTrue(reloadFromDisk(file).findUnitByCode("ARTS").isEmpty());
        assertTrue(module.hierarchyService().getByCode("ARTS").isEmpty(),
                   "memory must not have changed");

        try (Stream<Path> files = Files.list(tmp))
        {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")),
                       "no temporary file must remain");
        }

        // Once the problem is solved, the same operation succeeds.
        failing.set(false);
        module.hierarchyService().createUnit(
            CreateUnitCommand.of("ARTS", "Faculty of Arts", "FACULTE", "univ-x", Map.of("doyen", "A")));

        assertTrue(reloadFromDisk(file).findUnitByCode("ARTS").isPresent());
    }

    @Test
    void unwritableLocationRaisesAReadableSaveException() throws IOException
    {
        Path blocker = Files.writeString(tmp.resolve("not-a-directory"), "x");
        InstitutionConfigRepository repository = new InstitutionConfigRepository(
            blocker.resolve("institution.xml"));
        InstitutionConfig config = new XmlConfigLoader().load(TestSupport.resource("valid/university.xml"));

        assertThrows(ConfigurationSaveException.class, () -> repository.save(config));
    }

    @Test
    void invalidChangeNeverReachesTheDisk() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);
        byte[] before = Files.readAllBytes(file);

        assertThrows(ValidationException.class,
                     () -> module.hierarchyService().moveUnit("parcours-dev", "faculte-sciences"));
        assertThrows(ValidationException.class,
                     () -> module.attributeService().setValue("mention-droit", "effectifMaximum", "Hello"));

        assertArrayEquals(before, Files.readAllBytes(file));
        assertFalse(Files.exists(file.resolveSibling("institution.xml.bak")), "no write took place");
    }

    @Test
    void concurrentModificationsAreNeverSilentlyLost() throws Exception
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);
        int threads = 16;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++)
        {
            int index = i;

            futures.add(pool.submit(() ->
            {
                start.await();
                module.hierarchyService().createUnit(CreateUnitCommand.of(
                    "FAC_" + index, "Faculty " + index, "FACULTE", "univ-x", Map.of("doyen", "Dean " + index)));

                return null;
            }));
        }

        start.countDown();

        for (Future<?> future : futures)
        {
            future.get(60, TimeUnit.SECONDS);
        }

        pool.shutdown();

        assertEquals(11 + threads, module.institutionService().getConfig().units().size());

        InstitutionConfig onDisk = reloadFromDisk(file);

        assertEquals(11 + threads, onDisk.units().size());

        for (int i = 0; i < threads; i++)
        {
            assertTrue(onDisk.findUnitByCode("FAC_" + i).isPresent(), "lost update: FAC_" + i);
        }
    }

    // ------------------------------------------------------------------ missing configuration

    @Test
    void missingFileMeansNotInitializedAndNothingIsCreatedSilently()
    {
        Path file = tmp.resolve("config").resolve("institution.xml");
        InstitutionModule module = InstitutionModule.open(file);

        assertEquals(ConfigurationStatus.NOT_INITIALIZED, module.institutionService().getStatus());
        assertThrows(NotInitializedException.class, () -> module.hierarchyService().getRoots());
        assertThrows(NotInitializedException.class, () -> module.institutionService().getConfig());
        assertFalse(Files.exists(file));
    }

    @Test
    void explicitInitializationCreatesTheFileAndSurvivesRestart()
    {
        Path file = tmp.resolve("config").resolve("institution.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.institutionService().initialize("MY_SCHOOL", "My School", "Demonstration institution");
        module.unitTypeService().create(UnitTypeDefinition.of("SITE", "Site"));
        module.hierarchyService().createUnit(CreateUnitCommand.of("SITE_1", "Main site", "SITE", null, null));

        assertTrue(Files.exists(file));

        InstitutionModule restarted = InstitutionModule.open(file);

        assertEquals(ConfigurationStatus.READY, restarted.institutionService().getStatus());
        assertEquals("My School", restarted.institutionService().getInfo().name());
        assertEquals(1, restarted.hierarchyService().getRoots().size());
        assertThrows(ValidationException.class,
                     () -> restarted.institutionService().initialize("X", "X", null));
    }

    @Test
    void updatingGeneralInfoKeepsTheIdentifierAndIsPersisted()
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.institutionService().updateInfo("UNIVERSITY_X2", "University X (new name)", "New description");

        InstitutionConfig onDisk = reloadFromDisk(file);

        assertEquals("institution-001", onDisk.id());
        assertEquals("UNIVERSITY_X2", onDisk.code());
        assertEquals("New description", onDisk.description());
        assertThrows(ValidationException.class, () -> module.institutionService().updateInfo("", "x", null));
    }

    @Test
    void reloadPicksUpAManualEditOfTheFile() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        String edited = Files.readString(file)
                             .replace("Faculty of Law", "Faculty of Law and Political Science");
        Files.writeString(file, edited);
        module.institutionService().reload();

        assertEquals("Faculty of Law and Political Science",
                     module.hierarchyService().getById("faculte-droit").orElseThrow().name());
    }

    @Test
    void failedReloadKeepsTheLastGoodStateInMemory() throws IOException
    {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        Files.writeString(file, "<institution><not valid xml");

        assertThrows(RuntimeException.class, () -> module.institutionService().reload());
        assertEquals(11, module.institutionService().getConfig().units().size());
    }
}
