package com.dess.institution.infrastructure.xml;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeType;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ConfigurationLoadException;
import com.dess.institution.exception.MalformedXmlException;
import com.dess.institution.exception.SchemaViolationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture du fichier XML : niveau 1 (bien formé) et niveau 2 (XSD). Cette classe ne contient aucune
 * règle métier : elle ne fait que traduire le XML en objets de domaine. La validation sémantique
 * est appliquée ensuite par {@code InstitutionValidator}.
 */
public final class XmlConfigLoader {

    static final String XSD_RESOURCE = "/institution.xsd";

    private final Schema schema;

    public XmlConfigLoader() {
        this.schema = loadDefaultSchema();
    }

    public InstitutionConfig load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.toString());
        } catch (IOException e) {
            throw new ConfigurationLoadException("Lecture impossible du fichier " + file + " : " + e.getMessage(), e);
        }
    }

    public InstitutionConfig parse(String xml) {
        return read(new InputSource(new StringReader(xml)), "<chaîne XML>");
    }

    public InstitutionConfig load(InputStream in, String sourceName) {
        return read(new InputSource(in), sourceName);
    }

    private InstitutionConfig read(InputSource source, String sourceName) {
        Document doc = parseAndValidate(source, sourceName);
        return toConfig(doc.getDocumentElement());
    }

    private Document parseAndValidate(InputSource source, String sourceName) {
        DocumentBuilderFactory factory = SecureXml.documentBuilderFactory();
        factory.setSchema(schema);
        List<String> schemaErrors = new ArrayList<>();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // ignoré
                }

                @Override
                public void error(SAXParseException e) {
                    schemaErrors.add("ligne " + e.getLineNumber() + " : " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            Document doc = builder.parse(source);
            if (!schemaErrors.isEmpty()) {
                throw new SchemaViolationException(sourceName, schemaErrors);
            }
            return doc;
        } catch (SAXParseException e) {
            throw new MalformedXmlException("Le fichier " + sourceName + " n'est pas du XML valide (ligne "
                    + e.getLineNumber() + ") : " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new MalformedXmlException("Le fichier " + sourceName + " n'est pas du XML valide : "
                    + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConfigurationLoadException("Lecture impossible de " + sourceName + " : " + e.getMessage(), e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- DOM -> domaine

    private InstitutionConfig toConfig(Element root) {
        List<UnitTypeDefinition> types = new ArrayList<>();
        Element typesEl = child(root, "unitTypes");
        if (typesEl != null) {
            for (Element t : children(typesEl, "type")) {
                types.add(readType(t));
            }
        }

        List<AllowedParentChildPair> pairs = new ArrayList<>();
        Element pairsEl = child(root, "allowedParentChildPairs");
        if (pairsEl != null) {
            for (Element a : children(pairsEl, "allow")) {
                pairs.add(new AllowedParentChildPair(attr(a, "parent"), attr(a, "child")));
            }
        }

        List<OrganizationalUnit> units = new ArrayList<>();
        Element unitsEl = child(root, "units");
        if (unitsEl != null) {
            for (Element u : children(unitsEl, "unit")) {
                readUnit(u, null, units);
            }
        }

        return new InstitutionConfig(attr(root, "id"), attr(root, "code"), attr(root, "name"),
                attr(root, "description"), types, pairs, units);
    }

    private UnitTypeDefinition readType(Element t) {
        List<AttributeDefinition> attributes = new ArrayList<>();
        Element attrsEl = child(t, "attributes");
        if (attrsEl != null) {
            for (Element a : children(attrsEl, "attribute")) {
                attributes.add(new AttributeDefinition(
                        attr(a, "key"),
                        attr(a, "label"),
                        attr(a, "description"),
                        AttributeType.valueOf(attr(a, "type")),
                        bool(a, "required", false),
                        attr(a, "default")));
            }
        }
        return new UnitTypeDefinition(attr(t, "code"), attr(t, "label"), attr(t, "description"),
                bool(t, "active", true), attributes);
    }

    /** Lit une unité puis ses enfants imbriqués ; le parent est déduit de l'imbrication XML. */
    private void readUnit(Element el, String parentId, List<OrganizationalUnit> out) {
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        Element valuesEl = child(el, "values");
        if (valuesEl != null) {
            for (Element v : children(valuesEl, "value")) {
                String key = attr(v, "key");
                // les clés dupliquées sont déjà rejetées par la contrainte xs:unique du XSD
                values.put(key, new AttributeValue(key, v.getTextContent()));
            }
        }
        String id = attr(el, "id");
        out.add(new OrganizationalUnit(id, attr(el, "code"), attr(el, "name"), attr(el, "description"),
                attr(el, "type"), parentId, bool(el, "active", true), values));

        Element childrenEl = child(el, "children");
        if (childrenEl != null) {
            for (Element c : children(childrenEl, "unit")) {
                readUnit(c, id, out);
            }
        }
    }

    private static String attr(Element el, String name) {
        return el.hasAttribute(name) ? el.getAttribute(name) : null;
    }

    private static boolean bool(Element el, String name, boolean defaultValue) {
        if (!el.hasAttribute(name)) {
            return defaultValue;
        }
        String v = el.getAttribute(name).trim();
        return v.equals("true") || v.equals("1");
    }

    private static Element child(Element parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getNodeName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getNodeName().equals(name)) {
                result.add(e);
            }
        }
        return result;
    }

    private static Schema loadDefaultSchema() {
        try (InputStream in = XmlConfigLoader.class.getResourceAsStream(XSD_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Ressource " + XSD_RESOURCE + " introuvable dans le classpath");
            }
            return SecureXml.schemaFactory().newSchema(new StreamSource(in));
        } catch (IOException | SAXException e) {
            throw new IllegalStateException("Schéma institution.xsd illisible ou invalide", e);
        }
    }
}
