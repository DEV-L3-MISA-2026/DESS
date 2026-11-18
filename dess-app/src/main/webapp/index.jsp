<%--
    Institution tree editor.

    The page uses the CDI producers of the institution module and never touches the XML
    infrastructure directly. Read actions and form submissions follow the post/redirect/get
    pattern; feedback is carried through the session.

    The hierarchy is rendered as a server-side SVG canvas. Nodes can be dragged to arrange the
    layout (positions are kept in the browser), and Shift-dragging a node onto another one moves it
    in the tree. Each node has a + handle that opens the create dialog with that node as parent.
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
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<%!
    private static final List<String> EXAMPLES = List.of("university", "high-school", "custom-school");

    private static final int GRAPH_NODE_W = 200;
    private static final int GRAPH_NODE_H = 68;
    private static final int GRAPH_H_GAP = 28;
    private static final int GRAPH_V_GAP = 76;
    private static final int GRAPH_MARGIN = 28;

    private static final String CSS =
        "*{box-sizing:border-box}"
        + "body{margin:0;font-family:system-ui,-apple-system,'Segoe UI',sans-serif;background:#f4f5f7;"
        + "color:#1f2328;line-height:1.45}"
        + ".wrap{max-width:1280px;margin:0 auto;padding:32px 20px 56px}"
        + "header.top{display:flex;flex-wrap:wrap;align-items:center;gap:12px;justify-content:space-between;"
        + "margin-bottom:20px}"
        + "h1{font-size:1.35rem;margin:0;letter-spacing:-.02em}"
        + ".sub{color:#656d76;margin:2px 0 0;font-size:.9rem}"
        + ".card{background:#fff;border:1px solid #d8dee4;border-radius:10px;padding:18px 20px;margin-bottom:16px}"
        + ".card h2{font-size:1.02rem;margin:0 0 12px}"
        + ".card h3{font-size:.92rem;margin:16px 0 8px}"
        + ".muted{color:#656d76}"
        + ".mono{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.85em}"
        + ".badge{display:inline-block;padding:1px 8px;border-radius:999px;font-size:.75rem;font-weight:600}"
        + ".badge.ok{background:#dafbe1;color:#116329}"
        + ".badge.off{background:#ffebe9;color:#a40e26}"
        + ".flash{border-radius:8px;padding:10px 14px;margin-bottom:16px;font-size:.92rem}"
        + ".flash.ok{background:#dafbe1;border:1px solid #aceebb;color:#116329}"
        + ".flash.error{background:#ffebe9;border:1px solid #ffcecb;color:#a40e26}"
        + ".flash ul{margin:6px 0 0;padding-left:18px}"
        + "table{width:100%;border-collapse:collapse;font-size:.9rem}"
        + "th{text-align:left;font-size:.75rem;text-transform:uppercase;letter-spacing:.04em;color:#656d76;"
        + "padding:6px 8px;border-bottom:1px solid #d8dee4}"
        + "td{padding:8px;border-bottom:1px solid #eaeef2;vertical-align:top}"
        + "tr:last-child td{border-bottom:none}"
        + ".actions{white-space:nowrap}"
        + ".inline{display:inline}"
        + "form.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 16px;align-items:start}"
        + "form.row{display:flex;flex-wrap:wrap;gap:10px;align-items:end}"
        + "label{display:block;font-size:.82rem;font-weight:600;color:#424a53}"
        + "label input,label select,.row select{display:block;width:100%;margin-top:4px;padding:7px 9px;"
        + "border:1px solid #c9d1d9;border-radius:6px;font:inherit;font-weight:400;background:#fff}"
        + "label input[type=checkbox]{display:inline-block;width:auto;margin:0 6px 0 0}"
        + "label.checkbox{font-weight:400}"
        + ".span-2{grid-column:1/-1}"
        + ".hint{display:block;margin-top:3px;color:#8c959f;font-weight:400;font-size:.75rem}"
        + "fieldset{border:1px solid #d8dee4;border-radius:8px;padding:10px 12px 14px;margin:2px 0 0}"
        + "fieldset legend{font-size:.8rem;font-weight:600;padding:0 4px}"
        + "fieldset .grid{gap:10px 14px}"
        + ".form-actions{display:flex;flex-wrap:wrap;gap:8px}"
        + ".btn{display:inline-block;border:1px solid #1f2328;background:#1f2328;color:#fff;padding:7px 12px;"
        + "border-radius:6px;font:inherit;font-size:.86rem;font-weight:600;cursor:pointer;"
        + "text-decoration:none}"
        + ".btn:hover{background:#32383f}"
        + ".btn-sm{padding:3px 9px;font-size:.78rem}"
        + ".btn-ghost{background:#fff;color:#1f2328;border-color:#c9d1d9}"
        + ".btn-ghost:hover{background:#f6f8fa}"
        + ".btn-danger{background:#cf222e;border-color:#cf222e}"
        + ".btn-danger:hover{background:#a40e26}"
        + "hr{border:none;border-top:1px solid #eaeef2;margin:18px 0}"
        + "dl.info{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px 20px;margin:0}"
        + "dl.info div{min-width:0}"
        + "dt{font-size:.72rem;text-transform:uppercase;letter-spacing:.04em;color:#656d76}"
        + "dd{margin:2px 0 0;overflow-wrap:anywhere}"
        + ".kv{background:#f6f8fa;border:1px solid #eaeef2;border-radius:5px;padding:1px 7px;"
        + "display:inline-block;margin:1px 0}"
        + ".code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.8rem;"
        + "color:#424a53;background:#f6f8fa;padding:1px 6px;border-radius:4px}"
        + ".type{font-size:.75rem;color:#656d76}"
        + ".footer{color:#8c959f;font-size:.8rem;margin-top:8px}"
        + ".card-head{display:flex;flex-wrap:wrap;align-items:baseline;justify-content:space-between;"
        + "gap:8px;margin-bottom:12px}"
        + ".card-head h2{margin:0}"
        + ".graph-toolbar{display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;"
        + "gap:10px;margin-bottom:10px}"
        + ".graph-toolbar .legend{font-size:.78rem;color:#656d76}"
        + ".graph-scroll{overflow:auto;border:1px solid #eaeef2;border-radius:10px;"
        + "background-color:#fbfcfd;background-image:radial-gradient(#dfe3e8 1px,transparent 1px);"
        + "background-size:20px 20px}"
        + "svg.graph{display:block;margin:0 auto;user-select:none;touch-action:none}"
        + ".edge{fill:none;stroke:#c9d1d9;stroke-width:1.6}"
        + ".gnode{cursor:grab;outline:none}"
        + ".gnode rect{fill:#fff;stroke:#c9d1d9;stroke-width:1.4;transition:stroke .12s,fill .12s}"
        + ".gnode:hover rect{stroke:#57606a}"
        + ".gnode:focus-visible rect{stroke:#0969da;stroke-width:2.2}"
        + ".gnode.selected rect{stroke:#0969da;stroke-width:2.4}"
        + ".gnode.inactive rect{fill:#fff8f8;stroke-dasharray:4 3}"
        + ".gnode.drop-target rect{stroke:#1a7f37;stroke-width:2.6;fill:#f0fff4}"
        + ".gnode text{pointer-events:none}"
        + ".gname{font-size:13px;font-weight:600;fill:#1f2328}"
        + ".gline{font-size:10.5px;fill:#656d76}"
        + ".gmeta{font-size:10px;fill:#8c959f}"
        + ".gdot.ok{fill:#1a7f37}"
        + ".gdot.off{fill:#cf222e}"
        + ".drag-line{fill:none;stroke:#0969da;stroke-width:2;stroke-dasharray:5 4;pointer-events:none}"
        + ".hidden-form{display:none}"
        + ".breadcrumb{font-size:.84rem;color:#656d76;margin:0 0 12px}"
        + ".breadcrumb a{color:#0969da;text-decoration:none}"
        + ".path{font-size:.84rem;color:#424a53;background:#f6f8fa;border:1px solid #eaeef2;"
        + "border-radius:6px;padding:6px 9px;margin:0 0 12px;overflow-wrap:anywhere}"
        + ".preview{background:#0d1117;color:#e6edf3;border-radius:10px;padding:16px;overflow:auto;"
        + "max-height:520px;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;"
        + "font-size:.8rem;line-height:1.5;margin:0}"
        + ".empty{padding:28px 16px;text-align:center;color:#656d76;font-size:.9rem}"
        + ".danger-zone{border-top:1px solid #eaeef2;margin-top:16px;padding-top:12px}"
        + "[hidden]{display:none!important}"
        + ".workspace{display:grid;grid-template-columns:minmax(0,1fr) 360px;gap:16px;align-items:start;"
        + "margin-bottom:16px}"
        + ".canvas-card{padding:14px 14px 16px;margin-bottom:0}"
        + ".canvas-card h2{font-size:.98rem;margin:0}"
        + ".toolbar-title{display:flex;flex-wrap:wrap;align-items:baseline;gap:8px}"
        + ".inspector{margin-bottom:0;position:sticky;top:16px;max-height:calc(100vh - 32px);overflow:auto}"
        + ".inspector h2{font-size:1.06rem;margin:0 0 2px;overflow-wrap:anywhere}"
        + ".inspector .meta{font-size:.8rem;margin:0 0 10px}"
        + ".graph-toolbar{display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;"
        + "gap:10px;margin-bottom:8px}"
        + ".legend{font-size:.78rem;color:#656d76;margin:0 0 10px}"
        + ".graph-scroll{max-height:calc(100vh - 250px);min-height:420px}"
        + ".gnode.moving{cursor:grabbing}"
        + ".gnode.moving rect{stroke:#0969da}"
        + ".gnode.link-source{opacity:.45}"
        + "svg.graph.moving{cursor:grabbing}"
        + "svg.graph.linking{cursor:crosshair}"
        + ".add-child{cursor:pointer;outline:none}"
        + ".add-child circle{fill:#fff;stroke:#c9d1d9;stroke-width:1.4;transition:stroke .12s,fill .12s}"
        + ".add-child path{stroke:#57606a;stroke-width:1.6;fill:none}"
        + ".add-child:hover circle,.add-child:focus-visible circle{stroke:#0969da;fill:#eef6ff}"
        + ".add-child:hover path{stroke:#0969da}"
        + "details.card{padding:0}"
        + "details.card>summary{display:flex;flex-wrap:wrap;align-items:baseline;gap:8px;padding:14px 20px;"
        + "cursor:pointer;font-weight:600;list-style:none}"
        + "details.card>summary::-webkit-details-marker{display:none}"
        + "details.card>summary::before{content:\"▸\";color:#8c959f;font-size:.8rem}"
        + "details.card[open]>summary::before{content:\"▾\"}"
        + "details.card[open]>summary{border-bottom:1px solid #eaeef2}"
        + ".details-body{padding:16px 20px 18px}"
        + ".summary-note{font-weight:400;font-size:.82rem;color:#656d76}"
        + "details.subdetails{border:1px solid #eaeef2;border-radius:8px;padding:8px 10px;margin:0}"
        + "details.subdetails>summary{cursor:pointer;font-size:.82rem;font-weight:600;color:#424a53;"
        + "list-style:none}"
        + "details.subdetails>summary::-webkit-details-marker{display:none}"
        + "details.subdetails[open]>summary{margin-bottom:10px}"
        + ".inspector-actions{display:flex;flex-wrap:wrap;gap:8px;align-items:center}"
        + "dialog{border:none;border-radius:12px;padding:0;margin:auto;width:min(680px,94vw);"
        + "max-height:88vh;background:#fff;color:#1f2328;box-shadow:0 24px 64px rgba(27,31,36,.35)}"
        + "dialog::backdrop{background:rgba(27,31,36,.45)}"
        + ".dialog-form{display:flex;flex-direction:column;max-height:88vh}"
        + ".dialog-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;"
        + "padding:16px 20px;border-bottom:1px solid #eaeef2}"
        + ".dialog-head h2{margin:0;font-size:1.05rem}"
        + ".dialog-head p{margin:4px 0 0;font-size:.82rem}"
        + ".dialog-body{padding:16px 20px;overflow:auto}"
        + ".dialog-body .grid{gap:10px 14px}"
        + ".dialog-foot{display:flex;justify-content:flex-end;gap:8px;padding:12px 20px;"
        + "border-top:1px solid #eaeef2;background:#f6f8fa;border-radius:0 0 12px 12px}"
        + ".icon-btn{border:none;background:none;color:#656d76;font-size:1.35rem;line-height:1;"
        + "padding:2px 7px;border-radius:6px;cursor:pointer}"
        + ".icon-btn:hover{background:#f0f2f4;color:#1f2328}"
        + "@media (max-width:1080px){.workspace{grid-template-columns:1fr}"
        + ".inspector{position:static;max-height:none}}"
        + "@media (max-width:720px){form.grid,dl.info{grid-template-columns:1fr}.span-2{grid-column:auto}"
        + ".graph-scroll{max-height:70vh}}";

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
     * <p>The identifiers let the page re-route the cable while a node is dragged.</p>
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
     * Removes the fragment of a redirect target so error messages at the top stay visible.
     *
     * @param target redirect target
     * @return the target without its fragment
     */
    static String withoutAnchor(String target)
    {
        int hash = target.indexOf('#');

        return hash < 0 ? target : target.substring(0, hash);
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
     * @param confirm whether a confirmation dialog is required
     * @return the HTML form
     */
    static String postButton(String action, String field, String value, String label, String css, boolean confirm)
    {
        StringBuilder html = new StringBuilder("<form method=\"post\" class=\"inline\"");

        if (confirm)
        {
            html.append(" onsubmit=\"return confirm('Confirm this action?')\"");
        }

        html.append('>');
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
    <title>DESS — Institution unavailable</title>
    <style><%= CSS %></style>
</head>
<body>
<main class="wrap">
    <header class="top">
        <div>
            <h1>DESS · Institution Tree</h1>
            <p class="sub">Graph editor for the institution module</p>
        </div>
    </header>
    <div class="flash error">
        <strong>The institution module could not be opened.</strong><br>
        <%= esc(rootMessage(error)) %>
    </div>
    <p class="muted">
        Check the XML file configured by <span class="mono">DESS_INSTITUTION_CONFIG</span>
        (default <span class="mono">config/institution.xml</span>), then reload this page.
    </p>
</main>
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

        if ("initialize".equals(action) || "updateInfo".equals(action) || "reload".equals(action))
        {
            redirectTarget = "#institution";
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
                 || "deactivateUnit".equals(action))
        {
            String id = trim(request.getParameter("id"));
            redirectTarget = id == null || id.isEmpty()
                ? "#editor"
                : "?node=" + urlEncode(id) + "#editor";
        }
        else if ("moveUnit".equals(action))
        {
            String id = trim(request.getParameter("id"));
            redirectTarget = id == null || id.isEmpty()
                ? "#graph"
                : "?node=" + urlEncode(id) + "#graph";
        }
        else if ("deleteUnit".equals(action))
        {
            redirectTarget = "#graph";
        }
        else if ("createType".equals(action) || "activateType".equals(action)
                 || "deactivateType".equals(action) || "deleteType".equals(action)
                 || "addPair".equals(action) || "removePair".equals(action))
        {
            redirectTarget = "#types";
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
                message = "Tree '" + updated.name() + "' saved.";
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
                redirectTarget = "?node=" + urlEncode(moved.id()) + "#graph";
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
            redirectTarget = withoutAnchor(redirectTarget);
        }
        catch (RuntimeException error)
        {
            kind = "error";
            message = rootMessage(error);
            redirectTarget = withoutAnchor(redirectTarget);
        }

        session.setAttribute("dess.flash.kind", kind);
        session.setAttribute("dess.flash.text", message);
        response.sendRedirect(request.getContextPath() + "/" + redirectTarget);
        return;
    }

    String flashKind = (String) session.getAttribute("dess.flash.kind");
    String flashText = (String) session.getAttribute("dess.flash.text");
    session.removeAttribute("dess.flash.kind");
    session.removeAttribute("dess.flash.text");

    ConfigurationStatus status = institutionService.getStatus();
    String requestedParent = trim(request.getParameter("parent"));
    boolean openCreateDialog = status == ConfigurationStatus.READY
        && !"error".equals(flashKind)
        && (request.getParameter("create") != null || requestedParent != null);
    String openCreateAttribute = openCreateDialog
        ? " data-open-create=\"" + esc(requestedParent == null ? "" : requestedParent) + "\""
        : "";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>DESS — Institution Tree</title>
    <style><%= CSS %></style>
</head>
<body<%= openCreateAttribute %>>
<main class="wrap">
    <header class="top">
        <div>
            <h1>DESS · Institution Tree</h1>
            <p class="sub">Visual editor for the institution module</p>
        </div>
        <div class="header-actions">
            <span class="badge <%= status == ConfigurationStatus.READY ? "ok" : "off" %>">
                <%= status == ConfigurationStatus.READY ? "READY" : "NOT_INITIALIZED" %>
            </span>
            <% if (status == ConfigurationStatus.READY) { %>
            <a class="btn btn-sm btn-ghost" href="?check=1">Check</a>
            <a class="btn btn-sm btn-ghost" href="?preview=1#xml">XML</a>
            <a class="btn btn-sm" href="<%= request.getContextPath() %>/export.jsp">Download</a>
            <% } %>
        </div>
    </header>

    <% if (flashText != null) { %>
    <div class="flash <%= "error".equals(flashKind) ? "error" : "ok" %>"><%= esc(flashText) %></div>
    <% } %>

    <% if (status != ConfigurationStatus.READY) { %>
    <section class="card" id="institution">
        <h2>Create a named tree</h2>
        <p class="muted">
            No configuration is loaded. The expected file is
            <span class="mono"><%= esc(configFile.toString()) %></span>.
        </p>

        <form method="post" class="grid">
            <input type="hidden" name="action" value="initialize">
            <label>Tree code
                <input name="code" required maxlength="64" pattern="[A-Za-z0-9][A-Za-z0-9_.-]*"
                       placeholder="TEST_UNIV">
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
            <input type="hidden" name="action" value="loadExample">
            <select name="example" aria-label="Example">
                <option value="university">University (5 levels)</option>
                <option value="high-school">High school (5 levels)</option>
                <option value="custom-school">Conservatory (custom types)</option>
            </select>
            <button class="btn" type="submit">Load the example</button>
        </form>

        <hr>

        <form method="post" class="inline">
            <input type="hidden" name="action" value="reload">
            <button class="btn btn-sm btn-ghost" type="submit">Reload XML</button>
        </form>
    </section>
    <% } else {
        InstitutionInfo info = institutionService.getInfo();
        InstitutionConfig institutionConfig = institutionService.getConfig();
        List<UnitTypeDefinition> allTypes = unitTypeService.getAll();
        List<AllowedParentChildPair> pairs = unitTypeService.getAllowedPairs();
        List<OrganizationalUnit> roots = hierarchyService.getRoots();
        List<UnitTypeDefinition> activeTypes = new ArrayList<>();
        ValidationReport report = null;
        String selectedId = trim(request.getParameter("node"));
        OrganizationalUnit selectedUnit = selectedId == null || selectedId.isEmpty()
            ? null
            : hierarchyService.getById(selectedId).orElse(null);
        boolean selectedMissing = selectedId != null && !selectedId.isEmpty() && selectedUnit == null;
        String xmlPreview = null;
        String xmlPreviewError = null;

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

        if (request.getParameter("preview") != null)
        {
            try
            {
                xmlPreview = Files.readString(configFile, StandardCharsets.UTF_8);
            }
            catch (IOException error)
            {
                xmlPreviewError = rootMessage(error);
            }
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

        Map<String, List<String>> allowedParentsByType = new LinkedHashMap<>();

        for (UnitTypeDefinition type : activeTypes)
        {
            allowedParentsByType.put(type.code(), unitTypeService.getAllowedParentTypes(type.code()));
        }
    %>
    <details class="card" id="tree">
        <summary>
            <span>Tree settings</span>
            <span class="summary-note">
                <%= esc(info.name()) %> · <span class="mono"><%= esc(info.code()) %></span>
            </span>
        </summary>
        <div class="details-body">
            <form method="post" class="grid">
                <input type="hidden" name="action" value="updateInfo">
                <label>Tree code
                    <input name="code" required maxlength="64" pattern="[A-Za-z0-9][A-Za-z0-9_.-]*"
                           value="<%= esc(info.code()) %>">
                </label>
                <label>Tree name
                    <input name="name" required maxlength="120" value="<%= esc(info.name()) %>">
                </label>
                <label class="span-2">Description
                    <input name="description" maxlength="255" value="<%= esc(info.description()) %>"
                           placeholder="Optional">
                </label>
                <div class="form-actions span-2">
                    <button class="btn" type="submit">Save the tree</button>
                </div>
            </form>

            <div class="form-actions" style="margin-top:12px">
                <form method="post" class="inline">
                    <input type="hidden" name="action" value="reload">
                    <button class="btn btn-sm btn-ghost" type="submit">Reload XML</button>
                </form>
                <span class="summary-note">id <span class="mono"><%= esc(info.id()) %></span></span>
            </div>

            <p class="hint" style="margin-top:12px">
                XML file: <span class="mono"><%= esc(configFile.toString()) %></span>
            </p>
        </div>
    </details>

    <% if (report != null) { %>
        <% if (report.valid()) { %>
        <div class="flash ok">The configuration is valid.</div>
        <% } else { %>
        <div class="flash error">
            <strong>The configuration is invalid:</strong>
            <ul>
                <% for (String error : report.errors()) { %>
                <li><%= esc(error) %></li>
                <% } %>
            </ul>
        </div>
        <% } %>
    <% } %>

    <% if (request.getParameter("preview") != null) { %>
    <details class="card" id="xml" open>
        <summary>
            <span>XML preview</span>
            <span class="summary-note">persisted file</span>
        </summary>
        <div class="details-body">
            <p class="muted" style="margin-top:0">
                This is the current persisted file generated from the named tree.
            </p>
            <% if (xmlPreviewError != null) { %>
            <div class="flash error"><%= esc(xmlPreviewError) %></div>
            <% } else { %>
            <pre class="preview"><%= esc(xmlPreview) %></pre>
            <% } %>
        </div>
    </details>
    <% } %>

    <div class="workspace">
        <section class="card canvas-card" id="graph">
            <div class="graph-toolbar">
                <div class="toolbar-title">
                    <h2>Hierarchy</h2>
                    <span class="muted"><%= graphBoxes.size() %> unit(s)</span>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-sm btn-ghost" id="tidyLayout">Tidy layout</button>
                    <button type="button" class="btn btn-sm new-root">+ New root</button>
                </div>
            </div>
            <p class="legend">
                Drag a node to move it · Shift-drag onto another node to re-parent it ·
                use <strong>+</strong> on a node to add a child. Positions are kept in this browser.
            </p>

            <% if (graphBoxes.isEmpty()) { %>
            <div class="empty">
                No unit yet.
                <button type="button" class="btn btn-sm new-root">Create the first root unit</button>
            </div>
            <% } else { %>
            <div class="graph-scroll">
                <svg id="graphSvg" class="graph" width="<%= graphWidth %>" height="<%= graphHeight %>"
                     viewBox="0 0 <%= graphWidth %> <%= graphHeight %>" role="group"
                     data-tree="<%= esc(info.id()) %>"
                     data-width="<%= graphWidth %>" data-height="<%= graphHeight %>"
                     aria-label="Institution unit graph">
                    <g class="edges">
                        <% for (GraphEdge edge : graphEdges) { %>
                        <path class="edge"
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
                        <g class="gnode<%= selected ? " selected" : "" %><%= unit.active() ? "" : " inactive" %>"
                           data-id="<%= esc(unit.id()) %>"
                           data-name="<%= esc(unit.name()) %>"
                           data-code="<%= esc(unit.code()) %>"
                           data-type="<%= esc(unit.typeCode()) %>"
                           data-active="<%= unit.active() %>"
                           data-x="<%= box.x %>"
                           data-y="<%= box.y %>"
                           data-home-x="<%= box.x %>"
                           data-home-y="<%= box.y %>"
                           transform="translate(<%= box.x %>,<%= box.y %>)"
                           tabindex="0" role="button"
                           aria-label="<%= esc(unit.name() + ", " + unit.code() + ", type " + unit.typeCode()) %>">
                            <title><%= esc(unit.name() + " (" + unit.code() + ") · " + unit.typeCode()
                                             + (unit.active() ? " · active" : " · inactive")) %></title>
                            <rect width="<%= GRAPH_NODE_W %>" height="<%= GRAPH_NODE_H %>" rx="10"></rect>
                            <circle class="gdot <%= unit.active() ? "ok" : "off" %>"
                                    cx="<%= GRAPH_NODE_W - 14 %>" cy="14" r="4.5"></circle>
                            <text class="gname" x="12" y="25"><%= esc(clipText(unit.name(), 25)) %></text>
                            <text class="gline" x="12" y="43"><%= esc(clipText(
                                unit.code() + " · " + unit.typeCode(), 32)) %></text>
                            <text class="gmeta" x="12" y="58"><%= esc(meta) %></text>
                        </g>
                        <% } %>
                    </g>
                    <g class="adds">
                        <% for (GraphBox box : graphBoxes) { %>
                        <g class="add-child" data-for="<%= esc(box.unit.id()) %>"
                           transform="translate(<%= box.x + GRAPH_NODE_W + 16 %>,<%= box.y + GRAPH_NODE_H / 2 %>)"
                           tabindex="0" role="button"
                           aria-label="<%= esc("Add a child under " + box.unit.name()) %>">
                            <title>Add a child under <%= esc(box.unit.name()) %></title>
                            <circle r="11"></circle>
                            <path d="M-5,0 H5 M0,-5 V5"></path>
                        </g>
                        <% } %>
                    </g>
                </svg>
            </div>
            <% } %>

            <form id="moveForm" method="post" class="hidden-form">
                <input type="hidden" name="action" value="moveUnit">
                <input type="hidden" name="id" id="moveId">
                <input type="hidden" name="parentId" id="moveParentId">
            </form>
        </section>

    <aside class="card inspector" id="editor">
        <% if (selectedMissing) { %>
        <h2>Node</h2>
        <div class="flash error">This node no longer exists.</div>
        <% } else if (selectedUnit == null) { %>
        <h2>Node</h2>
        <p class="muted" style="margin-top:0">
            Select a node in the graph to edit its fields, parent and status.
        </p>
        <p class="muted">
            Drag nodes to arrange the canvas. Shift-drag one node onto another to change its
            parent. Positions are stored in this browser; <em>Tidy layout</em> restores the
            automatic tree.
        </p>
        <button type="button" class="btn btn-sm btn-ghost new-root">+ New root unit</button>
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
            <h2><%= esc(selectedUnit.name()) %></h2>
            <span class="badge <%= selectedUnit.active() ? "ok" : "off" %>">
                <%= selectedUnit.active() ? "active" : "inactive" %>
            </span>
        </div>
        <p class="meta">
            <span class="mono"><%= esc(selectedUnit.code()) %></span> ·
            <%= esc(selectedUnit.typeCode()) %>
            <% if (selectedType != null && !selectedType.label().equals(selectedType.code())) { %>
            · <%= esc(selectedType.label()) %>
            <% } %>
        </p>
        <p class="breadcrumb">
            <a href="#graph">Graph</a> / <%= esc(pathText.toString()) %>
        </p>

        <form method="post" class="stack">
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
            <details class="subdetails">
                <summary>Attributes (<%= selectedType.attributes().size() %>)</summary>
                <div class="stack">
                    <% for (AttributeDefinition attribute : selectedType.attributes()) {
                        AttributeValue current = selectedUnit.values().get(attribute.key());
                        String raw = current == null ? "" : current.value();
                        boolean required = attribute.required() && !attribute.hasDefault();
                    %>
                    <label>
                        <%= esc(attribute.label()) %><%= attribute.required() ? " *" : "" %>
                        <% if (attribute.type() == AttributeType.BOOLEAN) { %>
                        <select name="attr_<%= esc(attribute.key()) %>" <%= required ? "required" : "" %>>
                            <option value="">(default value)</option>
                            <option value="true" <%= "true".equals(raw) ? "selected" : "" %>>true</option>
                            <option value="false" <%= "false".equals(raw) ? "selected" : "" %>>false</option>
                        </select>
                        <% } else { %>
                        <input type="<%= inputType(attribute.type()) %>"
                               name="attr_<%= esc(attribute.key()) %>"
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
            </details>
            <% } %>

            <div>
                <button class="btn" type="submit">Save changes</button>
            </div>
        </form>

        <hr>

        <h3>Parent</h3>
        <p class="muted" style="margin-top:0">
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

        <form method="post" class="stack">
            <input type="hidden" name="action" value="moveUnit">
            <input type="hidden" name="id" value="<%= esc(selectedUnit.id()) %>">
            <label>New parent
                <select name="parentId">
                    <option value="" <%= selectedUnit.parentId() == null ? "selected" : "" %>>
                        (none — make this node a root)
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
                <button class="btn btn-sm" type="submit">Move node</button>
            </div>
        </form>
        <p class="hint" style="margin-top:6px">
            Or Shift-drag the node onto the new parent in the graph.
        </p>

        <hr>

        <div class="inspector-actions">
            <button type="button" class="btn btn-sm btn-ghost add-child-btn"
                    data-parent-id="<%= esc(selectedUnit.id()) %>"
                    data-parent-name="<%= esc(selectedUnit.name()) %>"
                    data-parent-code="<%= esc(selectedUnit.code()) %>"
                    data-parent-type="<%= esc(selectedUnit.typeCode()) %>"
                    data-parent-active="<%= selectedUnit.active() %>">+ Add child</button>
            <%= selectedUnit.active()
                ? "<form method=\"post\" class=\"inline\""
                  + " onsubmit=\"return confirm('Deactivate this unit and its descendants?')\">"
                  + "<input type=\"hidden\" name=\"action\" value=\"deactivateUnit\">"
                  + "<input type=\"hidden\" name=\"id\" value=\"" + esc(selectedUnit.id()) + "\">"
                  + "<input type=\"hidden\" name=\"cascade\" value=\"true\">"
                  + "<button type=\"submit\" class=\"btn btn-sm btn-ghost\">Deactivate</button></form>"
                : postButton("activateUnit", "id", selectedUnit.id(), "Activate", "btn-ghost", false) %>
            <form method="post" class="inline"
                  onsubmit="return confirm('Delete this unit? Deletion is refused when it has children.')">
                <input type="hidden" name="action" value="deleteUnit">
                <input type="hidden" name="id" value="<%= esc(selectedUnit.id()) %>">
                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
            </form>
        </div>
        <% } %>
    </aside>
    </div>

    <details class="card" id="types">
        <summary>
            <span>Unit types and relations</span>
            <span class="summary-note">
                <%= allTypes.size() %> type(s) · <%= pairs.size() %> relation(s)
            </span>
        </summary>
        <div class="details-body">
        <% if (allTypes.isEmpty()) { %>
        <p class="muted">No unit types. Create one to build a hierarchy.</p>
        <% } else { %>
        <table>
            <thead>
            <tr>
                <th>Code</th>
                <th>Label</th>
                <th>State</th>
                <th>Attributes</th>
                <th>Allowed children</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <% for (UnitTypeDefinition type : allTypes) {
                List<String> childTypes = unitTypeService.getAllowedChildTypes(type.code());
            %>
            <tr>
                <td class="mono"><%= esc(type.code()) %></td>
                <td>
                    <%= esc(type.label()) %>
                    <% if (type.description() != null) { %>
                    <div class="muted"><%= esc(type.description()) %></div>
                    <% } %>
                </td>
                <td>
                    <span class="badge <%= type.active() ? "ok" : "off" %>">
                        <%= type.active() ? "active" : "inactive" %>
                    </span>
                </td>
                <td>
                    <% if (type.attributes().isEmpty()) { %>
                    <span class="muted">—</span>
                    <% } else {
                        for (AttributeDefinition attribute : type.attributes()) { %>
                    <span class="kv">
                        <span class="mono"><%= esc(attribute.key()) %></span> :
                        <%= esc(attribute.type().name()) %><%= attribute.required() ? " required" : "" %>
                    </span>
                    <%      }
                       } %>
                </td>
                <td>
                    <% if (childTypes.isEmpty()) { %>
                    <span class="muted">—</span>
                    <% } else { %>
                    <span class="mono"><%= esc(String.join(", ", childTypes)) %></span>
                    <% } %>
                </td>
                <td class="actions">
                    <%= type.active()
                        ? postButton("deactivateType", "code", type.code(), "Deactivate", "btn-ghost", false)
                        : postButton("activateType", "code", type.code(), "Activate", "btn-ghost", false) %>
                    <%= postButton("deleteType", "code", type.code(), "Delete", "btn-danger", true) %>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <% } %>

        <hr>

        <h3>Create a type</h3>
        <form method="post" class="grid">
            <input type="hidden" name="action" value="createType">
            <label>Code
                <input name="code" required maxlength="64" pattern="[A-Za-z0-9][A-Za-z0-9_.-]*"
                       placeholder="DEPARTMENT">
            </label>
            <label>Label
                <input name="label" required maxlength="120" placeholder="Department">
            </label>
            <div class="form-actions span-2">
                <button class="btn" type="submit">Create the type</button>
            </div>
        </form>

        <hr>

        <h3>Allowed relations</h3>
        <% if (pairs.isEmpty()) { %>
        <p class="muted">
            No relation. A root unit may use any type; a relation is required to create a child
            unit.
        </p>
        <% } else { %>
        <table>
            <thead>
            <tr>
                <th>Parent type</th>
                <th></th>
                <th>Child type</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <% for (AllowedParentChildPair pair : pairs) { %>
            <tr>
                <td class="mono"><%= esc(pair.parent()) %></td>
                <td>→</td>
                <td class="mono"><%= esc(pair.child()) %></td>
                <td class="actions">
                    <form method="post" class="inline"
                          onsubmit="return confirm('Confirm this action?')">
                        <input type="hidden" name="action" value="removePair">
                        <input type="hidden" name="parentType" value="<%= esc(pair.parent()) %>">
                        <input type="hidden" name="childType" value="<%= esc(pair.child()) %>">
                        <button class="btn btn-sm btn-danger" type="submit">Remove</button>
                    </form>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <% } %>

        <form method="post" class="row" style="margin-top:12px">
            <input type="hidden" name="action" value="addPair">
            <label>Parent type
                <select name="parentType" required>
                    <option value="">Choose…</option>
                    <% for (UnitTypeDefinition type : allTypes) { %>
                    <option value="<%= esc(type.code()) %>"><%= esc(type.code() + " — " + type.label()) %></option>
                    <% } %>
                </select>
            </label>
            <label>Child type
                <select name="childType" required>
                    <option value="">Choose…</option>
                    <% for (UnitTypeDefinition type : allTypes) { %>
                    <option value="<%= esc(type.code()) %>"><%= esc(type.code() + " — " + type.label()) %></option>
                    <% } %>
                </select>
            </label>
            <div>
                <button class="btn" type="submit">Allow the relation</button>
            </div>
        </form>
        </div>
    </details>

    <dialog id="createDialog" aria-labelledby="createTitle">
        <% if (activeTypes.isEmpty()) { %>
        <div class="dialog-head">
            <div>
                <h2 id="createTitle">New unit</h2>
                <p class="muted">No active unit type is available.</p>
            </div>
            <button type="button" class="icon-btn" data-close-dialog aria-label="Close">×</button>
        </div>
        <div class="dialog-body">
            <p class="muted" style="margin:0">
                Activate a type or create one under <a href="#types">Unit types and relations</a>
                before adding units.
            </p>
        </div>
        <% } else { %>
        <form method="post" class="dialog-form">
            <input type="hidden" name="action" value="createUnit">
            <input type="hidden" name="parentId" id="createParentId" value="">
            <div class="dialog-head">
                <div>
                    <h2 id="createTitle">New root unit</h2>
                    <p class="muted" id="createParentLine">
                        The unit is added at the top of the hierarchy.
                    </p>
                </div>
                <button type="button" class="icon-btn" data-close-dialog aria-label="Close">×</button>
            </div>
            <div class="dialog-body">
                <div class="grid">
                    <label>Name
                        <input id="createName" name="name" required maxlength="120"
                               autocomplete="off" placeholder="Computer Science Department">
                    </label>
                    <label>Code
                        <input id="createCode" name="code" required maxlength="64"
                               pattern="[A-Za-z0-9][A-Za-z0-9_.-]*" autocomplete="off"
                               placeholder="CS_DEPT">
                        <span class="hint">Generated from the name; edit it if needed.</span>
                    </label>
                    <label>Type
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
                        <input type="checkbox" name="active" id="createActive" checked>
                        Activate the unit on creation
                        <span class="hint" id="createActiveHint" hidden>
                            The selected parent is inactive, so the new unit must start inactive.
                        </span>
                    </label>
                </div>

                <p class="hint" id="createNoType" hidden>
                    No active type is allowed under this parent. Add an allowed relation under
                    Unit types and relations first.
                </p>

                <% for (UnitTypeDefinition type : activeTypes) {
                    if (type.attributes().isEmpty())
                    {
                        continue;
                    }
                %>
                <fieldset data-attributes="<%= esc(type.code()) %>" hidden disabled
                          style="margin-top:12px">
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
                            <select name="attr_<%= esc(attribute.key()) %>" <%= required ? "required" : "" %>>
                                <option value="">(default value)</option>
                                <option value="true">true</option>
                                <option value="false">false</option>
                            </select>
                            <% } else { %>
                            <input type="<%= inputType(attribute.type()) %>"
                                   name="attr_<%= esc(attribute.key()) %>"
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

    <p class="footer">
        Changes are validated by the institution module and then written atomically to the XML
        file. This page is only for local development.
    </p>

    <script>
        (function () {
            var NODE_W = <%= GRAPH_NODE_W %>;
            var NODE_H = <%= GRAPH_NODE_H %>;
            var MARGIN = <%= GRAPH_MARGIN %>;
            var PLUS_GAP = 16;
            var svg = document.getElementById('graphSvg');
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
            var moveForm = document.getElementById('moveForm');
            var moveId = document.getElementById('moveId');
            var moveParent = document.getElementById('moveParentId');
            var nodes = {};
            var adds = {};
            var positions = {};
            var storageKey = svg
                ? 'dess.institution.layout.' + svg.getAttribute('data-tree')
                : null;
            var activeNode = null;
            var currentTarget = null;
            var overlay = null;
            var dragged = false;
            var linkMode = false;
            var startClientX = 0;
            var startClientY = 0;
            var pointerX = 0;
            var pointerY = 0;
            var cableX = 0;
            var cableY = 0;
            var originX = 0;
            var originY = 0;
            var codeEdited = false;

            document.querySelectorAll('.gnode').forEach(function (node) {
                nodes[node.getAttribute('data-id')] = node;
            });

            document.querySelectorAll('.add-child').forEach(function (add) {
                adds[add.getAttribute('data-for')] = add;
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
                    createActiveHint.hidden = !parent || parent.active;
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

            if (dialog) {
                dialog.addEventListener('click', function (event) {
                    if (event.target === dialog && dialog.close) {
                        dialog.close();
                    }
                });
            }

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

            if (svg) {
                svg.querySelectorAll('.gnode').forEach(function (node) {
                    var id = node.getAttribute('data-id');

                    positions[id] = {
                        x: parseFloat(node.getAttribute('data-x')),
                        y: parseFloat(node.getAttribute('data-y'))
                    };

                    node.addEventListener('pointerdown', onNodePointerDown);
                    node.addEventListener('keydown', function (event) {
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            openNode(id);
                        }
                    });
                });

                svg.querySelectorAll('.add-child').forEach(function (add) {
                    var id = add.getAttribute('data-for');

                    add.addEventListener('pointerdown', function (event) {
                        event.stopPropagation();
                    });

                    add.addEventListener('click', function (event) {
                        event.preventDefault();
                        event.stopPropagation();
                        openForNode(id);
                    });

                    add.addEventListener('keydown', function (event) {
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            openForNode(id);
                        }
                    });
                });

                loadStored();
                Object.keys(positions).forEach(applyPosition);
                routeEdges();
                updateExtent();

                var tidyButton = document.getElementById('tidyLayout');

                if (tidyButton) {
                    tidyButton.addEventListener('click', tidy);
                }
            }

            var requestedParent = document.body.getAttribute('data-open-create');

            if (requestedParent !== null) {
                openForNode(requestedParent);
            }

            function svgPoint(event) {
                var point = svg.createSVGPoint();
                point.x = event.clientX;
                point.y = event.clientY;
                return point.matrixTransform(svg.getScreenCTM().inverse());
            }

            function openNode(id) {
                window.location.href = '?node=' + encodeURIComponent(id) + '#editor';
            }

            function applyPosition(id) {
                var position = positions[id];
                var node = nodes[id];
                var add = adds[id];

                if (!position || !node) {
                    return;
                }

                node.setAttribute('transform', 'translate(' + position.x + ',' + position.y + ')');

                if (add) {
                    add.setAttribute('transform', 'translate('
                        + (position.x + NODE_W + PLUS_GAP) + ',' + (position.y + NODE_H / 2) + ')');
                }
            }

            function routeEdges() {
                svg.querySelectorAll('.edge').forEach(function (edge) {
                    var parent = positions[edge.getAttribute('data-parent')];
                    var child = positions[edge.getAttribute('data-child')];

                    if (!parent || !child) {
                        return;
                    }

                    var x1 = parent.x + NODE_W / 2;
                    var y1 = parent.y + NODE_H;
                    var x2 = child.x + NODE_W / 2;
                    var y2 = child.y;
                    var bend = Math.max(40, Math.abs(y2 - y1) / 2);

                    edge.setAttribute('d', 'M' + x1 + ',' + y1
                        + ' C' + x1 + ',' + (y1 + bend)
                        + ' ' + x2 + ',' + (y2 - bend)
                        + ' ' + x2 + ',' + y2);
                });
            }

            function updateExtent() {
                var width = parseFloat(svg.getAttribute('data-width')) || 0;
                var height = parseFloat(svg.getAttribute('data-height')) || 0;

                Object.keys(positions).forEach(function (id) {
                    width = Math.max(width, positions[id].x + NODE_W + PLUS_GAP + 28);
                    height = Math.max(height, positions[id].y + NODE_H + MARGIN);
                });

                width = Math.ceil(width);
                height = Math.ceil(height);
                svg.setAttribute('width', width);
                svg.setAttribute('height', height);
                svg.setAttribute('viewBox', '0 0 ' + width + ' ' + height);
            }

            function loadStored() {
                if (!storageKey) {
                    return;
                }

                try {
                    var stored = JSON.parse(window.localStorage.getItem(storageKey) || '{}');

                    Object.keys(stored).forEach(function (id) {
                        if (positions[id]
                            && typeof stored[id].x === 'number'
                            && typeof stored[id].y === 'number') {
                            positions[id].x = Math.max(MARGIN, stored[id].x);
                            positions[id].y = Math.max(MARGIN, stored[id].y);
                        }
                    });
                } catch (error) {
                    // Ignore an unreadable layout entry.
                }
            }

            function persist() {
                if (!storageKey) {
                    return;
                }

                try {
                    window.localStorage.setItem(storageKey, JSON.stringify(positions));
                } catch (error) {
                    // Private browsing or a full quota: the layout is not kept.
                }
            }

            function clearTarget() {
                if (currentTarget) {
                    currentTarget.classList.remove('drop-target');
                }

                currentTarget = null;
            }

            function cleanup() {
                clearTarget();

                if (overlay && overlay.parentNode) {
                    overlay.parentNode.removeChild(overlay);
                }

                if (activeNode) {
                    activeNode.classList.remove('link-source');
                    activeNode.classList.remove('moving');
                }

                overlay = null;
                activeNode = null;
                svg.classList.remove('linking');
                svg.classList.remove('moving');
            }

            function onNodePointerDown(event) {
                if (event.button !== 0) {
                    return;
                }

                var id = event.currentTarget.getAttribute('data-id');

                if (!positions[id]) {
                    return;
                }

                activeNode = event.currentTarget;
                dragged = false;
                linkMode = event.shiftKey;
                startClientX = event.clientX;
                startClientY = event.clientY;
                originX = positions[id].x;
                originY = positions[id].y;
                cableX = originX + NODE_W / 2;
                cableY = originY + NODE_H / 2;

                var point = svgPoint(event);
                pointerX = point.x;
                pointerY = point.y;

                document.addEventListener('pointermove', onPointerMove);
                document.addEventListener('pointerup', onPointerUp);
                document.addEventListener('pointercancel', onPointerUp);
            }

            function onPointerMove(event) {
                if (!activeNode) {
                    return;
                }

                if (!dragged && Math.abs(event.clientX - startClientX)
                    + Math.abs(event.clientY - startClientY) < 4) {
                    return;
                }

                if (!dragged) {
                    dragged = true;

                    if (linkMode) {
                        activeNode.classList.add('link-source');
                        svg.classList.add('linking');
                        overlay = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                        overlay.setAttribute('class', 'drag-line');
                        overlay.setAttribute('x1', cableX);
                        overlay.setAttribute('y1', cableY);
                        svg.appendChild(overlay);
                    } else {
                        activeNode.classList.add('moving');
                        svg.classList.add('moving');
                    }
                }

                var point = svgPoint(event);

                if (linkMode) {
                    overlay.setAttribute('x2', point.x);
                    overlay.setAttribute('y2', point.y);
                    clearTarget();

                    var element = document.elementFromPoint(event.clientX, event.clientY);
                    var target = element && element.closest ? element.closest('.gnode') : null;

                    if (target && target !== activeNode) {
                        currentTarget = target;
                        currentTarget.classList.add('drop-target');
                    }

                    return;
                }

                var id = activeNode.getAttribute('data-id');
                positions[id].x = Math.max(MARGIN, Math.round(originX + point.x - pointerX));
                positions[id].y = Math.max(MARGIN, Math.round(originY + point.y - pointerY));
                applyPosition(id);
                routeEdges();
                updateExtent();
            }

            function onPointerUp() {
                document.removeEventListener('pointermove', onPointerMove);
                document.removeEventListener('pointerup', onPointerUp);
                document.removeEventListener('pointercancel', onPointerUp);

                if (!activeNode) {
                    return;
                }

                var id = activeNode.getAttribute('data-id');
                var targetId = currentTarget ? currentTarget.getAttribute('data-id') : null;

                if (!dragged) {
                    cleanup();
                    openNode(id);
                    return;
                }

                cleanup();

                if (linkMode) {
                    if (targetId && targetId !== id && moveForm && moveId && moveParent) {
                        moveId.value = id;
                        moveParent.value = targetId;
                        moveForm.submit();
                    }

                    return;
                }

                persist();
                updateExtent();
            }

            function tidy() {
                Object.keys(nodes).forEach(function (id) {
                    positions[id].x = parseFloat(nodes[id].getAttribute('data-home-x'));
                    positions[id].y = parseFloat(nodes[id].getAttribute('data-home-y'));
                    applyPosition(id);
                });

                try {
                    window.localStorage.removeItem(storageKey);
                } catch (error) {
                    // Ignore.
                }

                var width = parseFloat(svg.getAttribute('data-width')) || 0;
                var height = parseFloat(svg.getAttribute('data-height')) || 0;
                svg.setAttribute('width', width);
                svg.setAttribute('height', height);
                svg.setAttribute('viewBox', '0 0 ' + width + ' ' + height);
                routeEdges();
            }
        })();
    </script>
    <% } %>
</main>
</body>
</html>
