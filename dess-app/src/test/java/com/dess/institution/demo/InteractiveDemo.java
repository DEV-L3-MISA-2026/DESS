package com.dess.institution.demo;

import com.dess.institution.InstitutionModule;
import com.dess.institution.application.ConfigurationStatus;
import com.dess.institution.application.CreateUnitCommand;
import com.dess.institution.application.HierarchyService;
import com.dess.institution.application.UpdateUnitCommand;
import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeType;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.InstitutionException;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
 * @brief Interactive console demonstration of the institution module.
 *
 * <p>The demonstration follows three steps:</p>
 *
 * <ol>
 *   <li>the user builds an architecture (types, attributes, relations, units) or loads one of the
 *       bundled examples;</li>
 *   <li>the XML file is regenerated after every modification and can be displayed;</li>
 *   <li>a transaction menu calls the services directly ({@code getById}, {@code getByCode},
 *       {@code moveUnit} and so on).</li>
 * </ol>
 *
 * <p>See the README for the launch command. The XML file is written to a temporary directory, or to
 * the path supplied as the first argument; it is never written into the project files.</p>
 */
public final class InteractiveDemo
{
    private final Scanner in;
    private final Path xmlFile;
    private InstitutionModule module;

    private InteractiveDemo(Scanner in, Path xmlFile)
    {
        this.in = in;
        this.xmlFile = xmlFile;
    }

    /**
     * Starts the interactive demonstration.
     *
     * @param args optional output XML file path
     * @throws IOException if the temporary directory or the XML file cannot be created
     */
    public static void main(String[] args) throws IOException
    {
        // Correct accents even when the terminal locale is not UTF-8.
        System.setOut(new PrintStream(
            new java.io.FileOutputStream(java.io.FileDescriptor.out),
            true,
            StandardCharsets.UTF_8
        ));

        Path file = args.length > 0
            ? Path.of(args[0])
            : Files.createTempDirectory("institution-demo").resolve("institution.xml");

        try (Scanner scanner = new Scanner(System.in))
        {
            new InteractiveDemo(scanner, file).run();
        }
    }

    // ------------------------------------------------------------------ main flow

    private void run() throws IOException
    {
        System.out.println("=== Interactive demonstration of the institution module ===");
        System.out.println("XML file: " + xmlFile.toAbsolutePath());

        try
        {
            module = InstitutionModule.open(xmlFile);

            if (module.institutionService().getStatus() == ConfigurationStatus.READY)
            {
                System.out.println("Existing configuration loaded: "
                                   + module.institutionService().getInfo().name());
            }
            else if (!startNewInstitution())
            {
                return;
            }

            buildMenu();
        }
        catch (EndOfInput e)
        {
            System.out.println("\n(end of input, stopping)");
        }

        System.out.println("Goodbye. Final file: " + xmlFile.toAbsolutePath());
    }

    private boolean startNewInstitution() throws IOException
    {
        System.out.println("\nNo configuration. What do you want to do?");
        System.out.println("  1) Build my own architecture");
        System.out.println("  2) Load the University example");
        System.out.println("  3) Load the High School example");
        System.out.println("  4) Load the Custom institution example (conservatory)");
        System.out.println("  0) Quit");

        switch (ask("Choice"))
        {
            case "1" ->
            {
                String code = ask("Institution code (e.g. MY_SCHOOL)");
                String name = ask("Institution name");
                String description = ask("Description (optional)");

                attempt(() ->
                {
                    module.institutionService().initialize(code, name, description);
                    System.out.println("Institution created. The XML file has been generated.");
                });

                return module.institutionService().getStatus() == ConfigurationStatus.READY;
            }

            case "2" -> loadExample("university");

            case "3" -> loadExample("high-school");

            case "4" -> loadExample("custom-school");

            default ->
            {
                return false;
            }
        }

        return true;
    }

    private void loadExample(String name) throws IOException
    {
        try (InputStream example = InteractiveDemo.class.getResourceAsStream("/examples/" + name + ".xml"))
        {
            if (example == null)
            {
                throw new IOException("Example not found: " + name);
            }

            Files.createDirectories(xmlFile.toAbsolutePath().getParent());
            Files.copy(example, xmlFile, StandardCopyOption.REPLACE_EXISTING);
        }

        module = InstitutionModule.open(xmlFile);
        System.out.println("Example loaded: " + module.institutionService().getInfo().name());
    }

    // ------------------------------------------------------------------ architecture menu

    private void buildMenu()
    {
        while (true)
        {
            System.out.println("\n--- ARCHITECTURE CONSTRUCTION ---");
            System.out.println("  1) Add a unit type");
            System.out.println("  2) Add an attribute to a type");
            System.out.println("  3) Allow a parent → child relation");
            System.out.println("  4) Create a unit (with its attributes)");
            System.out.println("  5) Show the architecture (types, relations)");
            System.out.println("  6) Show the unit tree");
            System.out.println("  7) Show the generated XML file");
            System.out.println("  8) >>> Switch to transactions (getById, getByCode, ...)");
            System.out.println("  0) Quit");

            switch (ask("Choice"))
            {
                case "1" -> addType();

                case "2" -> addAttribute();

                case "3" -> addPair();

                case "4" -> createUnit();

                case "5" -> showArchitecture();

                case "6" -> showTree();

                case "7" -> showXml();

                case "8" -> transactionMenu();

                case "0" ->
                {
                    return;
                }

                default -> System.out.println("Unknown choice.");
            }
        }
    }

    private void addType()
    {
        String code = ask("Type code (e.g. FACULTY)");
        String label = ask("Label");
        String description = ask("Description (optional)");

        attempt(() ->
        {
            module.unitTypeService().create(new UnitTypeDefinition(code, label, description, true, List.of()));
            System.out.println("Type '" + code + "' created. XML updated.");
        });
    }

    private void addAttribute()
    {
        if (module.unitTypeService().getAll().isEmpty())
        {
            System.out.println("Create a type first.");
            return;
        }

        printTypes();

        String typeCode = ask("Type code");
        String key = ask("Attribute key (e.g. departmentHead)");
        String label = ask("Label");

        System.out.println("Data types: " + Arrays.toString(AttributeType.values()));

        String typeName = ask("Data type").toUpperCase();
        boolean required = ask("Required? (y/n)").toLowerCase().startsWith("y");
        String defaultValue = ask("Default value (optional)");

        attempt(() ->
        {
            AttributeType type = AttributeType.valueOf(typeName);

            module.attributeService().addDefinition(
                typeCode,
                AttributeDefinition.of(key, label, type, required, defaultValue.isBlank() ? null : defaultValue)
            );

            System.out.println("Attribute '" + key + "' added to type '" + typeCode + "'. XML updated.");
        });
    }

    private void addPair()
    {
        printTypes();

        String parent = ask("Parent type");
        String child = ask("Child type");

        attempt(() ->
        {
            module.unitTypeService().addAllowedPair(parent, child);
            System.out.println("Relation " + parent + " → " + child + " allowed. XML updated.");
        });
    }

    private void createUnit()
    {
        printTypes();

        String typeCode = ask("Unit type");
        Optional<UnitTypeDefinition> type = module.unitTypeService().getByCode(typeCode);

        if (type.isEmpty())
        {
            System.out.println("Unknown type: " + typeCode);
            return;
        }

        List<String> parentTypes = module.unitTypeService().getAllowedParentTypes(typeCode);

        System.out.println("Allowed parent types: "
                           + (parentTypes.isEmpty() ? "(none: root unit)" : parentTypes));

        showTree();

        String parent = ask("Parent (id or code, empty = root)");
        String code = ask("Unit code");
        String name = ask("Unit name");
        Map<String, String> values = new LinkedHashMap<>();

        for (AttributeDefinition definition : type.get().attributes())
        {
            String prompt = definition.label() + " [" + definition.key() + ", " + definition.type()
                            + (definition.required() ? ", REQUIRED" : "")
                            + (definition.hasDefault() ? ", default=" + definition.defaultValue() : "") + "]";
            String value = ask(prompt);

            if (!value.isBlank())
            {
                values.put(definition.key(), value);
            }
        }

        attempt(() ->
        {
            String parentId = parent.isBlank() ? null : resolve(parent);
            OrganizationalUnit unit = module.hierarchyService()
                                               .createUnit(CreateUnitCommand.of(
                                                   code, name, typeCode, parentId, values));

            System.out.println("Unit created, id = " + unit.id() + ". XML updated.");
        });
    }

    // ------------------------------------------------------------------ transactions

    private void transactionMenu()
    {
        HierarchyService hierarchy = module.hierarchyService();

        while (true)
        {
            System.out.println("\n--- TRANSACTIONS (services) ---");
            System.out.println("  Read:");
            System.out.println("   1) getById            2) getByCode          3) getRoots");
            System.out.println("   4) getChildren        5) getParent          6) getDescendants");
            System.out.println("   7) getAncestors       8) getPath            9) getByType");
            System.out.println("  10) getSubtree        11) isDescendantOf    12) areInSameBranch");
            System.out.println("  13) getNearestCommonAncestor");
            System.out.println("  Write:");
            System.out.println("  20) moveUnit          21) updateUnit        22) deactivateUnit");
            System.out.println("  23) activateUnit      24) deleteUnit        25) setValue (attribute)");
            System.out.println("  26) removeValue       27) getValues of a unit");
            System.out.println("  Miscellaneous:");
            System.out.println("  30) Show tree         31) Show XML           32) check() (global validation)");
            System.out.println("   0) Back");

            String choice = ask("Choice");

            if (choice.equals("0"))
            {
                return;
            }

            attempt(() -> runTransaction(choice, hierarchy));
        }
    }

    private void runTransaction(String choice, HierarchyService hierarchy)
    {
        switch (choice)
        {
            case "1" -> printOptional(hierarchy.getById(ask("id")));

            case "2" -> printOptional(hierarchy.getByCode(ask("code")));

            case "3" -> printUnits(hierarchy.getRoots());

            case "4" -> printUnits(hierarchy.getChildren(resolve(ask("unit (id or code)"))));

            case "5" -> printOptional(hierarchy.getParent(resolve(ask("unit (id or code)"))));

            case "6" -> printUnits(hierarchy.getDescendants(resolve(ask("unit (id or code)"))));

            case "7" -> printUnits(hierarchy.getAncestors(resolve(ask("unit (id or code)"))));

            case "8" ->
            {
                List<OrganizationalUnit> path = hierarchy.getPath(resolve(ask("unit (id or code)")));
                System.out.println(String.join(" > ", path.stream().map(OrganizationalUnit::name).toList()));
            }

            case "9" -> printUnits(hierarchy.getByType(ask("type code")));

            case "10" -> printUnits(hierarchy.getSubtree(resolve(ask("unit (id or code)"))));

            case "11" -> System.out.println(hierarchy.isDescendantOf(
                resolve(ask("candidate unit")),
                resolve(ask("assumed ancestor"))
            ));

            case "12" -> System.out.println(hierarchy.areInSameBranch(
                resolve(ask("unit A")),
                resolve(ask("unit B"))
            ));

            case "13" -> printOptional(hierarchy.getNearestCommonAncestor(
                resolve(ask("unit A")),
                resolve(ask("unit B"))
            ));

            case "20" ->
            {
                String id = resolve(ask("unit to move"));
                String target = ask("new parent (id or code, empty = root)");
                OrganizationalUnit moved = hierarchy.moveUnit(id, target.isBlank() ? null : resolve(target));

                System.out.println("Moved (id unchanged: " + moved.id() + "). XML updated.");
            }

            case "21" ->
            {
                String id = resolve(ask("unit to update"));
                String code = ask("new code (empty = unchanged)");
                String name = ask("new name (empty = unchanged)");

                hierarchy.updateUnit(id, new UpdateUnitCommand(
                    code.isBlank() ? null : code,
                    name.isBlank() ? null : name,
                    null,
                    null
                ));

                System.out.println("Unit updated. XML updated.");
            }

            case "22" ->
            {
                String id = resolve(ask("unit to deactivate"));
                boolean cascade = ask("Cascade to descendants? (y/n)").toLowerCase().startsWith("y");

                hierarchy.deactivateUnit(id, cascade);
                System.out.println("Deactivated. XML updated.");
            }

            case "23" ->
            {
                hierarchy.activateUnit(resolve(ask("unit to reactivate")));
                System.out.println("Reactivated. XML updated.");
            }

            case "24" ->
            {
                hierarchy.deleteUnit(resolve(ask("unit to delete")));
                System.out.println("Deleted. XML updated.");
            }

            case "25" ->
            {
                String id = resolve(ask("unit"));
                String key = ask("attribute key");
                String value = ask("value");

                module.attributeService().setValue(id, key, value);
                System.out.println("Value saved (validated against the type). XML updated.");
            }

            case "26" ->
            {
                module.attributeService().removeValue(resolve(ask("unit")), ask("attribute key"));
                System.out.println("Value deleted. XML updated.");
            }

            case "27" -> module.attributeService()
                                .getValues(resolve(ask("unit")))
                                .forEach(value -> System.out.println("  " + value.key() + " = " + value.value()));

            case "30" -> showTree();

            case "31" -> showXml();

            case "32" ->
            {
                var report = module.institutionService().check();

                System.out.println(report.valid() ? "Configuration valid." : "Errors: " + report.errors());
            }

            default -> System.out.println("Unknown choice.");
        }
    }

    // ------------------------------------------------------------------ display

    private void printTypes()
    {
        System.out.println("Existing types:");

        for (UnitTypeDefinition type : module.unitTypeService().getAll())
        {
            System.out.println("  - " + type.code() + " (" + type.label() + ")"
                               + (type.active() ? "" : " [inactive]"));
        }
    }

    private void showArchitecture()
    {
        System.out.println("\nUnit types:");

        for (UnitTypeDefinition type : module.unitTypeService().getAll())
        {
            System.out.println("  " + type.code() + " — " + type.label() + (type.active() ? "" : " [inactive]"));

            for (AttributeDefinition attribute : type.attributes())
            {
                System.out.println("      · " + attribute.key() + " : " + attribute.type()
                                   + (attribute.required() ? " (required)" : "")
                                   + (attribute.hasDefault() ? " default=" + attribute.defaultValue() : ""));
            }
        }

        System.out.println("Allowed relations:");

        for (AllowedParentChildPair pair : module.unitTypeService().getAllowedPairs())
        {
            System.out.println("  " + pair.parent() + " → " + pair.child());
        }
    }

    private void showTree()
    {
        List<OrganizationalUnit> roots = module.hierarchyService().getRoots();

        if (roots.isEmpty())
        {
            System.out.println("(no unit)");
        }

        for (OrganizationalUnit root : roots)
        {
            printNode(root, "");
        }
    }

    private void printNode(OrganizationalUnit unit, String indent)
    {
        System.out.println(indent + "└ " + describe(unit));

        for (OrganizationalUnit child : module.hierarchyService().getChildren(unit.id()))
        {
            printNode(child, indent + "   ");
        }
    }

    private void showXml()
    {
        try
        {
            System.out.println("\n----- " + xmlFile.toAbsolutePath() + " -----");
            System.out.println(Files.readString(xmlFile));
            System.out.println("----- end of XML -----");
        }
        catch (IOException e)
        {
            System.out.println("XML file unreadable: " + e.getMessage());
        }
    }

    private static String describe(OrganizationalUnit unit)
    {
        return unit.name() + " [" + unit.typeCode() + ", code=" + unit.code() + ", id=" + unit.id()
               + (unit.active() ? "" : ", INACTIVE") + "]"
               + (unit.values().isEmpty()
                   ? ""
                   : " " + unit.values().values().stream().map(value -> value.key() + "=" + value.value()).toList());
    }

    private static void printUnits(List<OrganizationalUnit> units)
    {
        if (units.isEmpty())
        {
            System.out.println("(no result)");
        }

        units.forEach(unit -> System.out.println("  " + describe(unit)));
    }

    private static void printOptional(Optional<OrganizationalUnit> unit)
    {
        System.out.println(unit.map(found -> "  " + describe(found)).orElse("(no result)"));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Resolves an identifier or a unit code to the unit identifier.
     *
     * @param idOrCode unit identifier or business code
     * @return the matching unit identifier
     * @throws NotFoundException if no unit matches
     */
    private String resolve(String idOrCode)
    {
        HierarchyService hierarchy = module.hierarchyService();

        return hierarchy.getById(idOrCode)
                        .or(() -> hierarchy.getByCode(idOrCode))
                        .map(OrganizationalUnit::id)
                        .orElseThrow(() -> new NotFoundException(
                            "No unit with id or code '" + idOrCode + "'"));
    }

    private void attempt(Runnable action)
    {
        try
        {
            action.run();
        }
        catch (ValidationException e)
        {
            System.out.println("REFUSED (the configuration did not change):");
            e.getViolations().forEach(violation -> System.out.println("  ✗ " + violation));
        }
        catch (InstitutionException | IllegalArgumentException e)
        {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private String ask(String prompt)
    {
        System.out.print(prompt + " > ");
        System.out.flush();

        try
        {
            return in.nextLine().trim();
        }
        catch (NoSuchElementException e)
        {
            throw new EndOfInput();
        }
    }

    private static final class EndOfInput extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
    }
}
