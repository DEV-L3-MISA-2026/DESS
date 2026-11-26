<%--
    Institution tree editor.

    The page uses the CDI producers of the institution module and never touches the XML
    infrastructure directly. Read actions and form submissions follow the post/redirect/get
    pattern; feedback is carried through the session.

    The page follows the order of the work: unit types, allowed relations, hierarchy, settings.
    The hierarchy is a server-side SVG tree. Each node is a link to its details and has a + control
    that opens the create dialog with that node as parent. A unit is moved with the parent selector.
    The page is a development tool, not a production user interface.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dess.institution.InstitutionSettings" %>
<%@ page import="com.dess.institution.application.ConfigurationStatus" %>
<%@ page import="com.dess.institution.application.CreateUnitCommand" %>
<%@ page import="com.dess.institution.application.HierarchyService" %>
<%@ page import="com.dess.institution.application.InstitutionInfo" %>
<%@ page import="com.dess.institution.application.InstitutionService" %>
<%@ page import="com.dess.institution.application.UnitTypeService" %>
<%@ page import="com.dess.institution.application.UpdateUnitCommand" %>
<%@ page import="com.dess.institution.application.ValidationReport" %>
<%@ page import="com.dess.institution.domain.AllowedParentChildPair" %>
<%@ page import="com.dess.institution.domain.AttributeDefinition" %>
<%@ page import="com.dess.institution.domain.AttributeType" %>
<%@ page import="com.dess.institution.domain.AttributeValue" %>
<%@ page import="com.dess.institution.domain.InstitutionConfig" %>
<%@ page import="com.dess.institution.domain.OrganizationalUnit" %>
<%@ page import="com.dess.institution.domain.UnitTypeDefinition" %>
<%@ page import="com.dess.institution.exception.ConfigurationLoadException" %>
<%@ page import="com.dess.institution.exception.InstitutionException" %>
<%@ page import="com.dess.institution.exception.ValidationException" %>
<%@ page import="jakarta.enterprise.inject.spi.CDI" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.nio.file.StandardCopyOption" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="com.dess.security.domain.RoleCatalog" %>
<%@ page import="com.dess.security.web.CsrfTokens" %>
<%@ page import="com.dess.security.web.SessionUser" %>
<%
    String csrfToken = CsrfTokens.ensure(session);
    // The filter enforces this server-side; the page only uses it to choose what to render.
    SessionUser sessionUser = (SessionUser) session.getAttribute(SessionUser.SESSION_ATTRIBUTE);
    boolean canWrite = sessionUser != null
            && sessionUser.isPermitted(RoleCatalog.INSTITUTION_WRITE);
%>
<c:set var="activeNav" value="institution"/>

<%!
    private static final List<String> EXAMPLES = List.of("university", "high-school", "custom-school");

    private static final int GRAPH_NODE_W = 220;
    private static final int GRAPH_NODE_H = 78;
    // The node title is 14px semibold and the status dot starts at x=200, so a longer title
    // would run into the dot. Twenty-two characters keep a visible gap at that size.
    private static final int GRAPH_NODE_TITLE_MAX = 22;
    private static final int GRAPH_H_GAP = 34;
    private static final int GRAPH_V_GAP = 86;
    private static final int GRAPH_MARGIN = 32;


    /**
     * @brief One node of the display-only SVG tree layout.
     */
    private static final class GraphBox
    {
        private final OrganizationalUnit unit;
        private final List<GraphBox> children = new ArrayList<>();
        private GraphBox parent;
        private int x;
        private int y;
        private int width;

        private GraphBox(OrganizationalUnit unit)
        {
            this.unit = unit;
        }
    }

    /**
     * @brief One parent-child cable of the display graph.
     *
     * <p>The identifiers mark the cables that touch the selected unit.</p>
     */
    private static final class GraphEdge
    {
        private final String parentId;
        private final String childId;
        private final String path;

        private GraphEdge(String parentId, String childId, String path)
        {
            this.parentId = parentId;
            this.childId = childId;
            this.path = path;
        }
    }

    /**
     * Escapes a value for HTML text and attribute contexts.
     *
     * @param value value to escape; {@code null} becomes an empty string
     * @return the escaped value
     */
    static String esc(String value)
    {
        if (value == null)
        {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    /**
     * Trims a request parameter.
     *
     * @param value raw value
     * @return the trimmed value, or {@code null}
     */
    static String trim(String value)
    {
        return value == null ? null : value.trim();
    }

    /**
     * Truncates a display label on a single SVG line.
     *
     * @param value source value
     * @param max   maximum number of characters
     * @return the truncated label
     */
    static String clipText(String value, int max)
    {
        if (value == null)
        {
            return "";
        }

        String normalized = value.strip();

        return normalized.length() <= max
            ? normalized
            : normalized.substring(0, Math.max(0, max - 1)) + "…";
    }

    /**
     * Displays an optional value, using an em dash when it is absent.
     *
     * @param value value to display
     * @return the value or {@code "—"}
     */
    static String orDash(String value)
    {
        return value == null || value.isBlank() ? "—" : value;
    }

    /**
     * Returns the deepest cause message of a failure.
     *
     * @param error failure to inspect
     * @return a non-empty human-readable message
     */
    static String rootMessage(Throwable error)
    {
        Throwable cause = error;

        while (cause.getCause() != null && cause.getCause() != cause)
        {
            cause = cause.getCause();
        }

        return cause.getMessage() == null || cause.getMessage().isBlank()
            ? cause.getClass().getSimpleName()
            : cause.getMessage();
    }

    /**
     * Builds an indentation prefix for a select option.
     *
     * @param depth tree depth
     * @return a repeated em dash prefix
     */
    static String depthPrefix(int depth)
    {
        return "— ".repeat(Math.max(0, depth));
    }

    /**
     * Encodes a query-string value.
     *
     * @param value raw value
     * @return the encoded value
     */
    static String urlEncode(String value)
    {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Returns the page section named by the fragment of a redirect target.
     *
     * @param target redirect target
     * @return the section identifier, or an empty string
     */
    static String sectionOf(String target)
    {
        int hash = target.indexOf('#');

        return hash < 0 ? "" : target.substring(hash + 1);
    }

    /**
     * Builds the feedback message when it belongs to the given section.
     *
     * <p>The browser jumps to the section after a form is sent, so the message must be there too.</p>
     *
     * @param section section being rendered
     * @param target  section that the message belongs to
     * @param kind    {@code "error"} or {@code "ok"}
     * @param text    message, or {@code null} when there is none
     * @return the HTML alert, or an empty string
     */
    static String flash(String section, String target, String kind, String text)
    {
        if (text == null || !section.equals(target))
        {
            return "";
        }

        boolean error = "error".equals(kind);

        return "<div class=\"flash " + (error ? "error" : "ok") + "\" role=\""
            + (error ? "alert" : "status") + "\">" + esc(text) + "</div>";
    }

    /**
     * Returns the HTML input type matching an attribute type.
     *
     * @param type attribute type
     * @return an HTML input type
     */
    static String inputType(AttributeType type)
    {
        if (type == AttributeType.NUMBER || type == AttributeType.DECIMAL)
        {
            return "number";
        }

        if (type == AttributeType.DATE)
        {
            return "date";
        }

        if (type == AttributeType.EMAIL)
        {
            return "email";
        }

        if (type == AttributeType.URL)
        {
            return "url";
        }

        return "text";
    }

    /**
     * Returns the HTML step attribute for a numeric input.
     *
     * @param type attribute type
     * @return the step attribute, or an empty string
     */
    static String inputStep(AttributeType type)
    {
        if (type == AttributeType.NUMBER)
        {
            return " step=\"1\"";
        }

        if (type == AttributeType.DECIMAL)
        {
            return " step=\"any\"";
        }

        return "";
    }

    /**
     * Appends an HTML hidden input.
     *
     * @param html  target buffer
     * @param name  input name
     * @param value input value
     */
    static void appendHidden(StringBuilder html, String name, String value)
    {
        html.append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"")
            .append(esc(value)).append("\">");
    }

    /**
     * Builds a small POST action form with a single hidden field.
     *
     * @param action  value of the {@code action} parameter
     * @param field   name of the hidden field carrying the target
     * @param value   value of the hidden field
     * @param label   button label
     * @param css     additional button CSS classes
     * @param confirmMessage confirmation message, or {@code null} when no confirmation is needed
     * @return the HTML form
     */
    static String postButton(String csrfToken, String action, String field, String value, String label, String css,
                             String confirmMessage)
    {
        StringBuilder html = new StringBuilder("<form method=\"post\" class=\"inline\"");

        if (confirmMessage != null && !confirmMessage.isBlank())
        {
            html.append(" data-confirm=\"").append(esc(confirmMessage)).append('"');
        }

        html.append('>');
        appendHidden(html, "csrfToken", csrfToken);
        appendHidden(html, "action", action);
        appendHidden(html, field, value);
        html.append("<button type=\"submit\" class=\"btn btn-sm ").append(esc(css)).append("\">")
            .append(esc(label)).append("</button></form>");

        return html.toString();
    }

    /**
     * Builds the display tree of a unit and its descendants.
     *
     * @param config  configuration owning the units
     * @param unit    subtree root
     * @param visited identifiers already placed, used to stop cycles
     * @return the display node
     */
    static GraphBox graphTree(InstitutionConfig config, OrganizationalUnit unit, Set<String> visited)
    {
        GraphBox box = new GraphBox(unit);
        visited.add(unit.id());

        for (OrganizationalUnit child : config.childrenOf(unit.id()))
        {
            if (!visited.contains(child.id()))
            {
                GraphBox childBox = graphTree(config, child, visited);
                childBox.parent = box;
                box.children.add(childBox);
            }
        }

        return box;
    }

    /**
     * Measures a display subtree.
     *
     * @param box subtree root
     * @return the width required by the subtree
     */
    static int graphMeasure(GraphBox box)
    {
        if (box.children.isEmpty())
        {
            box.width = GRAPH_NODE_W;
        }
        else
        {
            int width = 0;

            for (int index = 0; index < box.children.size(); index++)
            {
                if (index > 0)
                {
                    width += GRAPH_H_GAP;
                }

                width += graphMeasure(box.children.get(index));
            }

            box.width = Math.max(GRAPH_NODE_W, width);
        }

        return box.width;
    }

    /**
     * Assigns coordinates to a measured display subtree.
     *
     * @param box   subtree root
     * @param left  left coordinate available for the subtree
     * @param depth depth of the subtree root
     */
    static void graphPosition(GraphBox box, int left, int depth)
    {
        box.y = GRAPH_MARGIN + depth * (GRAPH_NODE_H + GRAPH_V_GAP);

        if (box.children.isEmpty())
        {
            box.x = left + (box.width - GRAPH_NODE_W) / 2;
            return;
        }

        int cursor = left;

        for (GraphBox child : box.children)
        {
            graphPosition(child, cursor, depth + 1);
            cursor += child.width + GRAPH_H_GAP;
        }

        GraphBox first = box.children.get(0);
        GraphBox last = box.children.get(box.children.size() - 1);
        int center = (first.x + last.x + GRAPH_NODE_W) / 2;
        box.x = Math.max(left, Math.min(center - GRAPH_NODE_W / 2, left + box.width - GRAPH_NODE_W));
    }

    /**
     * Collects display nodes and edge paths in depth-first order.
     *
     * @param box   subtree root
     * @param nodes target node list
     * @param edges target edge list
     */
    static void graphCollect(GraphBox box, List<GraphBox> nodes, List<GraphEdge> edges)
    {
        nodes.add(box);

        if (box.parent != null)
        {
            int x1 = box.parent.x + GRAPH_NODE_W / 2;
            int y1 = box.parent.y + GRAPH_NODE_H;
            int x2 = box.x + GRAPH_NODE_W / 2;
            int y2 = box.y;
            int bend = Math.max(40, Math.abs(y2 - y1) / 2);

            edges.add(new GraphEdge(
                box.parent.unit.id(),
                box.unit.id(),
                "M" + x1 + "," + y1 + " C" + x1 + "," + (y1 + bend) + " "
                    + x2 + "," + (y2 - bend) + " " + x2 + "," + y2
            ));
        }

        for (GraphBox child : box.children)
        {
            graphCollect(child, nodes, edges);
        }
    }
%>
<%
    request.setCharacterEncoding("UTF-8");

    InstitutionService institutionService;
    UnitTypeService unitTypeService;
    HierarchyService hierarchyService;

    try
    {
        institutionService = CDI.current().select(InstitutionService.class).get();
        unitTypeService = CDI.current().select(UnitTypeService.class).get();
        hierarchyService = CDI.current().select(HierarchyService.class).get();
    }
    catch (RuntimeException error)
    {
        response.setStatus(500);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Institution unavailable · DESS</title>
    <link rel="icon" href="<%= request.getContextPath() %>/favicon.ico" sizes="any">
    <link rel="icon" type="image/png" href="<%= request.getContextPath() %>/assets/img/app-icon-192.png">
    <link rel="apple-touch-icon" href="<%= request.getContextPath() %>/assets/img/app-icon-192.png">
    <script>document.documentElement.classList.add('js');</script>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/tokens.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/institution.css">
    <script src="<%= request.getContextPath() %>/assets/js/shell.js" defer></script>
</head>
<body>
<a class="dess-skip-link" href="#main-content">Skip to main content</a>
<div class="dess-shell">
    <%@ include file="/WEB-INF/views/components/sidebar.jsp" %>
    <div class="dess-main">
        <header class="dess-topbar">
            <div class="dess-topbar-context">
                <button type="button" class="dess-mobile-menu" data-nav-toggle
                        aria-expanded="false" aria-controls="dess-sidebar">Menu</button>
                <span class="dess-topbar-title">Institution module</span>
                <span class="dess-topbar-separator" aria-hidden="true">/</span>
                <span>Configuration</span>
            </div>
            <div class="dess-topbar-right">
                <%@ include file="/WEB-INF/views/components/user-menu.jspf" %>
            </div>
        </header>
        <main id="main-content" class="dess-content" tabindex="-1">
            <div class="dess-page-header">
                <div class="dess-page-heading">
                    <nav class="dess-breadcrumb" aria-label="Breadcrumb">
                        <ol>
                            <li>Institution</li>
                            <li aria-current="page">Unavailable</li>
                        </ol>
                    </nav>
                    <h1 class="dess-page-title">Institution unavailable</h1>
                    <p class="dess-page-description">
                        The institution module could not be opened because its configuration was not loaded.
                    </p>
                </div>
            </div>

            <div class="flash error" role="alert">
                <strong>The institution module could not be opened.</strong><br>
                <%= esc(rootMessage(error)) %>
            </div>

            <section class="card" aria-labelledby="recovery-title">
                <h2 id="recovery-title">What to check</h2>
                <ol>
                    <li>Confirm that the XML file exists and is readable.</li>
                    <li>
                        Check the path in <span class="mono">DESS_INSTITUTION_CONFIG</span>.
                        The default is <span class="mono">config/institution.xml</span>.
                    </li>
                    <li>Reload this page after correcting the file.</li>
                </ol>
                <div class="form-actions">
                    <a class="btn" href="<%= request.getContextPath() %>/">Reload the page</a>
                </div>
            </section>

            <footer class="dess-footer">
                <p>DESS · Department Exam and Schedule System</p>
                <p>Local development only.</p>
            </footer>
        </main>
    </div>
</div>
</body>
</html>
<%
        return;
    }

    Path configFile = InstitutionSettings.fromEnvironment().configFile().toAbsolutePath().normalize();

    if ("POST".equalsIgnoreCase(request.getMethod()))
    {
        String action = trim(request.getParameter("action"));
        String kind = "ok";
        String message = null;
        String redirectTarget = "";

        if ("initialize".equals(action))
        {
            redirectTarget = "#institution";
        }
        else if ("updateInfo".equals(action))
        {
            redirectTarget = "#settings";
        }
        else if ("reload".equals(action))
        {
            redirectTarget = "";
        }
        else if ("loadExample".equals(action))
        {
            redirectTarget = "#graph";
        }
        else if ("createUnit".equals(action))
        {
            String parent = trim(request.getParameter("parentId"));
            redirectTarget = parent == null || parent.isEmpty()
                ? "?create=1#graph"
                : "?parent=" + urlEncode(parent) + "#graph";
        }
        else if ("updateUnit".equals(action) || "activateUnit".equals(action)
                 || "deactivateUnit".equals(action) || "deleteUnit".equals(action)
                 || "moveUnit".equals(action))
        {
            String id = trim(request.getParameter("id"));
            redirectTarget = id == null || id.isEmpty()
                ? "#editor"
                : "?node=" + urlEncode(id) + "#editor";
        }
        else if ("createType".equals(action) || "activateType".equals(action)
                 || "deactivateType".equals(action) || "deleteType".equals(action))
        {
            redirectTarget = "#types";
        }
        else if ("addPair".equals(action) || "removePair".equals(action))
        {
            redirectTarget = "#relations";
        }

        try
        {
            if ("initialize".equals(action))
            {
                InstitutionInfo created = institutionService.initialize(
                    trim(request.getParameter("code")),
                    trim(request.getParameter("name")),
                    trim(request.getParameter("description"))
                );
                message = "Tree '" + created.name() + "' created.";
            }
            else if ("loadExample".equals(action))
            {
                if (institutionService.getStatus() != ConfigurationStatus.NOT_INITIALIZED)
                {
                    throw new ValidationException(
                        "Loading an example requires a non-initialized configuration.");
                }

                String example = trim(request.getParameter("example"));

                if (example == null || !EXAMPLES.contains(example))
                {
                    throw new ValidationException("Unknown example.");
                }

                try (InputStream stream = Thread.currentThread()
                                                    .getContextClassLoader()
                                                    .getResourceAsStream("examples/" + example + ".xml"))
                {
                    if (stream == null)
                    {
                        throw new ConfigurationLoadException("Example not found: " + example);
                    }

                    Files.createDirectories(configFile.getParent());
                    Files.copy(stream, configFile, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException error)
                {
                    throw new ConfigurationLoadException(
                        "Unable to copy the example: " + error.getMessage(), error);
                }

                institutionService.reload();
                message = "Example '" + example + "' loaded into " + configFile + ".";
            }
            else if ("reload".equals(action))
            {
                institutionService.reload();
                message = institutionService.getStatus() == ConfigurationStatus.READY
                    ? "Configuration reloaded from the XML file."
                    : "No configuration file: the module is not initialized.";
            }
            else if ("updateInfo".equals(action))
            {
                InstitutionInfo updated = institutionService.updateInfo(
                    trim(request.getParameter("code")),
                    trim(request.getParameter("name")),
                    trim(request.getParameter("description"))
                );
                message = "Institution '" + updated.name() + "' saved.";
            }
            else if ("createType".equals(action))
            {
                UnitTypeDefinition created = unitTypeService.create(
                    UnitTypeDefinition.of(trim(request.getParameter("code")), trim(request.getParameter("label"))));
                message = "Type '" + created.code() + "' created.";
            }
            else if ("activateType".equals(action))
            {
                UnitTypeDefinition type = unitTypeService.activate(trim(request.getParameter("code")));
                message = "Type '" + type.code() + "' activated.";
            }
            else if ("deactivateType".equals(action))
            {
                UnitTypeDefinition type = unitTypeService.deactivate(trim(request.getParameter("code")));
                message = "Type '" + type.code() + "' deactivated.";
            }
            else if ("deleteType".equals(action))
            {
                String code = trim(request.getParameter("code"));
                unitTypeService.delete(code);
                message = "Type '" + code + "' deleted.";
            }
            else if ("addPair".equals(action))
            {
                String parentType = trim(request.getParameter("parentType"));
                String childType = trim(request.getParameter("childType"));
                unitTypeService.addAllowedPair(parentType, childType);
                message = "Relation " + parentType + " → " + childType + " allowed.";
            }
            else if ("removePair".equals(action))
            {
                String parentType = trim(request.getParameter("parentType"));
                String childType = trim(request.getParameter("childType"));
                unitTypeService.removeAllowedPair(parentType, childType);
                message = "Relation " + parentType + " → " + childType + " removed.";
            }
            else if ("createUnit".equals(action))
            {
                String typeCode = trim(request.getParameter("typeCode"));
                String parentId = trim(request.getParameter("parentId"));

                if (parentId != null && parentId.isEmpty())
                {
                    parentId = null;
                }

                UnitTypeDefinition type = unitTypeService.getByCode(typeCode)
                    .orElseThrow(() -> new ValidationException("Unknown unit type: " + typeCode));
                Map<String, String> values = new LinkedHashMap<>();

                for (AttributeDefinition definition : type.attributes())
                {
                    String raw = request.getParameter("attr_" + definition.key());

                    if (raw != null && !raw.isBlank())
                    {
                        values.put(definition.key(), raw.trim());
                    }
                }

                OrganizationalUnit created = hierarchyService.createUnit(new CreateUnitCommand(
                    null,
                    trim(request.getParameter("code")),
                    trim(request.getParameter("name")),
                    trim(request.getParameter("description")),
                    typeCode,
                    parentId,
                    request.getParameter("active") != null,
                    values
                ));

                message = "Unit '" + created.name() + "' created.";
                redirectTarget = "?node=" + urlEncode(created.id()) + "#editor";
            }
            else if ("updateUnit".equals(action))
            {
                String id = trim(request.getParameter("id"));
                OrganizationalUnit unit = hierarchyService.getById(id)
                    .orElseThrow(() -> new ValidationException("Unit not found: " + id));
                UnitTypeDefinition type = unitTypeService.getByCode(unit.typeCode())
                    .orElseThrow(() -> new ValidationException(
                        "Unit type '" + unit.typeCode() + "' does not exist"));
                Map<String, String> values = new LinkedHashMap<>();

                for (AttributeDefinition definition : type.attributes())
                {
                    String raw = request.getParameter("attr_" + definition.key());

                    if (raw != null && !raw.isBlank())
                    {
                        values.put(definition.key(), raw.trim());
                    }
                }

                OrganizationalUnit updated = hierarchyService.updateUnit(id, new UpdateUnitCommand(
                    trim(request.getParameter("code")),
                    trim(request.getParameter("name")),
                    trim(request.getParameter("description")),
                    values
                ));

                message = "Unit '" + updated.name() + "' updated.";
                redirectTarget = "?node=" + urlEncode(updated.id()) + "#editor";
            }
            else if ("moveUnit".equals(action))
            {
                String id = trim(request.getParameter("id"));
                String parentId = trim(request.getParameter("parentId"));

                if (parentId != null && parentId.isEmpty())
                {
                    parentId = null;
                }

                OrganizationalUnit moved = hierarchyService.moveUnit(id, parentId);
                String parentName = moved.parentId() == null
                    ? null
                    : hierarchyService.getById(moved.parentId()).map(OrganizationalUnit::name)
                                     .orElse(moved.parentId());

                message = parentName == null
                    ? "Unit '" + moved.name() + "' is now a root."
                    : "Unit '" + moved.name() + "' moved under '" + parentName + "'.";
                redirectTarget = "?node=" + urlEncode(moved.id()) + "#editor";
            }
            else if ("activateUnit".equals(action))
            {
                OrganizationalUnit unit = hierarchyService.activateUnit(trim(request.getParameter("id")));
                message = "Unit '" + unit.name() + "' activated.";
                redirectTarget = "?node=" + urlEncode(unit.id()) + "#editor";
            }
            else if ("deactivateUnit".equals(action))
            {
                OrganizationalUnit unit = hierarchyService.deactivateUnit(
                    trim(request.getParameter("id")),
                    request.getParameter("cascade") != null
                );
                message = "Unit '" + unit.name() + "' deactivated with its descendants.";
                redirectTarget = "?node=" + urlEncode(unit.id()) + "#editor";
            }
            else if ("deleteUnit".equals(action))
            {
                hierarchyService.deleteUnit(trim(request.getParameter("id")));
                message = "Unit deleted.";
                redirectTarget = "#graph";
            }
            else
            {
                throw new ValidationException("Unknown action: " + action);
            }

            if (message == null)
            {
                message = "Operation completed.";
            }
        }
        catch (InstitutionException error)
        {
            kind = "error";
            message = error.getMessage();
        }
        catch (RuntimeException error)
        {
            kind = "error";
            message = rootMessage(error);
        }

        session.setAttribute("dess.flash.kind", kind);
        session.setAttribute("dess.flash.text", message);
        session.setAttribute("dess.flash.section", sectionOf(redirectTarget));

        // Keep what the user typed in the two short forms when the server refuses it.
        if ("error".equals(kind) && ("createType".equals(action) || "addPair".equals(action)))
        {
            Map<String, String> draft = new HashMap<>();

            for (String field : List.of("code", "label", "parentType", "childType"))
            {
                draft.put(action + "." + field, trim(request.getParameter(field)));
            }

            session.setAttribute("dess.institution.draft", draft);
        }

        response.sendRedirect(request.getContextPath() + "/" + redirectTarget);
        return;
    }

    String flashKind = (String) session.getAttribute("dess.flash.kind");
    String flashText = (String) session.getAttribute("dess.flash.text");
    session.removeAttribute("dess.flash.kind");
    session.removeAttribute("dess.flash.text");

    ConfigurationStatus status = institutionService.getStatus();
    String flashSection = (String) session.getAttribute("dess.flash.section");
    session.removeAttribute("dess.flash.section");

    // Only the ready page has sections; every other message goes to the top.
    if (status != ConfigurationStatus.READY || flashSection == null
        || !List.of("types", "relations", "graph", "editor", "settings").contains(flashSection))
    {
        flashSection = "top";
    }

    @SuppressWarnings("unchecked")
    Map<String, String> draft = (Map<String, String>) session.getAttribute("dess.institution.draft");
    session.removeAttribute("dess.institution.draft");

    if (draft == null)
    {
        draft = Map.of();
    }

    String requestedParent = trim(request.getParameter("parent"));
    boolean openCreateDialog = canWrite
        && status == ConfigurationStatus.READY
        && !"error".equals(flashKind)
        && (request.getParameter("create") != null || requestedParent != null);
    String openCreateAttribute = openCreateDialog
        ? " data-open-create=\"" + esc(requestedParent == null ? "" : requestedParent) + "\""
        : "";
    InstitutionInfo currentInfo = status == ConfigurationStatus.READY
        ? institutionService.getInfo()
        : null;
    String selectedId = trim(request.getParameter("node"));
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Institution structure · DESS</title>
    <link rel="icon" href="<%= request.getContextPath() %>/favicon.ico" sizes="any">
    <link rel="icon" type="image/png" href="<%= request.getContextPath() %>/assets/img/app-icon-192.png">
    <link rel="apple-touch-icon" href="<%= request.getContextPath() %>/assets/img/app-icon-192.png">
    <script>document.documentElement.classList.add('js');</script>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/tokens.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/institution.css">
    <script src="<%= request.getContextPath() %>/assets/js/shell.js" defer></script>
</head>
<body<%= openCreateAttribute %>>
<a class="dess-skip-link" href="#main-content">Skip to main content</a>
<div class="dess-shell">
    <%@ include file="/WEB-INF/views/components/sidebar.jsp" %>
    <div class="dess-main">
        <header class="dess-topbar">
            <div class="dess-topbar-context">
                <button type="button" class="dess-mobile-menu" data-nav-toggle
                        aria-expanded="false" aria-controls="dess-sidebar">Menu</button>
                <span class="dess-topbar-title">Institution module</span>
                <span class="dess-topbar-separator" aria-hidden="true">/</span>
                <span><%= status == ConfigurationStatus.READY ? "Structure" : "Setup" %></span>
            </div>
            <div class="dess-topbar-right">
                <%@ include file="/WEB-INF/views/components/user-menu.jspf" %>
            </div>
        </header>
        <main id="main-content" class="dess-content" tabindex="-1">
            <div class="dess-page-header">
                <div class="dess-page-heading">
                    <nav class="dess-breadcrumb" aria-label="Breadcrumb">
                        <ol>
                            <li>Institution</li>
                            <li aria-current="page">Structure</li>
                        </ol>
                    </nav>
                    <h1 class="dess-page-title">Institution structure</h1>
                    <p class="dess-page-description">
                        <% if (status == ConfigurationStatus.READY) { %>
                        <% if (canWrite) { %>
                        Define the unit types and the relations allowed between them, then build the
                        hierarchy.
                        <% } else { %>
                        Browse the organizational hierarchy. Your role can view the structure but
                        cannot change it.
                        <% } %>
                        <% } else { %>
                        Create a named tree or load an example to start the institution hierarchy.
                        <% } %>
                    </p>
                </div>
                <div class="header-actions">
                    <% if (status == ConfigurationStatus.READY) {
                        String nodeQuery = selectedId == null || selectedId.isEmpty()
                            ? "" : "node=" + urlEncode(selectedId) + "&";
                    %>
                    <a class="btn btn-sm btn-ghost" href="?<%= nodeQuery %>check=1">Validate</a>
                    <a class="btn btn-sm btn-ghost" href="<%= request.getContextPath() %>/export.jsp">Download XML</a>
                    <% } %>
                </div>
            </div>

            <%= flash("top", flashSection, flashKind, flashText) %>

            <% if (!canWrite) { %>
            <div class="flash info" role="status">
                <strong>Read-only view.</strong>
                Your role can view the institution structure but cannot change it.
                Ask an administrator to make changes.
            </div>
            <% } %>

            <% if (status != ConfigurationStatus.READY) { %>
            <% if (!canWrite) { %>
            <section class="card" id="institution" aria-labelledby="initialize-title">
                <h2 id="initialize-title">Institution not initialized</h2>
                <p class="muted">
                    No configuration is loaded. An administrator must create the tree or load an
                    example before the hierarchy can be viewed.
                </p>
            </section>
            <% } else { %>
            <section class="card" id="institution" aria-labelledby="initialize-title">
                <h2 id="initialize-title">Create a named tree</h2>
                <p class="muted">
                    No configuration is loaded. The expected file is
                    <span class="mono"><%= esc(configFile.toString()) %></span>.
                </p>

                <form method="post" class="grid">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                    <input type="hidden" name="action" value="initialize">
                    <label>Tree code
                        <input name="code" required maxlength="64"
                               pattern="[A-Za-z0-9][A-Za-z0-9_.-]*" placeholder="TEST_UNIV">
                    </label>
                    <label>Tree name
                        <input name="name" required maxlength="120" placeholder="Test University">
                    </label>
                    <label class="span-2">Description
                        <input name="description" maxlength="255" placeholder="Optional">
                    </label>
                    <div class="form-actions span-2">
                        <button class="btn" type="submit">Create the tree</button>
                    </div>
                </form>

                <hr>

                <h3>Load an example</h3>
                <p class="muted">
                    The example is copied to the configuration file, then reloaded. No unit type is
                    hard-coded in the application: everything comes from the XML.
                </p>
                <form method="post" class="row">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                    <input type="hidden" name="action" value="loadExample">
                    <label>Example
                        <select name="example">
                            <option value="university">University (5 levels)</option>
                            <option value="high-school">High school (5 levels)</option>
                            <option value="custom-school">Conservatory (custom types)</option>
                        </select>
                    </label>
                    <button class="btn" type="submit">Load the example</button>
                </form>

                <hr>

                <form method="post" class="inline">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                    <input type="hidden" name="action" value="reload">
                    <button class="btn btn-sm btn-ghost" type="submit">Reload XML</button>
                </form>
            </section>
            <% } %>
            <% } else {
        InstitutionConfig institutionConfig = institutionService.getConfig();
        List<UnitTypeDefinition> allTypes = unitTypeService.getAll();
        List<AllowedParentChildPair> pairs = unitTypeService.getAllowedPairs();
        List<OrganizationalUnit> roots = hierarchyService.getRoots();
        List<UnitTypeDefinition> activeTypes = new ArrayList<>();
        ValidationReport report = null;
        OrganizationalUnit selectedUnit = selectedId == null || selectedId.isEmpty()
            ? null
            : hierarchyService.getById(selectedId).orElse(null);
        boolean selectedMissing = selectedId != null && !selectedId.isEmpty() && selectedUnit == null;

        for (UnitTypeDefinition type : allTypes)
        {
            if (type.active())
            {
                activeTypes.add(type);
            }
        }

        if (request.getParameter("check") != null)
        {
            report = institutionService.check();
        }

        Set<String> graphVisited = new HashSet<>();
        List<GraphBox> graphBoxes = new ArrayList<>();
        List<GraphEdge> graphEdges = new ArrayList<>();
        int graphCursor = GRAPH_MARGIN;

        for (OrganizationalUnit rootUnit : roots)
        {
            GraphBox box = graphTree(institutionConfig, rootUnit, graphVisited);
            graphMeasure(box);
            graphPosition(box, graphCursor, 0);
            graphCollect(box, graphBoxes, graphEdges);
            graphCursor += box.width + GRAPH_H_GAP * 2;
        }

        for (OrganizationalUnit unit : institutionConfig.units())
        {
            if (!graphVisited.contains(unit.id()))
            {
                GraphBox box = graphTree(institutionConfig, unit, graphVisited);
                graphMeasure(box);
                graphPosition(box, graphCursor, 0);
                graphCollect(box, graphBoxes, graphEdges);
                graphCursor += box.width + GRAPH_H_GAP * 2;
            }
        }

        int graphWidth = graphBoxes.isEmpty()
            ? 0
            : graphCursor - GRAPH_H_GAP * 2 + GRAPH_MARGIN;
        int graphHeight = GRAPH_NODE_H + GRAPH_MARGIN * 2;

        for (GraphBox box : graphBoxes)
        {
            graphHeight = Math.max(graphHeight, box.y + GRAPH_NODE_H + GRAPH_MARGIN);
        }

        // The canvas may shrink to fit, but not below 70% of the graph's
        // natural width; narrower panes scroll instead of shrinking labels.
        int graphMinWidth = (int) Math.round(graphWidth * 0.7);

        List<OrganizationalUnit> moveCandidates = new ArrayList<>();
        List<String> allowedParentTypes = new ArrayList<>();

        if (selectedUnit != null)
        {
            Set<String> excludedIds = new HashSet<>();
            excludedIds.add(selectedUnit.id());

            for (OrganizationalUnit descendant : institutionConfig.descendantsOf(selectedUnit.id()))
            {
                excludedIds.add(descendant.id());
            }

            for (OrganizationalUnit candidate : institutionConfig.units())
            {
                if (!excludedIds.contains(candidate.id()))
                {
                    moveCandidates.add(candidate);
                }
            }

            allowedParentTypes = unitTypeService.getAllowedParentTypes(selectedUnit.typeCode());
        }

        Map<String, String> typeLabels = new HashMap<>();

        for (UnitTypeDefinition type : allTypes)
        {
            typeLabels.put(type.code(), type.label());
        }

        Map<String, List<String>> allowedParentsByType = new LinkedHashMap<>();

        for (UnitTypeDefinition type : activeTypes)
        {
            allowedParentsByType.put(type.code(), unitTypeService.getAllowedParentTypes(type.code()));
        }
    %>
    <% if (report != null) { %>
        <% if (report.valid()) { %>
        <div class="flash ok" role="status">The configuration is valid.</div>
        <% } else { %>
        <div class="flash error" role="alert">
            <strong>The configuration is invalid:</strong>
            <ul>
                <% for (String error : report.errors()) { %>
                <li><%= esc(error) %></li>
                <% } %>
            </ul>
        </div>
        <% } %>
    <% } %>

    <nav class="dess-context-bar" aria-label="On this page">
        <span class="dess-context-item">
            <strong><%= esc(currentInfo.name()) %></strong>
            <span class="mono"><%= esc(currentInfo.code()) %></span>
        </span>
        <a class="dess-context-item" href="#types">Unit types (<%= allTypes.size() %>)</a>
        <a class="dess-context-item" href="#relations">Allowed relations (<%= pairs.size() %>)</a>
        <a class="dess-context-item" href="#graph">Hierarchy (<%= graphBoxes.size() %>)</a>
        <a class="dess-context-item" href="#settings">Settings</a>
    </nav>

    <div class="structure-rules">
    <section class="card" id="types" aria-labelledby="types-title">
        <h2 id="types-title">Unit types</h2>
        <p class="muted flush-top">The kinds of unit this institution uses.</p>
        <%= flash("types", flashSection, flashKind, flashText) %>
        <% if (allTypes.isEmpty()) { %>
        <div class="empty">
            <p><strong>No unit types yet.</strong></p>
            <% if (canWrite) { %>
            <p>Add the first type below, for example a faculty or a class.</p>
            <% } else { %>
            <p>An administrator must add the first type.</p>
            <% } %>
        </div>
        <% } else { %>
        <div class="table-wrap" role="region" aria-labelledby="types-title" tabindex="0">
        <table>
            <thead>
            <tr>
                <th scope="col">Type</th>
                <th scope="col">Attributes</th>
                <th scope="col">Status</th>
                <% if (canWrite) { %>
                <th scope="col">Actions</th>
                <% } %>
            </tr>
            </thead>
            <tbody>
            <% for (UnitTypeDefinition type : allTypes) { %>
            <tr>
                <td>
                    <strong><%= esc(type.label()) %></strong>
                    <div class="mono muted"><%= esc(type.code()) %></div>
                    <% if (type.description() != null) { %>
                    <div class="muted"><%= esc(type.description()) %></div>
                    <% } %>
                </td>
                <td>
                    <% if (type.attributes().isEmpty()) { %>
                    <span class="muted">None</span>
                    <% } %>
                    <% for (AttributeDefinition attribute : type.attributes()) { %>
                    <div>
                        <%= esc(attribute.label()) %>
                        <span class="muted"><%= esc(attribute.type().name().toLowerCase(java.util.Locale.ROOT)) %><%=
                            attribute.required() ? ", required" : "" %></span>
                    </div>
                    <% } %>
                </td>
                <td>
                    <span class="badge <%= type.active() ? "ok" : "off" %>">
                        <%= type.active() ? "Active" : "Inactive" %>
                    </span>
                </td>
                <% if (canWrite) { %>
                <td class="actions">
                    <div class="row-actions">
                    <%= type.active()
                        ? postButton(csrfToken, "deactivateType", "code", type.code(), "Deactivate", "btn-ghost", null)
                        : postButton(csrfToken, "activateType", "code", type.code(), "Activate", "btn-ghost", null) %>
                    <%= postButton(csrfToken, "deleteType", "code", type.code(), "Delete", "btn-danger",
                        "Delete unit type '" + type.code() + "'? Deletion is refused while units or relations still use it.") %>
                    </div>
                </td>
                <% } %>
            </tr>
            <% } %>
            </tbody>
        </table>
        </div>
        <% } %>

        <% if (canWrite) { %>
        <h3>Add a unit type</h3>
        <form method="post" class="row" id="create-type-form">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="createType">
            <label>Label
                <input name="label" required maxlength="120" placeholder="Department"
                       value="<%= esc(draft.get("createType.label")) %>">
            </label>
            <label>Code
                <input name="code" required maxlength="64" pattern="[A-Za-z0-9][A-Za-z0-9_.-]*"
                       title="Letters, digits, dot, dash or underscore; start with a letter or digit"
                       placeholder="DEPARTMENT" value="<%= esc(draft.get("createType.code")) %>">
            </label>
            <button class="btn" type="submit">Add type</button>
        </form>
        <p class="hint">Both fields are required. The code cannot be changed later.</p>
        <% } %>
    </section>

    <section class="card" id="relations" aria-labelledby="relations-title">
        <h2 id="relations-title">Allowed relations</h2>
        <p class="muted flush-top">Which type can be placed directly under which other type.</p>
        <%= flash("relations", flashSection, flashKind, flashText) %>
        <% if (pairs.isEmpty()) { %>
        <div class="empty">
            <p><strong>No relations yet.</strong></p>
            <p>Root units can use any type. A child unit needs a relation.</p>
        </div>
        <% } else { %>
        <div class="table-wrap" role="region" aria-labelledby="relations-title" tabindex="0">
        <table>
            <thead>
            <tr>
                <th scope="col">Parent type</th>
                <th scope="col">Child type</th>
                <% if (canWrite) { %>
                <th scope="col">Actions</th>
                <% } %>
            </tr>
            </thead>
            <tbody>
            <% for (AllowedParentChildPair pair : pairs) { %>
            <tr>
                <td><%= esc(typeLabels.getOrDefault(pair.parent(), pair.parent())) %></td>
                <td><%= esc(typeLabels.getOrDefault(pair.child(), pair.child())) %></td>
                <% if (canWrite) { %>
                <td class="actions">
                    <form method="post" class="inline"
                          data-confirm="Remove the allowed relation <%= esc(pair.parent()) %> → <%= esc(pair.child()) %>? Removal is refused while units rely on it.">
                        <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                        <input type="hidden" name="action" value="removePair">
                        <input type="hidden" name="parentType" value="<%= esc(pair.parent()) %>">
                        <input type="hidden" name="childType" value="<%= esc(pair.child()) %>">
                        <button class="btn btn-sm btn-danger" type="submit"
                                aria-label="<%= esc("Remove " + pair.parent() + " to " + pair.child()) %>">Remove</button>
                    </form>
                </td>
                <% } %>
            </tr>
            <% } %>
            </tbody>
        </table>
        </div>
        <% } %>

        <% if (canWrite) { %>
        <h3>Allow a relation</h3>
        <% if (allTypes.isEmpty()) { %>
        <p class="muted">Add a unit type first.</p>
        <% } else { %>
        <form method="post" class="row">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="addPair">
            <label>Parent type
                <select name="parentType" required>
                    <option value="">Choose…</option>
                    <% for (UnitTypeDefinition type : allTypes) { %>
                    <option value="<%= esc(type.code()) %>" <%= type.code().equals(draft.get("addPair.parentType"))
                        ? "selected" : "" %>><%= esc(type.label() + " (" + type.code() + ")") %></option>
                    <% } %>
                </select>
            </label>
            <label>Child type
                <select name="childType" required>
                    <option value="">Choose…</option>
                    <% for (UnitTypeDefinition type : allTypes) { %>
                    <option value="<%= esc(type.code()) %>" <%= type.code().equals(draft.get("addPair.childType"))
                        ? "selected" : "" %>><%= esc(type.label() + " (" + type.code() + ")") %></option>
                    <% } %>
                </select>
            </label>
            <button class="btn" type="submit">Allow relation</button>
        </form>
        <% } } %>
    </section>
    </div>

    <div class="workspace">
        <section class="card canvas-card" id="graph" aria-labelledby="graph-title">
            <div class="graph-toolbar">
                <div class="toolbar-title">
                    <h2 id="graph-title">Hierarchy</h2>
                    <span class="muted">
                        <%= graphBoxes.size() %> <%= graphBoxes.size() == 1 ? "unit" : "units" %>
                    </span>
                </div>
                <div class="form-actions">
                    <% if (canWrite) { %>
                    <button type="button" class="btn btn-sm new-root">+ New root unit</button>
                    <% } %>
                </div>
            </div>
            <% if (!graphBoxes.isEmpty()) { %>
            <p class="legend" id="graph-legend">
                <% if (canWrite) { %>
                Select a unit to edit or move it. Use <strong>+</strong> to add a child under it.
                <% } else { %>
                Select a unit to view its details.
                <% } %>
                <span class="graph-hint">
                    On a small screen, scroll the canvas sideways to reach every unit.
                </span>
            </p>
            <% } %>

            <%= flash("graph", flashSection, flashKind, flashText) %>
            <% if (graphBoxes.isEmpty()) { %>
            <div class="empty">
                <p><strong>No units yet.</strong></p>
                <% if (canWrite) { %>
                <p>Create the first root unit to start the institution hierarchy.</p>
                <button type="button" class="btn new-root">Create the first root unit</button>
                <% } else { %>
                <p>An administrator must create the first root unit.</p>
                <% } %>
            </div>
            <% } else { %>
            <div class="graph-scroll" tabindex="0" role="region"
                 aria-label="Institution hierarchy canvas" aria-describedby="graph-legend">
                <svg id="graphSvg" class="graph" width="<%= graphWidth %>" height="<%= graphHeight %>"
                     viewBox="0 0 <%= graphWidth %> <%= graphHeight %>" role="group"
                     style="--graph-min: <%= graphMinWidth %>px"
                     aria-label="Institution unit graph">
                    <g class="edges">
                        <% for (GraphEdge edge : graphEdges) {
                            boolean connected = selectedUnit != null
                                && (selectedUnit.id().equals(edge.parentId)
                                    || selectedUnit.id().equals(edge.childId));
                        %>
                        <path class="edge<%= connected ? " connected" : "" %>"
                              data-parent="<%= esc(edge.parentId) %>"
                              data-child="<%= esc(edge.childId) %>"
                              d="<%= edge.path %>"></path>
                        <% } %>
                    </g>
                    <g class="nodes">
                        <% for (GraphBox box : graphBoxes) {
                            OrganizationalUnit unit = box.unit;
                            boolean selected = selectedUnit != null && selectedUnit.id().equals(unit.id());
                            int childCount = box.children.size();
                            String meta = childCount == 0
                                ? "leaf"
                                : childCount + (childCount == 1 ? " child" : " children");

                            if (!unit.active())
                            {
                                meta += " · inactive";
                            }
                        %>
                        <a class="gnode<%= selected ? " selected" : "" %><%= unit.active() ? "" : " inactive" %>"
                           data-id="<%= esc(unit.id()) %>"
                           data-name="<%= esc(unit.name()) %>"
                           data-code="<%= esc(unit.code()) %>"
                           data-type="<%= esc(unit.typeCode()) %>"
                           data-active="<%= unit.active() %>"
                           transform="translate(<%= box.x %>,<%= box.y %>)"
                           href="?node=<%= urlEncode(unit.id()) %>#editor"
                           <%= selected ? "aria-current=\"true\"" : "" %>
                           aria-label="<%= esc(unit.name() + ", " + unit.code() + ", type "
                               + unit.typeCode() + (unit.active() ? ", active" : ", inactive")) %>">
                            <title><%= esc(unit.name() + " (" + unit.code() + ") · " + unit.typeCode()
                                             + (unit.active() ? " · active" : " · inactive")) %></title>
                            <rect width="<%= GRAPH_NODE_W %>" height="<%= GRAPH_NODE_H %>" rx="10"></rect>
                            <circle class="gdot <%= unit.active() ? "ok" : "off" %>"
                                    cx="<%= GRAPH_NODE_W - 16 %>" cy="16" r="4.5"></circle>
                            <text class="gname" x="14" y="27"><%= esc(clipText(unit.name(), GRAPH_NODE_TITLE_MAX)) %></text>
                            <text class="gline" x="14" y="47"><%= esc(clipText(
                                unit.code() + " · " + unit.typeCode(), 35)) %></text>
                            <text class="gmeta" x="14" y="64"><%= esc(meta) %></text>
                        </a>
                        <% } %>
                    </g>
                    <% if (canWrite) { %>
                    <g class="adds">
                        <% for (GraphBox box : graphBoxes) { %>
                        <g class="add-child" data-for="<%= esc(box.unit.id()) %>"
                           transform="translate(<%= box.x + GRAPH_NODE_W + 16 %>,<%= box.y + GRAPH_NODE_H / 2 %>)"
                           tabindex="0" role="button"
                           aria-label="<%= esc("Add a child under " + box.unit.name()) %>">
                            <title>Add a child under <%= esc(box.unit.name()) %></title>
                            <ellipse class="add-child-hit" rx="12" ry="20"></ellipse>
                            <circle class="add-child-ring" r="11"></circle>
                            <path d="M-5,0 H5 M0,-5 V5"></path>
                        </g>
                        <% } %>
                    </g>
                    <% } %>
                </svg>
            </div>
            <% } %>

        </section>

    <aside class="card inspector" id="editor" aria-labelledby="editor-title">
        <%= flash("editor", flashSection, flashKind, flashText) %>
        <% if (selectedMissing) { %>
        <h2 id="editor-title">Unit not found</h2>
        <div class="flash error" role="alert">This node no longer exists.</div>
        <% } else if (selectedUnit == null) { %>
        <h2 id="editor-title">Unit details</h2>
        <% if (canWrite) { %>
        <p class="muted flush-top">
            Select a unit in the hierarchy to edit its fields, parent and status.
        </p>
        <% } else { %>
        <p class="muted flush-top">
            Select a unit in the hierarchy to view its details, parent and status.
        </p>
        <% } %>
        <% } else {
            UnitTypeDefinition selectedType = unitTypeService.getByCode(selectedUnit.typeCode()).orElse(null);
            List<OrganizationalUnit> path = institutionConfig.pathOf(selectedUnit);
            StringBuilder pathText = new StringBuilder();

            for (int index = 0; index < path.size(); index++)
            {
                if (index > 0)
                {
                    pathText.append(" → ");
                }

                pathText.append(path.get(index).name());
            }
        %>
        <div class="card-head">
            <h2 id="editor-title"><%= esc(selectedUnit.name()) %></h2>
            <span class="badge <%= selectedUnit.active() ? "ok" : "off" %>">
                <%= selectedUnit.active() ? "Active" : "Inactive" %>
            </span>
        </div>
        <p class="meta">
            <span class="mono"><%= esc(selectedUnit.code()) %></span> ·
            <%= esc(selectedUnit.typeCode()) %>
            <% if (selectedType != null && !selectedType.label().equals(selectedType.code())) { %>
            · <%= esc(selectedType.label()) %>
            <% } %>
        </p>
        <p class="path-trail">
            <a href="#graph">Hierarchy</a> / <%= esc(pathText.toString()) %>
        </p>

        <% if (canWrite) { %>
        <form method="post" class="stack">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="updateUnit">
            <input type="hidden" name="id" value="<%= esc(selectedUnit.id()) %>">
            <label>Name
                <input name="name" required maxlength="120"
                       value="<%= esc(selectedUnit.name()) %>">
            </label>
            <label>Code
                <input name="code" required maxlength="64"
                       pattern="[A-Za-z0-9][A-Za-z0-9_.-]*"
                       value="<%= esc(selectedUnit.code()) %>">
            </label>
            <label>Description
                <input name="description" maxlength="255"
                       value="<%= esc(selectedUnit.description()) %>" placeholder="Optional">
            </label>

            <% if (selectedType != null && !selectedType.attributes().isEmpty()) { %>
            <fieldset>
                <legend>Attributes</legend>
                <div class="stack">
                    <% for (AttributeDefinition attribute : selectedType.attributes()) {
                        AttributeValue current = selectedUnit.values().get(attribute.key());
                        String raw = current == null ? "" : current.value();
                        boolean required = attribute.required() && !attribute.hasDefault();
                    %>
                    <label>
                        <%= esc(attribute.label()) %><%= attribute.required() ? " *" : "" %>
                        <% if (attribute.type() == AttributeType.BOOLEAN) { %>
                        <select name="attr_<%= esc(attribute.key()) %>"
                                aria-label="<%= esc(attribute.label() + (attribute.required() ? " (required)" : "")) %>"
                                <%= required ? "required" : "" %>>
                            <option value="">(default value)</option>
                            <option value="true" <%= "true".equals(raw) ? "selected" : "" %>>true</option>
                            <option value="false" <%= "false".equals(raw) ? "selected" : "" %>>false</option>
                        </select>
                        <% } else { %>
                        <input type="<%= inputType(attribute.type()) %>"
                               name="attr_<%= esc(attribute.key()) %>"
                               aria-label="<%= esc(attribute.label() + (attribute.required() ? " (required)" : "")) %>"
                               value="<%= esc(raw) %>"
                               placeholder="<%= esc(attribute.hasDefault()
                                   ? "Default: " + attribute.defaultValue() : attribute.type().name()) %>"
                               <%= required ? "required" : "" %><%= inputStep(attribute.type()) %>>
                        <% } %>
                        <span class="hint">
                            <%= esc(attribute.type().name()) %><%= attribute.hasDefault()
                                ? " · default: " + esc(attribute.defaultValue()) : "" %>
                        </span>
                    </label>
                    <% } %>
                </div>
            </fieldset>
            <% } %>

            <div>
                <button class="btn" type="submit">Save changes</button>
            </div>
        </form>
        <% } else { %>
        <p class="muted flush-top">
            <% if (selectedUnit.description() == null || selectedUnit.description().isBlank()) { %>
            No description.
            <% } else { %>
            <%= esc(selectedUnit.description()) %>
            <% } %>
        </p>
        <% if (selectedType != null && !selectedType.attributes().isEmpty()) { %>
        <fieldset>
            <legend>Attributes</legend>
            <div class="stack">
                <% for (AttributeDefinition attribute : selectedType.attributes()) {
                    AttributeValue current = selectedUnit.values().get(attribute.key());
                %>
                <p class="kv">
                    <span class="mono"><%= esc(attribute.key()) %></span> :
                    <%= esc(attribute.label()) %>
                    <% if (current != null) { %>
                    — <%= esc(current.value()) %>
                    <% } else { %>
                    <span class="muted">— not set</span>
                    <% } %>
                </p>
                <% } %>
            </div>
        </fieldset>
        <% } %>
        <% } %>

        <hr>

        <h3>Parent</h3>
        <p class="muted flush-top">
            <% if (selectedUnit.parentId() == null) { %>
            <strong>none (root)</strong>
            <% } else {
                OrganizationalUnit currentParent = institutionConfig.findUnit(
                    selectedUnit.parentId()).orElse(null);
                if (currentParent == null) { %>
            <strong>missing</strong>
            <% } else { %>
            <a href="?node=<%= esc(currentParent.id()) %>#editor"><%= esc(currentParent.name()) %></a>
            <% } } %>
        </p>

        <% if (canWrite) { %>
        <form method="post" class="stack">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="moveUnit">
            <input type="hidden" name="id" value="<%= esc(selectedUnit.id()) %>">
            <label>New parent
                <select name="parentId">
                    <option value="" <%= selectedUnit.parentId() == null ? "selected" : "" %>>
                        (none — make this unit a root)
                    </option>
                    <% for (OrganizationalUnit candidate : moveCandidates) {
                        boolean current = candidate.id().equals(selectedUnit.parentId());
                        boolean allowed = allowedParentTypes.contains(candidate.typeCode());
                        boolean inactiveBlocked = selectedUnit.active() && !candidate.active();
                        boolean disabled = !current && (!allowed || inactiveBlocked);
                        String reason = !allowed
                            ? " — relation not allowed"
                            : inactiveBlocked ? " — inactive parent" : "";
                    %>
                    <option value="<%= esc(candidate.id()) %>"
                            <%= current ? "selected" : "" %><%= disabled ? " disabled" : "" %>>
                        <%= esc(depthPrefix(institutionConfig.pathOf(candidate).size() - 1)
                                + candidate.name() + " (" + candidate.code() + " · "
                                + candidate.typeCode() + ")" + reason) %>
                    </option>
                    <% } %>
                </select>
            </label>
            <div>
                <button class="btn btn-sm btn-ghost" type="submit">Move unit</button>
            </div>
        </form>
        <hr>

        <div class="inspector-actions">
            <button type="button" class="btn btn-sm btn-ghost add-child-btn"
                    data-parent-id="<%= esc(selectedUnit.id()) %>"
                    data-parent-name="<%= esc(selectedUnit.name()) %>"
                    data-parent-code="<%= esc(selectedUnit.code()) %>"
                    data-parent-type="<%= esc(selectedUnit.typeCode()) %>"
                    data-parent-active="<%= selectedUnit.active() %>">+ Add child</button>
            <% if (selectedUnit.active()) { %>
            <form method="post" class="inline"
                  data-confirm="Deactivate '<%= esc(selectedUnit.name()) %>' and its descendants? They will also be deactivated.">
                <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                <input type="hidden" name="action" value="deactivateUnit">
                <input type="hidden" name="id" value="<%= esc(selectedUnit.id()) %>">
                <input type="hidden" name="cascade" value="true">
                <button type="submit" class="btn btn-sm btn-ghost">Deactivate</button>
            </form>
            <% } else { %>
            <%= postButton(csrfToken, "activateUnit", "id", selectedUnit.id(), "Activate", "btn-ghost", null) %>
            <% } %>
        </div>

        <div class="danger-zone">
            <p class="hint">
                Deleting a unit is permanent. Deletion is refused while the unit has children.
            </p>
            <form method="post" class="inline"
                  data-confirm="Delete '<%= esc(selectedUnit.name()) %>'? This cannot be undone. Deletion is refused while the unit has children.">
                <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                <input type="hidden" name="action" value="deleteUnit">
                <input type="hidden" name="id" value="<%= esc(selectedUnit.id()) %>">
                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
            </form>
        </div>
        <% } %>
        <% } %>
    </aside>
    </div>

    <section class="card" id="settings" aria-labelledby="settings-title">
        <h2 id="settings-title">Institution settings</h2>
        <%= flash("settings", flashSection, flashKind, flashText) %>
        <% if (canWrite) { %>
        <form method="post" class="grid">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="updateInfo">
            <label>Institution code
                <input name="code" required maxlength="64" pattern="[A-Za-z0-9][A-Za-z0-9_.-]*"
                       value="<%= esc(currentInfo.code()) %>">
            </label>
            <label>Institution name
                <input name="name" required maxlength="120" value="<%= esc(currentInfo.name()) %>">
            </label>
            <label class="span-2">Description
                <input name="description" maxlength="255" value="<%= esc(currentInfo.description()) %>"
                       placeholder="Optional">
            </label>
            <div class="form-actions span-2">
                <button class="btn" type="submit">Save settings</button>
            </div>
        </form>
        <form method="post" class="danger-zone">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="reload">
            <button class="btn btn-sm btn-ghost" type="submit">Reload from XML file</button>
            <span class="hint">
                Use this after editing the file by hand:
                <span class="mono file-path"><%= esc(configFile.toString()) %></span>
            </span>
        </form>
        <% } else { %>
        <p class="flush">
            <strong><%= esc(currentInfo.name()) %></strong>
            <span class="mono"><%= esc(currentInfo.code()) %></span>
        </p>
        <% if (currentInfo.description() != null && !currentInfo.description().isBlank()) { %>
        <p class="muted"><%= esc(currentInfo.description()) %></p>
        <% } } %>
    </section>

    <% if (canWrite) { %>
    <dialog id="createDialog" aria-labelledby="createTitle"
            <%= activeTypes.isEmpty() ? "" : "aria-describedby=\"createParentLine\"" %>>
        <% if (activeTypes.isEmpty()) { %>
        <div class="dialog-head">
            <div>
                <h2 id="createTitle">New unit</h2>
                <p class="muted">No active unit type is available.</p>
            </div>
            <button type="button" class="icon-btn" data-close-dialog
                    aria-label="Close dialog">×</button>
        </div>
        <div class="dialog-body">
            <p class="muted flush">
                Activate a type or create one under
                <a href="#types" data-close-dialog>Unit types</a>
                before adding units.
            </p>
        </div>
        <% } else { %>
        <form method="post" class="dialog-form">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="hidden" name="action" value="createUnit">
            <input type="hidden" name="parentId" id="createParentId" value="">
            <div class="dialog-head">
                <div>
                    <h2 id="createTitle">New root unit</h2>
                    <p class="muted" id="createParentLine">
                        The unit is added at the top of the hierarchy.
                    </p>
                </div>
                <button type="button" class="icon-btn" data-close-dialog
                        aria-label="Close dialog">×</button>
            </div>
            <div class="dialog-body">
                <p class="hint flush-top" aria-hidden="true">
                    Fields marked <span class="req">*</span> are required.
                </p>
                <div class="grid">
                    <label>Name <span class="req" aria-hidden="true">*</span>
                        <input id="createName" name="name" required maxlength="120"
                               autocomplete="off" placeholder="Computer Science Department">
                    </label>
                    <label><span id="createCodeLabel">Code <span class="req" aria-hidden="true">*</span></span>
                        <input id="createCode" name="code" required maxlength="64"
                               aria-labelledby="createCodeLabel"
                               pattern="[A-Za-z0-9][A-Za-z0-9_.-]*" autocomplete="off"
                               placeholder="CS_DEPT">
                        <span class="hint">Generated from the name; edit it if needed.</span>
                    </label>
                    <label>Type <span class="req" aria-hidden="true">*</span>
                        <select id="createType" name="typeCode" required>
                            <% for (UnitTypeDefinition type : activeTypes) { %>
                            <option value="<%= esc(type.code()) %>"
                                    data-parents="<%= esc(String.join(" ",
                                        allowedParentsByType.get(type.code()))) %>">
                                <%= esc(type.code() + " — " + type.label()) %>
                            </option>
                            <% } %>
                        </select>
                    </label>
                    <label>Description
                        <input name="description" maxlength="255" placeholder="Optional">
                    </label>
                    <label class="checkbox span-2">
                        <input type="checkbox" name="active" id="createActive" checked
                               aria-labelledby="createActiveLabel">
                        <span id="createActiveLabel">Activate the unit on creation</span>
                        <span class="hint" id="createActiveHint" hidden>
                            The selected parent is inactive, so the new unit must start inactive.
                        </span>
                    </label>
                </div>

                <p class="hint" id="createNoType" role="status" hidden>
                    No active type is allowed under this parent. Add one under
                    Allowed relations first.
                </p>

                <% for (UnitTypeDefinition type : activeTypes) {
                    if (type.attributes().isEmpty())
                    {
                        continue;
                    }
                %>
                <fieldset class="dialog-fieldset" data-attributes="<%= esc(type.code()) %>"
                          hidden disabled>
                    <legend>Attributes — <%= esc(type.label()) %></legend>
                    <div class="grid">
                        <% for (AttributeDefinition attribute : type.attributes()) {
                            String placeholder = attribute.hasDefault()
                                ? "Default: " + attribute.defaultValue()
                                : attribute.type().name();
                            boolean required = attribute.required() && !attribute.hasDefault();
                        %>
                        <label>
                            <%= esc(attribute.label()) %><%= attribute.required() ? " *" : "" %>
                            <% if (attribute.type() == AttributeType.BOOLEAN) { %>
                            <select name="attr_<%= esc(attribute.key()) %>"
                                    aria-label="<%= esc(attribute.label() + (attribute.required() ? " (required)" : "")) %>"
                                    <%= required ? "required" : "" %>>
                                <option value="">(default value)</option>
                                <option value="true">true</option>
                                <option value="false">false</option>
                            </select>
                            <% } else { %>
                            <input type="<%= inputType(attribute.type()) %>"
                                   name="attr_<%= esc(attribute.key()) %>"
                                   aria-label="<%= esc(attribute.label() + (attribute.required() ? " (required)" : "")) %>"
                                   placeholder="<%= esc(placeholder) %>"
                                   <%= required ? "required" : "" %><%= inputStep(attribute.type()) %>>
                            <% } %>
                            <span class="hint">
                                <%= esc(attribute.type().name()) %><%= attribute.hasDefault()
                                    ? " · default: " + esc(attribute.defaultValue()) : "" %>
                            </span>
                        </label>
                        <% } %>
                    </div>
                </fieldset>
                <% } %>
            </div>
            <div class="dialog-foot">
                <button type="button" class="btn btn-ghost" data-close-dialog>Cancel</button>
                <button class="btn" type="submit" id="createSubmit">Create unit</button>
            </div>
        </form>
        <% } %>
    </dialog>

    <% } %>

    <footer class="dess-footer">
        <% if (canWrite) { %>
        <p>Changes are validated by the institution module and then written atomically to the XML file.</p>
        <% } else { %>
        <p>Read-only view. Changes are validated by the institution module before they are written to the XML file.</p>
        <% } %>
        <p>Local development only.</p>
    </footer>

    <script>
        (function () {
            var dialog = document.getElementById('createDialog');
            var form = dialog ? dialog.querySelector('form') : null;
            var createName = document.getElementById('createName');
            var createCode = document.getElementById('createCode');
            var createType = document.getElementById('createType');
            var createParentId = document.getElementById('createParentId');
            var createTitle = document.getElementById('createTitle');
            var createParentLine = document.getElementById('createParentLine');
            var createActive = document.getElementById('createActive');
            var createActiveHint = document.getElementById('createActiveHint');
            var createNoType = document.getElementById('createNoType');
            var createSubmit = document.getElementById('createSubmit');
            var nodes = {};
            var codeEdited = false;

            document.querySelectorAll('.gnode').forEach(function (node) {
                nodes[node.getAttribute('data-id')] = node;
            });

            function syncAttributes() {
                if (!createType) {
                    return;
                }

                var selected = createType.value;

                document.querySelectorAll('#createDialog [data-attributes]').forEach(function (block) {
                    var matches = block.getAttribute('data-attributes') === selected;
                    block.hidden = !matches;
                    block.disabled = !matches;
                });
            }

            function filterTypes(parentType) {
                if (!createType) {
                    return;
                }

                var first = null;

                for (var index = 0; index < createType.options.length; index++) {
                    var option = createType.options[index];
                    var parents = (option.getAttribute('data-parents') || '').split(' ');
                    var allowed = !parentType || parents.indexOf(parentType) >= 0;

                    option.hidden = !allowed;
                    option.disabled = !allowed;

                    if (allowed && !first) {
                        first = option;
                    }
                }

                createType.value = first ? first.value : '';

                if (createNoType) {
                    createNoType.hidden = Boolean(first);
                }

                if (createSubmit) {
                    createSubmit.disabled = !first;
                }

                syncAttributes();
            }

            function cleanCreateParams() {
                try {
                    var url = new URL(window.location.href);

                    if (url.searchParams.has('create') || url.searchParams.has('parent')) {
                        url.searchParams.delete('create');
                        url.searchParams.delete('parent');

                        if (url.hash === '#create') {
                            url.hash = '';
                        }

                        window.history.replaceState(null, '', url.pathname + url.search + url.hash);
                    }
                } catch (error) {
                    // Older browsers keep the parameter; the dialog still works.
                }
            }

            function openCreate(parent) {
                if (!dialog) {
                    return;
                }

                cleanCreateParams();

                if (form) {
                    form.reset();
                }

                codeEdited = false;

                if (createParentId) {
                    createParentId.value = parent ? parent.id : '';
                }

                if (createTitle) {
                    createTitle.textContent = parent ? 'Add a child unit' : 'New root unit';
                }

                if (createParentLine) {
                    createParentLine.textContent = parent
                        ? 'Parent: ' + parent.name + ' (' + parent.code + ' · ' + parent.type + ')'
                        : 'The unit is added at the top of the hierarchy.';
                }

                if (createActive) {
                    createActive.disabled = parent ? !parent.active : false;
                    createActive.checked = parent ? parent.active : true;
                }

                if (createActiveHint) {
                    var hintApplies = parent && !parent.active;

                    createActiveHint.hidden = !hintApplies;

                    // Referenced hidden text is still announced, so detach the
                    // description unless the hint actually applies.
                    if (createActive) {
                        if (hintApplies) {
                            createActive.setAttribute('aria-describedby', 'createActiveHint');
                        } else {
                            createActive.removeAttribute('aria-describedby');
                        }
                    }
                }

                filterTypes(parent ? parent.type : null);

                if (typeof dialog.showModal === 'function') {
                    dialog.showModal();
                } else {
                    dialog.setAttribute('open', '');
                }

                if (createName) {
                    createName.focus();
                }
            }

            function openForNode(id) {
                var node = nodes[id];

                openCreate(node ? {
                    id: id,
                    name: node.getAttribute('data-name'),
                    code: node.getAttribute('data-code'),
                    type: node.getAttribute('data-type'),
                    active: node.getAttribute('data-active') === 'true'
                } : null);
            }

            if (createName) {
                createName.addEventListener('input', function () {
                    if (!createCode || codeEdited) {
                        return;
                    }

                    createCode.value = createName.value
                        .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
                        .toUpperCase()
                        .replace(/[^A-Z0-9]+/g, '_')
                        .replace(/^_+|_+$/g, '')
                        .slice(0, 64);
                });
            }

            if (createCode) {
                createCode.addEventListener('input', function () {
                    codeEdited = true;
                });
            }

            if (createType) {
                createType.addEventListener('change', syncAttributes);
            }

            document.querySelectorAll('[data-close-dialog]').forEach(function (button) {
                button.addEventListener('click', function () {
                    if (!dialog) {
                        return;
                    }

                    if (dialog.close) {
                        dialog.close();
                    } else {
                        dialog.removeAttribute('open');
                    }
                });
            });

            document.querySelectorAll('.new-root').forEach(function (button) {
                button.addEventListener('click', function () {
                    openCreate(null);
                });
            });

            document.querySelectorAll('.add-child-btn').forEach(function (button) {
                button.addEventListener('click', function () {
                    openForNode(button.getAttribute('data-parent-id'));
                });
            });

            document.querySelectorAll('.add-child').forEach(function (add) {
                var id = add.getAttribute('data-for');

                add.addEventListener('click', function () {
                    openForNode(id);
                });

                add.addEventListener('keydown', function (event) {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        openForNode(id);
                    }
                });
            });

            // On narrow screens the canvas scrolls sideways. Start centred so the root is visible.
            var scroll = document.querySelector('.graph-scroll');

            if (scroll && scroll.scrollWidth > scroll.clientWidth) {
                scroll.scrollLeft = (scroll.scrollWidth - scroll.clientWidth) / 2;
            }

            var requestedParent = document.body.getAttribute('data-open-create');

            if (requestedParent !== null) {
                openForNode(requestedParent);
            }
        })();
    </script>
    <% } %>
        </main>
    </div>
</div>
</body>
</html>
