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

/** Le fichier XML est la source de vérité : tout est relu depuis le disque dans ces tests. */
class XmlPersistenceTest {

    @TempDir
    Path tmp;

    private InstitutionConfig reloadFromDisk(Path file) {
        return new XmlConfigLoader().load(file);
    }

    @Test
    void createdUnitIsWrittenToTheXmlFile() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().createUnit(CreateUnitCommand.of(
                "LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "Rabe Marie")));

        String xml = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(xml.contains("Faculté des Lettres"));
        assertTrue(xml.contains("Rabe Marie"));
        OrganizationalUnit onDisk = reloadFromDisk(file).findUnitByCode("LETTRES").orElseThrow();
        assertEquals("univ-x", onDisk.parentId());
        assertEquals("Rabe Marie", onDisk.value("doyen").orElseThrow().value());
    }

    @Test
    void updatedUnitIsWrittenToTheXmlFile() {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().updateUnit("faculte-droit",
                new UpdateUnitCommand(null, "Faculté de Droit (renommée)", null, null));
        module.attributeService().setValue("faculte-droit", "doyen", "Randria Paul");

        OrganizationalUnit onDisk = reloadFromDisk(file).findUnit("faculte-droit").orElseThrow();
        assertEquals("Faculté de Droit (renommée)", onDisk.name());
        assertEquals("Randria Paul", onDisk.value("doyen").orElseThrow().value());
    }

    @Test
    void movedUnitIsWrittenToTheXmlFileWithTheSameId() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().moveUnit("mention-droit", "faculte-sciences");

        InstitutionConfig onDisk = reloadFromDisk(file);
        assertEquals("faculte-sciences", onDisk.findUnit("mention-droit").orElseThrow().parentId());
        assertEquals(11, onDisk.units().size());
        // le sous-arbre suit : le parcours reste sous la mention déplacée
        assertEquals("mention-droit", onDisk.findUnit("parcours-droit-prive").orElseThrow().parentId());
        assertTrue(Files.readString(file).contains("id=\"mention-droit\""));
    }

    @Test
    void typesAttributeDefinitionsAndPairsArePersisted() {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.unitTypeService().create(UnitTypeDefinition.of("LABORATOIRE", "Laboratoire"));
        module.attributeService().addDefinition("LABORATOIRE",
                AttributeDefinition.of("responsable", "Responsable", AttributeType.TEXT, false));
        module.unitTypeService().addAllowedPair("FACULTE", "LABORATOIRE");
        module.unitTypeService().deactivate("NIVEAU");

        InstitutionConfig onDisk = reloadFromDisk(file);
        assertEquals(1, onDisk.findType("LABORATOIRE").orElseThrow().attributes().size());
        assertTrue(onDisk.isAllowed("FACULTE", "LABORATOIRE"));
        assertFalse(onDisk.findType("NIVEAU").orElseThrow().active());
    }

    @Test
    void simulatedRestartKeepsEverything() {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule first = InstitutionModule.open(file);
        first.hierarchyService().createUnit(CreateUnitCommand.of(
                "LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "Rabe Marie")));
        first.hierarchyService().moveUnit("mention-droit", "faculte-sciences");
        first.hierarchyService().deactivateUnit("dev-m1", false);
        InstitutionConfig before = first.institutionService().getConfig();

        InstitutionModule restarted = InstitutionModule.open(file);
        HierarchyService h = restarted.hierarchyService();

        assertEquals(before.units().size(), restarted.institutionService().getConfig().units().size());
        assertTrue(h.getByCode("LETTRES").isPresent());
        assertEquals("faculte-sciences", h.getParent("mention-droit").orElseThrow().id());
        assertFalse(h.getById("dev-m1").orElseThrow().active());
        assertTrue(restarted.institutionService().check().valid());
    }

    @Test
    void previousVersionIsKeptAsBackup() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        byte[] original = Files.readAllBytes(file);
        InstitutionModule module = InstitutionModule.open(file);

        module.hierarchyService().deactivateUnit("dev-m1", false);

        Path backup = file.resolveSibling("institution.xml.bak");
        assertTrue(Files.exists(backup));
        assertArrayEquals(original, Files.readAllBytes(backup));
    }

    @Test
    void failedWriteLeavesTheOldFileValidAndMemoryUnchanged() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        AtomicBoolean failing = new AtomicBoolean(true);
        InstitutionConfigRepository repository = new InstitutionConfigRepository(file, temp -> {
            if (failing.get()) {
                throw new IOException("disque plein (simulé)");
            }
        });
        InstitutionModule module = InstitutionModule.open(repository);
        byte[] before = Files.readAllBytes(file);

        assertThrows(ConfigurationSaveException.class, () -> module.hierarchyService().createUnit(
                CreateUnitCommand.of("LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "A"))));

        assertArrayEquals(before, Files.readAllBytes(file), "le fichier ne doit pas avoir changé");
        assertTrue(reloadFromDisk(file).findUnitByCode("LETTRES").isEmpty());
        assertTrue(module.hierarchyService().getByCode("LETTRES").isEmpty(), "la mémoire ne doit pas avoir changé");
        try (Stream<Path> files = Files.list(tmp)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "aucun fichier temporaire ne doit rester");
        }

        // une fois le problème résolu, la même opération réussit
        failing.set(false);
        module.hierarchyService().createUnit(
                CreateUnitCommand.of("LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "A")));
        assertTrue(reloadFromDisk(file).findUnitByCode("LETTRES").isPresent());
    }

    @Test
    void unwritableLocationRaisesAReadableSaveException() throws IOException {
        Path blocker = Files.writeString(tmp.resolve("pas-un-dossier"), "x");
        InstitutionConfigRepository repository = new InstitutionConfigRepository(blocker.resolve("institution.xml"));
        InstitutionConfig cfg = new XmlConfigLoader().load(TestSupport.resource("valid/university.xml"));
        assertThrows(ConfigurationSaveException.class, () -> repository.save(cfg));
    }

    @Test
    void invalidChangeNeverReachesTheDisk() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);
        byte[] before = Files.readAllBytes(file);

        assertThrows(ValidationException.class, () -> module.hierarchyService().moveUnit("parcours-dev", "faculte-sciences"));
        assertThrows(ValidationException.class, () -> module.attributeService()
                .setValue("mention-droit", "effectifMaximum", "Bonjour"));

        assertArrayEquals(before, Files.readAllBytes(file));
        assertFalse(Files.exists(file.resolveSibling("institution.xml.bak")), "aucune écriture n'a eu lieu");
    }

    @Test
    void concurrentModificationsAreNeverSilentlyLost() throws Exception {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);
        int threads = 16;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            futures.add(pool.submit(() -> {
                start.await();
                module.hierarchyService().createUnit(CreateUnitCommand.of(
                        "FAC_" + n, "Faculté " + n, "FACULTE", "univ-x", Map.of("doyen", "Doyen " + n)));
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(11 + threads, module.institutionService().getConfig().units().size());
        InstitutionConfig onDisk = reloadFromDisk(file);
        assertEquals(11 + threads, onDisk.units().size());
        for (int i = 0; i < threads; i++) {
            assertTrue(onDisk.findUnitByCode("FAC_" + i).isPresent(), "mise à jour perdue : FAC_" + i);
        }
    }

    // ---------------------------------------------------------------- absence de configuration

    @Test
    void missingFileMeansNotInitializedAndNothingIsCreatedSilently() {
        Path file = tmp.resolve("config").resolve("institution.xml");
        InstitutionModule module = InstitutionModule.open(file);

        assertEquals(ConfigurationStatus.NOT_INITIALIZED, module.institutionService().getStatus());
        assertThrows(NotInitializedException.class, () -> module.hierarchyService().getRoots());
        assertThrows(NotInitializedException.class, () -> module.institutionService().getConfig());
        assertFalse(Files.exists(file));
    }

    @Test
    void explicitInitializationCreatesTheFileAndSurvivesRestart() {
        Path file = tmp.resolve("config").resolve("institution.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.institutionService().initialize("MON_ECOLE", "Mon école", "Établissement de démonstration");
        module.unitTypeService().create(UnitTypeDefinition.of("SITE", "Site"));
        module.hierarchyService().createUnit(CreateUnitCommand.of("SITE_1", "Site principal", "SITE", null, null));

        assertTrue(Files.exists(file));
        InstitutionModule restarted = InstitutionModule.open(file);
        assertEquals(ConfigurationStatus.READY, restarted.institutionService().getStatus());
        assertEquals("Mon école", restarted.institutionService().getInfo().name());
        assertEquals(1, restarted.hierarchyService().getRoots().size());
        assertThrows(ValidationException.class, () -> restarted.institutionService().initialize("X", "X", null));
    }

    @Test
    void updatingGeneralInfoKeepsTheIdentifierAndIsPersisted() {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        module.institutionService().updateInfo("UNIVERSITE_X2", "Université X (nouveau nom)", "Nouvelle description");

        InstitutionConfig onDisk = reloadFromDisk(file);
        assertEquals("institution-001", onDisk.id());
        assertEquals("UNIVERSITE_X2", onDisk.code());
        assertEquals("Nouvelle description", onDisk.description());
        assertThrows(ValidationException.class, () -> module.institutionService().updateInfo("", "x", null));
    }

    @Test
    void reloadPicksUpAManualEditOfTheFile() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        String edited = Files.readString(file).replace("Faculté de Droit", "Faculté de Droit et Sciences Politiques");
        Files.writeString(file, edited);
        module.institutionService().reload();

        assertEquals("Faculté de Droit et Sciences Politiques",
                module.hierarchyService().getById("faculte-droit").orElseThrow().name());
    }

    @Test
    void failedReloadKeepsTheLastGoodStateInMemory() throws IOException {
        Path file = TestSupport.copyTo(tmp, "valid/university.xml");
        InstitutionModule module = InstitutionModule.open(file);

        Files.writeString(file, "<institution><pas du xml valide");
        assertThrows(RuntimeException.class, () -> module.institutionService().reload());

        assertEquals(11, module.institutionService().getConfig().units().size());
    }
}
