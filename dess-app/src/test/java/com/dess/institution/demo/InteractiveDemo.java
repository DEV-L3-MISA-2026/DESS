package com.dess.institution.demo;

import com.dess.institution.InstitutionModule;
import com.dess.institution.application.CreateUnitCommand;
import com.dess.institution.application.HierarchyService;
import com.dess.institution.application.UpdateUnitCommand;
import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeType;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.InstitutionException;
import com.dess.institution.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Démo console interactive du module institution.
 * <ol>
 *   <li>On construit l'architecture (types, attributs, relations, unités) ou on charge un exemple.</li>
 *   <li>Le fichier XML est généré à chaque modification et peut être affiché.</li>
 *   <li>Un menu de « transactions » appelle les services (getById, getByCode, moveUnit...).</li>
 * </ol>
 * Lancement : voir README (section « Démo interactive »). Le fichier XML est écrit dans un dossier
 * temporaire (ou dans le chemin passé en argument), jamais dans les fichiers du projet.
 */
public final class InteractiveDemo {

    private final Scanner in;
    private final Path xmlFile;
    private InstitutionModule module;

    private InteractiveDemo(Scanner in, Path xmlFile) {
        this.in = in;
        this.xmlFile = xmlFile;
    }

    public static void main(String[] args) throws IOException {
        // accents corrects même si le terminal n'a pas de locale UTF-8
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true,
                java.nio.charset.StandardCharsets.UTF_8));
        Path file = args.length > 0
                ? Path.of(args[0])
                : Files.createTempDirectory("institution-demo").resolve("institution.xml");
        try (Scanner scanner = new Scanner(System.in)) {
            new InteractiveDemo(scanner, file).run();
        }
    }

    // ------------------------------------------------------------------ flux principal

    private void run() throws IOException {
        System.out.println("=== Démo interactive du module institution ===");
        System.out.println("Fichier XML : " + xmlFile.toAbsolutePath());
        try {
            module = InstitutionModule.open(xmlFile);
            if (module.institutionService().getStatus() == com.dess.institution.application.ConfigurationStatus.READY) {
                System.out.println("Configuration existante chargée : " + module.institutionService().getInfo().name());
            } else if (!startNewInstitution()) {
                return;
            }
            buildMenu();
        } catch (EndOfInput e) {
            System.out.println("\n(fin de l'entrée, arrêt)");
        }
        System.out.println("Au revoir. Fichier final : " + xmlFile.toAbsolutePath());
    }

    private boolean startNewInstitution() throws IOException {
        System.out.println("\nAucune configuration. Que voulez-vous faire ?");
        System.out.println("  1) Construire ma propre architecture");
        System.out.println("  2) Charger l'exemple Université");
        System.out.println("  3) Charger l'exemple Lycée");
        System.out.println("  4) Charger l'exemple Établissement personnalisé (conservatoire)");
        System.out.println("  0) Quitter");
        switch (ask("Choix")) {
            case "1" -> {
                String code = ask("Code de l'établissement (ex. MON_ECOLE)");
                String name = ask("Nom de l'établissement");
                String desc = ask("Description (facultatif)");
                attempt(() -> {
                    module.institutionService().initialize(code, name, desc);
                    System.out.println("Établissement créé. Le fichier XML est généré.");
                });
                return module.institutionService().getStatus()
                        == com.dess.institution.application.ConfigurationStatus.READY;
            }
            case "2" -> loadExample("university");
            case "3" -> loadExample("high-school");
            case "4" -> loadExample("custom-school");
            default -> {
                return false;
            }
        }
        return true;
    }

    private void loadExample(String name) throws IOException {
        try (InputStream example = InteractiveDemo.class.getResourceAsStream("/examples/" + name + ".xml")) {
            if (example == null) {
                throw new IOException("Exemple introuvable : " + name);
            }
            Files.createDirectories(xmlFile.toAbsolutePath().getParent());
            Files.copy(example, xmlFile, StandardCopyOption.REPLACE_EXISTING);
        }
        module = InstitutionModule.open(xmlFile);
        System.out.println("Exemple chargé : " + module.institutionService().getInfo().name());
    }

    // ------------------------------------------------------------------ menu de construction

    private void buildMenu() {
        while (true) {
            System.out.println("\n--- CONSTRUCTION DE L'ARCHITECTURE ---");
            System.out.println("  1) Ajouter un type d'unité");
            System.out.println("  2) Ajouter un attribut à un type");
            System.out.println("  3) Autoriser une relation parent → enfant");
            System.out.println("  4) Créer une unité (avec ses attributs)");
            System.out.println("  5) Afficher l'architecture (types, relations)");
            System.out.println("  6) Afficher l'arbre des unités");
            System.out.println("  7) Afficher le fichier XML généré");
            System.out.println("  8) >>> Passer aux transactions (getById, getByCode, ...)");
            System.out.println("  0) Quitter");
            switch (ask("Choix")) {
                case "1" -> addType();
                case "2" -> addAttribute();
                case "3" -> addPair();
                case "4" -> createUnit();
                case "5" -> showArchitecture();
                case "6" -> showTree();
                case "7" -> showXml();
                case "8" -> transactionMenu();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Choix inconnu.");
            }
        }
    }

    private void addType() {
        String code = ask("Code du type (ex. FACULTE)");
        String label = ask("Libellé");
        String desc = ask("Description (facultatif)");
        attempt(() -> {
            module.unitTypeService().create(new UnitTypeDefinition(code, label, desc, true, List.of()));
            System.out.println("Type « " + code + " » créé. XML mis à jour.");
        });
    }

    private void addAttribute() {
        if (module.unitTypeService().getAll().isEmpty()) {
            System.out.println("Créez d'abord un type.");
            return;
        }
        printTypes();
        String typeCode = ask("Code du type");
        String key = ask("Clé de l'attribut (ex. chefMention)");
        String label = ask("Libellé");
        System.out.println("Types de données : " + Arrays.toString(AttributeType.values()));
        String typeName = ask("Type de donnée").toUpperCase();
        boolean required = ask("Obligatoire ? (o/n)").toLowerCase().startsWith("o");
        String def = ask("Valeur par défaut (facultatif)");
        attempt(() -> {
            AttributeType type = AttributeType.valueOf(typeName);
            module.attributeService().addDefinition(typeCode,
                    AttributeDefinition.of(key, label, type, required, def.isBlank() ? null : def));
            System.out.println("Attribut « " + key + " » ajouté au type « " + typeCode + " ». XML mis à jour.");
        });
    }

    private void addPair() {
        printTypes();
        String parent = ask("Type parent");
        String child = ask("Type enfant");
        attempt(() -> {
            module.unitTypeService().addAllowedPair(parent, child);
            System.out.println("Relation " + parent + " → " + child + " autorisée. XML mis à jour.");
        });
    }

    private void createUnit() {
        printTypes();
        String typeCode = ask("Type de l'unité");
        Optional<UnitTypeDefinition> type = module.unitTypeService().getByCode(typeCode);
        if (type.isEmpty()) {
            System.out.println("Type inconnu : " + typeCode);
            return;
        }
        List<String> parentTypes = module.unitTypeService().getAllowedParentTypes(typeCode);
        System.out.println("Types de parents autorisés : " + (parentTypes.isEmpty() ? "(aucun : unité racine)" : parentTypes));
        showTree();
        String parent = ask("Parent (id ou code, vide = racine)");
        String code = ask("Code de l'unité");
        String name = ask("Nom de l'unité");
        Map<String, String> values = new LinkedHashMap<>();
        for (AttributeDefinition def : type.get().attributes()) {
            String prompt = def.label() + " [" + def.key() + ", " + def.type()
                    + (def.required() ? ", OBLIGATOIRE" : "")
                    + (def.hasDefault() ? ", défaut=" + def.defaultValue() : "") + "]";
            String value = ask(prompt);
            if (!value.isBlank()) {
                values.put(def.key(), value);
            }
        }
        attempt(() -> {
            String parentId = parent.isBlank() ? null : resolve(parent);
            OrganizationalUnit u = module.hierarchyService()
                    .createUnit(CreateUnitCommand.of(code, name, typeCode, parentId, values));
            System.out.println("Unité créée, id = " + u.id() + ". XML mis à jour.");
        });
    }

    // ------------------------------------------------------------------ transactions

    private void transactionMenu() {
        HierarchyService h = module.hierarchyService();
        while (true) {
            System.out.println("\n--- TRANSACTIONS (services) ---");
            System.out.println("  Consultation :");
            System.out.println("   1) getById            2) getByCode          3) getRoots");
            System.out.println("   4) getChildren        5) getParent          6) getDescendants");
            System.out.println("   7) getAncestors       8) getPath            9) getByType");
            System.out.println("  10) getSubtree        11) isDescendantOf    12) areInSameBranch");
            System.out.println("  13) getNearestCommonAncestor");
            System.out.println("  Modification :");
            System.out.println("  20) moveUnit          21) updateUnit        22) deactivateUnit");
            System.out.println("  23) activateUnit      24) deleteUnit        25) setValue (attribut)");
            System.out.println("  26) removeValue       27) getValues d'une unité");
            System.out.println("  Divers :");
            System.out.println("  30) Afficher l'arbre  31) Afficher le XML   32) check() (validation globale)");
            System.out.println("   0) Retour");
            String choice = ask("Choix");
            if (choice.equals("0")) {
                return;
            }
            attempt(() -> runTransaction(choice, h));
        }
    }

    private void runTransaction(String choice, HierarchyService h) {
        switch (choice) {
            case "1" -> printOptional(h.getById(ask("id")));
            case "2" -> printOptional(h.getByCode(ask("code")));
            case "3" -> printUnits(h.getRoots());
            case "4" -> printUnits(h.getChildren(resolve(ask("unité (id ou code)"))));
            case "5" -> printOptional(h.getParent(resolve(ask("unité (id ou code)"))));
            case "6" -> printUnits(h.getDescendants(resolve(ask("unité (id ou code)"))));
            case "7" -> printUnits(h.getAncestors(resolve(ask("unité (id ou code)"))));
            case "8" -> {
                List<OrganizationalUnit> path = h.getPath(resolve(ask("unité (id ou code)")));
                System.out.println(String.join(" > ", path.stream().map(OrganizationalUnit::name).toList()));
            }
            case "9" -> printUnits(h.getByType(ask("code du type")));
            case "10" -> printUnits(h.getSubtree(resolve(ask("unité (id ou code)"))));
            case "11" -> System.out.println(h.isDescendantOf(
                    resolve(ask("unité candidate")), resolve(ask("ancêtre supposé"))));
            case "12" -> System.out.println(h.areInSameBranch(resolve(ask("unité A")), resolve(ask("unité B"))));
            case "13" -> printOptional(h.getNearestCommonAncestor(resolve(ask("unité A")), resolve(ask("unité B"))));
            case "20" -> {
                String id = resolve(ask("unité à déplacer"));
                String target = ask("nouveau parent (id ou code, vide = racine)");
                OrganizationalUnit moved = h.moveUnit(id, target.isBlank() ? null : resolve(target));
                System.out.println("Déplacée (id inchangé : " + moved.id() + "). XML mis à jour.");
            }
            case "21" -> {
                String id = resolve(ask("unité à modifier"));
                String code = ask("nouveau code (vide = inchangé)");
                String name = ask("nouveau nom (vide = inchangé)");
                h.updateUnit(id, new UpdateUnitCommand(code.isBlank() ? null : code,
                        name.isBlank() ? null : name, null, null));
                System.out.println("Unité modifiée. XML mis à jour.");
            }
            case "22" -> {
                String id = resolve(ask("unité à désactiver"));
                boolean cascade = ask("En cascade sur les descendants ? (o/n)").toLowerCase().startsWith("o");
                h.deactivateUnit(id, cascade);
                System.out.println("Désactivée. XML mis à jour.");
            }
            case "23" -> {
                h.activateUnit(resolve(ask("unité à réactiver")));
                System.out.println("Réactivée. XML mis à jour.");
            }
            case "24" -> {
                h.deleteUnit(resolve(ask("unité à supprimer")));
                System.out.println("Supprimée. XML mis à jour.");
            }
            case "25" -> {
                String id = resolve(ask("unité"));
                String key = ask("clé de l'attribut");
                String value = ask("valeur");
                module.attributeService().setValue(id, key, value);
                System.out.println("Valeur enregistrée (validée selon le type). XML mis à jour.");
            }
            case "26" -> {
                module.attributeService().removeValue(resolve(ask("unité")), ask("clé de l'attribut"));
                System.out.println("Valeur supprimée. XML mis à jour.");
            }
            case "27" -> module.attributeService().getValues(resolve(ask("unité")))
                    .forEach(v -> System.out.println("  " + v.key() + " = " + v.value()));
            case "30" -> showTree();
            case "31" -> showXml();
            case "32" -> {
                var report = module.institutionService().check();
                System.out.println(report.valid() ? "Configuration valide." : "Erreurs : " + report.errors());
            }
            default -> System.out.println("Choix inconnu.");
        }
    }

    // ------------------------------------------------------------------ affichage

    private void printTypes() {
        System.out.println("Types existants :");
        for (UnitTypeDefinition t : module.unitTypeService().getAll()) {
            System.out.println("  - " + t.code() + " (" + t.label() + ")" + (t.active() ? "" : " [inactif]"));
        }
    }

    private void showArchitecture() {
        System.out.println("\nTypes d'unités :");
        for (UnitTypeDefinition t : module.unitTypeService().getAll()) {
            System.out.println("  " + t.code() + " — " + t.label() + (t.active() ? "" : " [inactif]"));
            for (AttributeDefinition a : t.attributes()) {
                System.out.println("      · " + a.key() + " : " + a.type() + (a.required() ? " (obligatoire)" : "")
                        + (a.hasDefault() ? " défaut=" + a.defaultValue() : ""));
            }
        }
        System.out.println("Relations autorisées :");
        for (AllowedParentChildPair p : module.unitTypeService().getAllowedPairs()) {
            System.out.println("  " + p.parent() + " → " + p.child());
        }
    }

    private void showTree() {
        List<OrganizationalUnit> roots = module.hierarchyService().getRoots();
        if (roots.isEmpty()) {
            System.out.println("(aucune unité)");
        }
        for (OrganizationalUnit r : roots) {
            printNode(r, "");
        }
    }

    private void printNode(OrganizationalUnit u, String indent) {
        System.out.println(indent + "└ " + describe(u));
        for (OrganizationalUnit child : module.hierarchyService().getChildren(u.id())) {
            printNode(child, indent + "   ");
        }
    }

    private void showXml() {
        try {
            System.out.println("\n----- " + xmlFile.toAbsolutePath() + " -----");
            System.out.println(Files.readString(xmlFile));
            System.out.println("----- fin du XML -----");
        } catch (IOException e) {
            System.out.println("Fichier XML non lisible : " + e.getMessage());
        }
    }

    private static String describe(OrganizationalUnit u) {
        return u.name() + " [" + u.typeCode() + ", code=" + u.code() + ", id=" + u.id()
                + (u.active() ? "" : ", INACTIVE") + "]"
                + (u.values().isEmpty() ? "" : " " + u.values().values().stream()
                .map(v -> v.key() + "=" + v.value()).toList());
    }

    private static void printUnits(List<OrganizationalUnit> units) {
        if (units.isEmpty()) {
            System.out.println("(aucun résultat)");
        }
        units.forEach(u -> System.out.println("  " + describe(u)));
    }

    private static void printOptional(Optional<OrganizationalUnit> unit) {
        System.out.println(unit.map(u -> "  " + describe(u)).orElse("(aucun résultat)"));
    }

    // ------------------------------------------------------------------ utilitaires

    /** Accepte un id ou un code d'unité et retourne l'id. */
    private String resolve(String idOrCode) {
        HierarchyService h = module.hierarchyService();
        return h.getById(idOrCode).or(() -> h.getByCode(idOrCode)).map(OrganizationalUnit::id)
                .orElseThrow(() -> new com.dess.institution.exception.NotFoundException(
                        "Aucune unité avec l'id ou le code « " + idOrCode + " »"));
    }

    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (ValidationException e) {
            System.out.println("REFUSÉ (la configuration n'a pas changé) :");
            e.getViolations().forEach(v -> System.out.println("  ✗ " + v));
        } catch (InstitutionException | IllegalArgumentException e) {
            System.out.println("ERREUR : " + e.getMessage());
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt + " > ");
        System.out.flush();
        try {
            return in.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new EndOfInput();
        }
    }

    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
